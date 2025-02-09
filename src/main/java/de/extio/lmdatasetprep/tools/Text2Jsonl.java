package de.extio.lmdatasetprep.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmdatasetprep.Execution;
import de.extio.lmdatasetprep.Execution.WorkPacket;
import de.extio.lmdatasetprep.TextUtils;

@Component
public class Text2Jsonl implements DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl.class);
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	@Override
	public void accept(final Properties properties) {
		final Path out = Path.of("dataset-text.jsonl");
		Execution.streamOut(out, "text2Jsonl.destination", properties, fos -> {
			Execution.transform(properties.getProperty("text2Jsonl.source"), p -> {
				return List.of(() -> {
					final List<String> splits = this.splitToJsonl(p, Integer.parseInt(properties.getProperty("text2Jsonl.chunkNorm")), Integer.parseInt(properties.getProperty("text2Jsonl.chunkVar")));
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
		});
	}
	
	List<String> splitToJsonl(final WorkPacket packet, final int chunkNorm, final int chunkVar) {
		LOGGER.info("Splitting to jsonl " + packet.file());
		
		final String text = TextUtils.normalizeText(packet.text());
		
		final List<String> jsonl = new ArrayList<>();
		final ObjectMapper mapper = new ObjectMapper();
		
		final List<String> paragraphs = TextUtils.splitParagraphs(text, chunkNorm, chunkVar, true);
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
