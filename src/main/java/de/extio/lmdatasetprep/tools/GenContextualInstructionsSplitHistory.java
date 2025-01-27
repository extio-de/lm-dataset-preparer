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
public class GenContextualInstructionsSplitHistory extends AbstractContextualPrompts {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenContextualInstructionsSplitHistory.class);
	
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
								"contextualinstrhist",
								properties.getProperty("contextualPrompts.model"),
								String.valueOf(chunkCfg.chunksNorm()),
								String.valueOf(fi),
								".jsonl");
						
						Execution.streamOut(out, "contextualPrompts.destination", properties, fos -> {
							for (int j = 1; j < paragraphs.size(); j++) {
								LOGGER.info("Paragraph " + (j + 1) + "/" + paragraphs.size());
								
								final var inScope = String.join("\n", paragraphs.subList(j - 1, j));
								final var mapping = this.createCharacterNameMapping(inScope, properties);
								final var summary = this.requestCompletion(properties, this.renameCharacters(paragraphs.get(j - 1), mapping), "Rewrite the following story as a summary of the previous paragraph and write that it is the summary. Return the summary, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
								final var currentParagraph = this.renameCharacters(paragraphs.get(j), mapping);
								final var instruction = this.requestCompletion(properties, currentParagraph, "Summarize the following text as a single concise statement. Return the instruction, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
								final var qaLine = new QaLine("History: " + summary + "\nInstruction: " + instruction, currentParagraph);
								
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
