package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
	
	static String normalizeText(final String text) {
		String result = text;
		
		// Remove excessive newlines, spaces and tabs
		int cnt = 0;
		do {
			cnt = result.length();
			result = result.replace("\r", "\n");
			result = result.replace("\n\n\n", "\n\n");
			result = result.replace("\t", "");
			result = result.replace("  ", " ");
			result = result.replace(" .", ".");
			result = result.replace(".  ", ". ");
			result = result.replace("\n.", ".");
			result = result.replace(" !", "!");
			result = result.replace("!  ", "! ");
			result = result.replace("\n!", "!");
			result = result.replace(" ?", "?");
			result = result.replace("?  ", "? ");
			result = result.replace("\n?", "?");
			result = result.replace(" \n", "\n");
		} while (result.length() != cnt);
		
		// Remove newlines mid of the sentence
		final StringBuilder sb = new StringBuilder(result);
		for (int i = 1; i < sb.length(); i++) {
			final char c = sb.charAt(i);
			if (c == '\n' && sb.charAt(i - 1) != '.' && sb.charAt(i - 1) != '?' && sb.charAt(i - 1) != '!' && sb.charAt(i - 1) != '\n') {
				sb.setCharAt(i, ' ');
			}
		}
		result = sb.toString();
		
		LOGGER.info("Normalizer stats: Before: {} After {}", text.length(), result.length());
		
		return result;
	}
	
	static List<String> splitParagraphs(final String text, final int chunks_norm, final int chunks_var) {
		final int CHUNKS_MIN = chunks_norm - chunks_var;
		final int CHUNKS_MAX = chunks_norm + chunks_var;
		
		if (text.length() <= CHUNKS_MAX) {
			return Collections.singletonList(text);
		}
		
		final List<String> splits = new ArrayList<>();
		
		int pos = 0;
		paragraphs: do {
			if (text.length() - pos > CHUNKS_MAX) {
				delimiters: for (final String delimiter : List.of("\n\n", "\n", ".")) {
					int next = text.indexOf(delimiter, pos);
					while (next > -1) {
						if (next < pos + CHUNKS_MIN) {
							next = text.indexOf(delimiter, next + 1);
						}
						else if (next > pos + CHUNKS_MAX) {
							continue delimiters;
						}
						else {
							String split = text.substring(pos, next).trim();
							if (!split.endsWith(".")) {
								split = split + ".";
							}
							splits.add(split);
							pos = next + delimiter.length();
							continue paragraphs;
						}
					}
				}
			}
			
			final int next = Math.min(pos + CHUNKS_MAX, text.length());
			final String split = text.substring(pos, next).trim();
			splits.add(split);
			pos = next + 1;
		} while (pos < text.length());
		
		LOGGER.info("Created {} splits", splits.size());
		
		return splits;
	}
	
	static void transformDirectory(final String path, final Function<Path, List<Runnable>> createTasks) {
		try (final ExecutorService executor = Executors.newFixedThreadPool(4);
				Stream<Path> stream = Files.list(Paths.get(path))) {
			final List<CompletableFuture<?>> tasks = new ArrayList<>();
			stream
					.filter(p -> Files.isRegularFile(p))
					.forEach(p -> {
						for (final Runnable r : createTasks.apply(p)) {
							tasks.add(CompletableFuture.runAsync(r, executor));
						}
					});
			CompletableFuture.allOf(tasks.toArray(new CompletableFuture[0])).join();
		}
		catch (final IOException e) {
			LOGGER.error("Error", e);
		}
	}
	
	static Path suffixFilename(final Path f, final String... suffixes) {
		final String[] parts = f.getFileName().toString().split("[.]");
		final StringBuilder sb = new StringBuilder();
		for (final String suffix : suffixes) {
			sb.append('_');
			sb.append(suffix);
		}
		sb.append('.');
		return f.getParent().resolve(parts[0] + sb.toString() + parts[1]);
	}
}
