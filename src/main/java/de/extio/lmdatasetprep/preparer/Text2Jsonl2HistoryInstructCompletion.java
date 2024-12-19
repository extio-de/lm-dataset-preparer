package de.extio.lmdatasetprep.preparer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class Text2Jsonl2HistoryInstructCompletion extends AbstractContextualPrompts implements DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl2HistoryInstructCompletion.class);
	
	@Override
	protected CreateTasks createTasks(final String[] args, final List<ChunkCfg> chunkConfigurations, final int variations) {
		return p -> {
			final ObjectMapper mapper = new ObjectMapper();
			final List<Runnable> tasks = new ArrayList<>();
			
			for (final ChunkCfg chunkCfg : chunkConfigurations) {
				final List<String> paragraphs = this.fileToParagraphs(p, chunkCfg.chunksNorm(), chunkCfg.chunksVar());
				
				for (int i = 0; i < variations; i++) {
					final Path out = Utils.suffixFilename(Path.of(args[2]).resolve(p.getFileName()),
							"histinstrcompl",
							args[3],
							String.valueOf(chunkCfg.chunksNorm()),
							String.valueOf(i),
							".jsonl");
					if (Files.exists(out)) {
						LOGGER.info("Skipping {}", out);
						continue;
					}
					
					tasks.add(() -> {
						Utils.streamOut(out, fos -> {
							for (int j = 1; j < paragraphs.size(); j++) {
								LOGGER.info("Paragraph " + (j + 1) + "/" + paragraphs.size());
								
								final var inScope = String.join("\n", paragraphs.subList(j - 1, j));
								final var mapping = this.createCharacterNameMapping(inScope);
								final var instruct = this.createPrompt(this.renameCharacters(paragraphs.get(j), mapping), "Rewrite the following story as a concise instruction in a single paragraph. Return the instruction, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
								final var qaLine = new QaLine(this.renameCharacters(paragraphs.get(j - 1), mapping), instruct, this.renameCharacters(paragraphs.get(j), mapping));
								
								this.writeJsonLine(mapper, fos, qaLine);
							}
						});
					});
				}
			}
			
			return tasks;
		};
	}
	
	protected record QaLine(String history, String instruct, String completion) {
	}
}
