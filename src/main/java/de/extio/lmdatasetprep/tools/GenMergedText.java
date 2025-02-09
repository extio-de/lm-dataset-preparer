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
public class GenMergedText extends AbstractContextualPrompts {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenMergedText.class);
	
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
								"text",
								properties.getProperty("contextualPrompts.model"),
								String.valueOf(chunkCfg.chunksNorm()),
								String.valueOf(fi),
								".jsonl");
						
						Execution.streamOut(out, "contextualPrompts.destination", properties, fos -> {
							for (int j = 0; j < paragraphs.size(); j++) {
								LOGGER.info("Paragraph " + (j + 1) + "/" + paragraphs.size());
								
								final var mapping = this.createCharacterNameMapping(paragraphs.get(j), properties);
								final var paragraph = this.renameCharacters(paragraphs.get(j), mapping);
								final var textLine = new TextLine(paragraph);
								
								this.writeJsonLine(fos, textLine);
							}
						});
					});
				}
			}
			
			return tasks;
		};
	}
	
}
