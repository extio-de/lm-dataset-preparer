package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MergeJsonl implements Consumer<String[]> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MergeJsonl.class);
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 3) {
			LOGGER.error("Arguments are missing. <Path to jsonl files> <Path to output dir>");
			return;
		}
		
		final Path out = Path.of(args[2], "dataset-merged.jsonl");
		try (var fos = Files.newOutputStream(out)) {
			Utils.transformDirectory(args[1], p -> {
				return List.of(() -> {
					try {
						final byte[] b = Files.readAllBytes(p);
						synchronized (fos) {
							fos.write(b);
						}
					}
					catch (final IOException e) {
						throw new RuntimeException("Cannot read file", e);
					}
					
				});
			});
		}
		catch (final IOException e1) {
			LOGGER.error("IO exception", e1);
		}
	}
	
}
