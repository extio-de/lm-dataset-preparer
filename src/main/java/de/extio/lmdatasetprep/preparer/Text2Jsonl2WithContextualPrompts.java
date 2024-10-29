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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmdatasetprep.client.Client;
import de.extio.lmdatasetprep.client.profile.ModelCategory;

@Component
public class Text2Jsonl2WithContextualPrompts implements Consumer<String[]> {
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl2WithContextualPrompts.class);
	
	@Autowired
	private Client client;
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 6) {
			LOGGER.error("Arguments are missing. <Path to text files> <Path to output dir> <chunk norm> <chunk var> <variations>");
			return;
		}
		final int chunksNorm = Integer.parseInt(args[3]);
		final int chunksVar = Integer.parseInt(args[4]);
		final int variations = Integer.parseInt(args[5]);
		
		final ObjectMapper mapper = new ObjectMapper();
		
		final Path out = Path.of(args[2], "dataset_with_context_" + chunksNorm + ".jsonl");
		try (var fos = Files.newOutputStream(out)) {
			Utils.transformDirectory(args[1], p -> {
				final List<Runnable> tasks = new ArrayList<>();
				final List<String> paragraphs = this.fileToParagraphs(p, chunksNorm, chunksVar);
				for (final String paragraph : paragraphs) {
					for (int i = 0; i < variations; i++) {
						final int fI = i;
						tasks.add(() -> {
							
							LOGGER.info("### TASK ### " + paragraphs.indexOf(paragraph) + " " + fI + " " + paragraphs.size());
							
							final var completion = this.client.completion("You are an assistant with great authoring skills.",
									"Enhance the given text by crafting a prompt that effectively provides additional context, enabling the language model to better understand the subject matter and generate more informative and relevant responses. This prompt should be clear, concise, and tailored to facilitate contextual learning, allowing the model to grasp the essential details and nuances of the topic. Return the prompt, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:",
									paragraph,
									ModelCategory.COLD);
							final var qaLine = new QaLine(completion.response().replace("Prompt:", ""), paragraph);
							try {
								final var jsonb = mapper
										.writeValueAsString(qaLine)
										.getBytes(StandardCharsets.UTF_8);
								synchronized (this) {
									fos.write(jsonb);
									fos.write(NEWLINE);
								}
							}
							catch (final IOException e) {
								LOGGER.error("Cannot convert split to json", e);
							}
						});
					}
				}
				return tasks;
			});
		}
		catch (final IOException e1) {
			LOGGER.error("IO exception", e1);
		}
	}
	
	List<String> fileToParagraphs(final Path file, final int chunkNorm, final int chunkVar) {
		LOGGER.info("Splitting to jsonl " + file);
		
		String text;
		try {
			text = Utils.normalizeText(Files.readString(file));
		}
		catch (final IOException e) {
			throw new RuntimeException("Cannot read file", e);
		}
		
		return Utils.splitParagraphs(text, chunkNorm, chunkVar);
	}
	
	record QaLine(String question, String answer) {
	}
	
}
