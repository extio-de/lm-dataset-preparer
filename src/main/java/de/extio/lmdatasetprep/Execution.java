package de.extio.lmdatasetprep;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Execution {
	
	public static final AtomicInteger startedFileConsumers = new AtomicInteger();
	
	public static final AtomicInteger finishedFileConsumers = new AtomicInteger();
	
	public static final AtomicBoolean shutdown = new AtomicBoolean();
	
	public static final ConcurrentMap<String, ConcurrentMap<String, BlockingQueue<WorkPacket>>> work = new ConcurrentHashMap<>();
	
	public static final BlockingQueue<CompletableFuture<Void>> tasks;
	
	public static final ScheduledExecutorService scheduler;
	
	public static final ExecutorService executorService;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Execution.class);
	
	static {
		final int EXECUTORS = Integer.parseInt(LmDatasetPreparerApplication.applicationContext.getEnvironment().getProperty("agent.threads"));
		
		tasks = new LinkedBlockingQueue<>(EXECUTORS * 2);
		
		executorService = Executors.newFixedThreadPool(EXECUTORS);
		
		scheduler = Executors.newScheduledThreadPool(1);
		scheduler.scheduleAtFixedRate(() -> {
			tasks.removeIf(future -> future.isDone());
		}, 0, 1, TimeUnit.SECONDS);
	}
	
	public static void transform(final String source, final Function<WorkPacket, List<Runnable>> createTasks) {
		if (source.startsWith("queue")) {
			transformQueue(source, createTasks);
		}
		else {
			transformDirectory(source, createTasks);
		}
	}
	
	private static void transformDirectory(final String source, final Function<WorkPacket, List<Runnable>> createTasks) {
		startedFileConsumers.incrementAndGet();
		try (Stream<Path> stream = Files.list(Paths.get(source))) {
			stream.filter(p -> Files.isRegularFile(p))
					.map(p -> {
						try {
							return new WorkPacket(p, Files.readString(p));
						}
						catch (final IOException e) {
							LOGGER.error("IO exception", e);
							return null;
						}
					})
					.forEach(p -> {
						for (final Runnable r : createTasks.apply(p)) {
							try {
								tasks.put(CompletableFuture.runAsync(r, executorService));
							}
							catch (final InterruptedException e) {
								throw new RuntimeException("Interrupted", e);
							}
						}
					});
		}
		catch (final IOException e) {
			LOGGER.error("IO exception", e);
		}
		finally {
			finishedFileConsumers.incrementAndGet();
		}
	}
	
	private static void transformQueue(final String source, final Function<WorkPacket, List<Runnable>> createTasks) {
		final var queue = work.computeIfAbsent(source, k -> new ConcurrentHashMap<>()).computeIfAbsent(Thread.currentThread().getName(), p -> new LinkedBlockingQueue<>());
		final Supplier<WorkPacket> getNext = () -> {
			try {
				WorkPacket next;
				while ((next = queue.poll(1, TimeUnit.SECONDS)) == null) {
					if (shutdown.get()) {
						return null;
					}
				}
				return next;
			}
			catch (final InterruptedException e) {
				return null;
			}
		};
		
		WorkPacket next;
		while ((next = getNext.get()) != null) {
			for (final Runnable r : createTasks.apply(next)) {
				try {
					tasks.put(CompletableFuture.runAsync(r, executorService));
				}
				catch (final InterruptedException e) {
					throw new RuntimeException("Interrupted", e);
				}
			}
		}
	}
	
	public static Path suffixFilename(final Path f, final String... suffixes) {
		final StringBuilder sb = new StringBuilder();
		for (final String suffix : suffixes) {
			if (suffix.charAt(0) != '.') {
				sb.append('_');
				sb.append(suffix);
			}
		}
		
		final String[] parts = f.getFileName().toString().split("[.]");
		String last;
		if (suffixes[suffixes.length - 1].charAt(0) == '.') {
			last = suffixes[suffixes.length - 1];
		}
		else {
			last = "." + parts[1];
		}
		
		final var parent = f.getParent();
		if (parent != null) {
			return f.getParent().resolve(parts[0] + sb.toString() + last);
		}
		return Path.of(parts[0] + sb.toString() + last);
	}
	
	public static void streamOut(final Path f, final String propertyPrefix, final Properties properties, final Consumer<OutputStream> consumer) {
		final var destination = properties.getProperty(propertyPrefix);
		if (destination.startsWith("queue")) {
			streamOutToQueue(f, destination, consumer);
		}
		else {
			streamOutToFile(f, destination, consumer);
		}
		
	}
	
	private static void streamOutToFile(final Path f, final String destination, final Consumer<OutputStream> consumer) {
		final var out = Path.of(destination).resolve(f);
		if (Files.exists(out)) {
			LOGGER.info("File already exists: {} ", out);
			return;
		}
		
		final var tmp = out.resolveSibling(out.getFileName() + ".tmp");
		try (var fos = Files.newOutputStream(tmp)) {
			consumer.accept(fos);
		}
		catch (final IOException e1) {
			LOGGER.error("IO exception", e1);
			return;
		}
		try {
			Files.move(tmp, out);
		}
		catch (final IOException e) {
			LOGGER.error("IO exception", e);
		}
	}
	
	private static void streamOutToQueue(final Path f, final String destination, final Consumer<OutputStream> consumer) {
		try (var bos = new ByteArrayOutputStream()) {
			consumer.accept(bos);
			final var text = bos.toString(StandardCharsets.UTF_8);
			work.computeIfAbsent(destination, k -> new ConcurrentHashMap<>()).values().forEach(queue -> queue.add(new WorkPacket(f, text)));
		}
		catch (final IOException e) {
			LOGGER.error("IO exception", e);
		}
	}
	
	public record WorkPacket(Path file, String text) {
	}
}
