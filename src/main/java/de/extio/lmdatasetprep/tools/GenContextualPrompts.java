package de.extio.lmdatasetprep.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.Execution;

@Component
public class GenContextualPrompts extends AbstractContextualPrompts {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenContextualPrompts.class);
	
	@Override
	protected CreateTasks createTasks(final Properties properties, final List<ChunkCfg> chunkConfigurations, final int variations) {
		return p -> {
			final List<Runnable> tasks = new ArrayList<>();
			
			for (final ChunkCfg chunkCfg : chunkConfigurations) {
				for (int i = 0; i < variations; i++) {
					final int fi = i;
					
					tasks.add(() -> {
						final List<String> paragraphs = this.fileToParagraphs(p, chunkCfg.chunksNorm(), chunkCfg.chunksVar());
						
						final Path out = Execution.suffixFilename(p.file().getFileName(),
								"contextualds",
								properties.getProperty("contextualPrompts.model"),
								String.valueOf(chunkCfg.chunksNorm()),
								String.valueOf(fi),
								".jsonl");
						
						Execution.streamOut(out, "contextualPrompts.destination", properties, fos -> {
							for (int j = 0; j < paragraphs.size(); j++) {
								LOGGER.info("Paragraph " + (j + 1) + "/" + paragraphs.size());
								
								QaLine qaLine;
								if (j > 2) {
									final var history = String.join("\n", paragraphs.subList(j - 3, j));
									final var mapping = this.createCharacterNameMapping(history, properties);
									final var previous = this.createPrompt(properties, this.renameCharacters(history, mapping), "Write a summary of the following story as a user prompt that asks to write the story. Mention the place where they are and ask the question what could happen next. Return the prompt, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
									qaLine = new QaLine(previous, this.renameCharacters(paragraphs.get(j), mapping));
								}
								else {
									final var mapping = this.createCharacterNameMapping(paragraphs.get(j), properties);
									final var paragraph = this.renameCharacters(paragraphs.get(j), mapping);
									final var userPrompt = this.createPrompt(properties, paragraph, "Write a summary of the following story as a user prompt that asks to write the story. Include all essential details, enabling the user to better understand the subject matter. Return the prompt, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
									qaLine = new QaLine(userPrompt, paragraph);
								}
								
								this.writeJsonLine(fos, qaLine);
							}
						});
					});
				}
			}
			
			return tasks;
		};
	}
	
}
