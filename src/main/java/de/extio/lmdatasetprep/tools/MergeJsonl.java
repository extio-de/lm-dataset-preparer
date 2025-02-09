package de.extio.lmdatasetprep.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.Execution;

@Component
public class MergeJsonl implements DatasetTool {
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MergeJsonl.class);
	
	@Override
	public void accept(final Properties properties) {
		final var filters = StringUtils.defaultIfEmpty(properties.getProperty("mergeJsonl.filter", "*"), "*");
		for (final var filter : StringUtils.split(filters, ',')) {
			final List<String> lines = Collections.synchronizedList(new ArrayList<>());
			for (int i = 0; i < 99; i++) {
				final String source = properties.getProperty("mergeJsonl.source." + i, "");
				if (source.isBlank()) {
					break;
				}
				Execution.transform(source, p -> {
					if (!filter.equals("*") && !p.file().getFileName().toString().contains(filter)) {
						return List.of();
					}
					
					return List.of(() -> {
						lines.addAll(Arrays.asList(StringUtils.split(p.text(), '\n')));
					});
				});
			}
			
			Collections.shuffle(lines, ThreadLocalRandom.current());
			
			final Path out = Path.of("dataset-merged" + (filter.equals("*") ? "" : filter) + ".jsonl");
			
			Execution.streamOut(out, "mergeJsonl.destination", properties, fos -> {
				try {
					for (final String line : lines) {
						fos.write(line.getBytes(StandardCharsets.UTF_8));
						fos.write(NEWLINE);
					}
				}
				catch (final IOException e1) {
					LOGGER.error("IO exception", e1);
				}
			});
		}
		
	}
	
}
