package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Text2Jsonl implements Consumer<String[]>, DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl.class);
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 5) {
			LOGGER.error("Arguments are missing. <Path to text files> <Path to output dir> <chunk norm> <chunk var>");
			return;
		}
		
		final Path out = Path.of(args[2], "dataset.jsonl");
		try (var fos = Files.newOutputStream(out)) {
			Utils.transformDirectory(args[1], p -> {
				return List.of(() -> {
					final List<String> splits = this.splitToJsonl(p, Integer.parseInt(args[3]), Integer.parseInt(args[4]));
					synchronized (this) {
						try {
							for (final String split : splits) {
								fos.write(split.getBytes(StandardCharsets.UTF_8));
								fos.write(NEWLINE);
							}
						}
						catch (final IOException e) {
							LOGGER.error("IO exception", e);
						}
					}
				});
			});
		}
		catch (final IOException e1) {
			LOGGER.error("IO exception", e1);
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
		
		final List<String> jsonl = new ArrayList<>();
		final ObjectMapper mapper = new ObjectMapper();
		
		final List<String> paragraphs = Utils.splitParagraphs(text, chunkNorm, chunkVar, true);
		for (int i = 0; i < paragraphs.size(); i++) {
			final String split = paragraphs.get(i);
			try {
				jsonl.add(mapper.writeValueAsString(new TextLine(split)));
			}
			catch (final JsonProcessingException e) {
				LOGGER.error("Cannot convert split to json", e);
			}
		}
		
		return jsonl;
	}
	
	record TextLine(String text) {
	}
	
}
