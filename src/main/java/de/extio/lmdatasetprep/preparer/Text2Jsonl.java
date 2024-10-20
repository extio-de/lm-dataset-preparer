package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Text2Jsonl implements Consumer<String[]> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl.class);
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 5) {
			LOGGER.error("Arguments are missing. <Path to text files> <Path to output dir> <chunk norm> <chunk var>");
			return;
		}
		
		final List<String> lines = Collections.synchronizedList(new ArrayList<>());
		
		Utils.transformDirectory(args[1], p -> {
			return List.of(() -> {
				lines.addAll(this.splitToJsonl(p, Integer.parseInt(args[3]), Integer.parseInt(args[4])));
			});
		});
		
		final Path out = Path.of(args[2], "dataset.jsonl");
		try {
			Files.writeString(out, String.join("\n", lines));
		}
		catch (final IOException e) {
			LOGGER.error("Cannot store dataset", e);
		}
	}
	
	List<String> splitToJsonl(final Path file, final int chunkNorm, final int chunkVar) {
		LOGGER.info("Splitting to jsonl " + file);
		
		String text;
		try {
			text = Utils.normalizeText(Files.readString(file));
		}
		catch (final IOException e) {
			throw new RuntimeException("Cannot read file", e);
		}
		
		final ObjectMapper mapper = new ObjectMapper();
		
		return Utils
				.splitParagraphs(text, chunkNorm, chunkVar)
				.stream()
				.map(split -> {
					try {
						return mapper.writeValueAsString(new TextLine(split));
					}
					catch (final JsonProcessingException e) {
						LOGGER.error("Cannot convert split to json", e);
						return "";
					}
				})
				.toList();
	}
	
	record TextLine(String text) {
	}
	
}
