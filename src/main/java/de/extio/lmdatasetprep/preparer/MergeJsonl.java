package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MergeJsonl implements Consumer<String[]>, DatasetTool {
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MergeJsonl.class);
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 3) {
			LOGGER.error("Arguments are missing. <Path to output dir> [ <Path to jsonl files>, ... ]");
			return;
		}
		
		final List<String> lines = Collections.synchronizedList(new ArrayList<>());
		for (int i = 2; i < args.length; i++) {
			Utils.transformDirectory(args[i], p -> {
				return List.of(() -> {
					try {
						lines.addAll(Files.readAllLines(p));
					}
					catch (final IOException e) {
						throw new RuntimeException("Cannot read file", e);
					}
					
				});
			});
		}
		
		Collections.shuffle(lines, ThreadLocalRandom.current());
		
		final Path out = Path.of(args[1], "dataset-merged.jsonl");
		try (var fos = Files.newOutputStream(out)) {
			for (final String line : lines) {
				fos.write(line.getBytes(StandardCharsets.UTF_8));
				fos.write(NEWLINE);
			}
		}
		catch (final IOException e1) {
			LOGGER.error("IO exception", e1);
		}
	}
	
}
