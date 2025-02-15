package de.extio.lmdatasetprep.tools;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.Execution;
import de.extio.lmlib.client.Conversation;
import de.extio.lmlib.client.Conversation.Turn;
import de.extio.lmlib.client.Conversation.TurnType;

@Component
public class GenInstructionalConversation extends AbstractContextualPrompts {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(GenInstructionalConversation.class);
	
	@Override
	protected CreateTasks createTasks(final Properties properties, final List<ChunkCfg> chunkConfigurations, final int variations) {
		return p -> {
			final List<Runnable> tasks = new ArrayList<>();
			
			for (final ChunkCfg chunkCfg : chunkConfigurations) {
				for (int i = 0; i < variations; i++) {
					final int fi = i;
					
					tasks.add(() -> {
						final Path out = Execution.suffixFilename(p.file().getFileName(),
								"instrconversation",
								properties.getProperty("contextualPrompts.model"),
								String.valueOf(chunkCfg.chunksNorm()),
								String.valueOf(fi),
								".jsonl");
						
						Execution.streamOut(out, "contextualPrompts.destination", properties, fos -> {
							final var conversation = new ArrayList<ConversationLine>();
							{
								final List<String> paragraphs = this.fileToParagraphs(p, chunkCfg.chunksNorm(), chunkCfg.chunksVar());
								
								final var mapping = new HashMap<String, String>();
								for (int j = 0; j < paragraphs.size(); j++) {
									LOGGER.info("Name Mapping " + (j + 1) + "/" + paragraphs.size());
									mapping.putAll(this.createCharacterNameMapping(paragraphs.get(j), properties));
								}
								
								String previousParagraph = null;
								String currentParagraph = null;
								for (int j = 0; j < paragraphs.size(); j++) {
									LOGGER.info("Paragraph " + (j + 1) + "/" + paragraphs.size());
									
									previousParagraph = currentParagraph;
									currentParagraph = this.renameCharacters(paragraphs.get(j), mapping);
									
									final var c = Conversation.create("Summarize the following text as a single concise statement. Return the instruction, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation.", previousParagraph != null ? previousParagraph : currentParagraph);
									if (previousParagraph != null) {
										c.addTurn(new Turn(TurnType.ASSISTANT, conversation.getLast().user()));
										c.addTurn(new Turn(TurnType.USER, currentParagraph));
									}
									
									final var instruction = this.requestCompletion(properties, c);
									conversation.add(new ConversationLine(instruction, currentParagraph));
								}
							}
							
							this.writeJsonLine(fos, new ConversationsLine(conversation));
						});
					});
				}
			}
			
			return tasks;
		};
	}
}
