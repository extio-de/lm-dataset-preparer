package de.extio.lmdatasetprep.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.Execution;
import de.extio.lmdatasetprep.Execution.WorkPacket;
import de.extio.lmdatasetprep.TextUtils;
import de.extio.lmlib.client.ClientService;
import de.extio.lmlib.client.Conversation;
import de.extio.lmlib.client.Conversation.Turn;
import de.extio.lmlib.client.Conversation.TurnType;

@Component
public class Rewrite implements DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Rewrite.class);
	
	private static final byte[] PARAGRAPH = "\n\n".getBytes(StandardCharsets.UTF_8);
	
	private static final String SYSTEM_PROMPT = "You are a helpful assistant with great authoring skills.";
	
	private static final String IMPROVEMENT_PROMPT = "Improve %s without changing the existing sentences, improve the text in a nuanced and understated way. Return the improved text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:";
	
	private static final String ENHANCE_PROMPT = "Improve %s. %s without changing the existing sentences, focusing on enriching the description of the setting in a nuanced and understated way. Return the improved text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:";
	
	private static final String FIRST_PROMPT = "the following text";
	
	private static final String NEXT_PROMPT = "the next paragraph";
	
	private static final List<String> ENHANCEMENTS = List.of(
			"Infuse this paragraph with subtle sensory details, focusing on the texture of objects and the atmosphere of the setting",
			"Add subtle hints of the character's inner thoughts and emotions within the existing dialogue and actions",
			"Enhance the paragraph with figurative language like metaphors or similes to create a richer tapestry of imagery",
			"Develop the subtext of the paragraph by suggesting unspoken tensions or hidden motivations between characters",
			"Introduce a faint scent or sound that adds another layer of sensory detail to the scene without disrupting the flow",
			"Imbue the paragraph with a subtle sense of foreboding or anticipation by subtly hinting at future events without revealing specifics",
			"Elevate the language of the paragraph by replacing basic verbs and adjectives with more evocative synonyms",
			"Add a touch of humor or irony to the paragraph through clever wordplay or unexpected observations",
			"Infuse the paragraph with a hint of melancholy through carefully chosen word choices",
			"Elevate the prose by subtly substituting bland adjectives with more evocative synonyms",
			"Suggest a deeper history for the setting through understated details and allusions",
			"Employ precise verbs to bring a greater sense of movement and life to the scene",
			"Weave in olfactory details – scents, aromas, and odors – to add another layer of sensory experience",
			"Introduce a sense of anticipation or unease through carefully placed descriptive elements",
			"Imbue the setting with a sense of timelessness or age through subtle phrasing and word choice");
	
	@Autowired
	private ClientService clientService;
	
	@Override
	public String getModelCategoryPropertyName() {
		return "rewrite.category";
	}
	
	@Override
	public void accept(final Properties properties) {
		Execution.transform(properties.getProperty("rewrite.source"),
				packet -> {
					final List<Runnable> tasks = new ArrayList<>();
					for (int i = 0; i < Integer.parseInt(properties.getProperty("rewrite.improve")); i++) {
						final int fi = i;
						tasks.add(() -> this.rewriteFile(properties, packet, fi, "impr", first -> String.format(IMPROVEMENT_PROMPT, first ? FIRST_PROMPT : NEXT_PROMPT)));
					}
					for (int i = 0; i < Integer.parseInt(properties.getProperty("rewrite.enhance")); i++) {
						final int fi = i;
						tasks.add(() -> this.rewriteFile(properties, packet, fi, "enh", first -> String.format(ENHANCE_PROMPT, first ? FIRST_PROMPT : NEXT_PROMPT, ENHANCEMENTS.get(ThreadLocalRandom.current().nextInt(ENHANCEMENTS.size())))));
					}
					return tasks;
				});
	}
	
	private void rewriteFile(final Properties properties, final WorkPacket packet, final int variation, final String identifier, final Function<Boolean, String> promptSupplier) {
		final var out = Execution.suffixFilename(packet.file().getFileName(),
				"rewr",
				properties.getProperty("rewrite.model"),
				identifier,
				String.valueOf(variation));
		
		LOGGER.info("Rewriting " + packet.file().getFileName() + " " + identifier + " " + variation);
		
		Execution.streamOut(out, "rewrite.destination", properties, fos -> {
			final var client = this.getClient(properties, this.clientService);
			
			final var text = TextUtils.normalizeText(packet.text());
			final var splits = TextUtils.splitParagraphs(text, 1250, 350, false);
			
			var first = true;
			String turn = null;
			String response = null;
			for (int i = 0; i < splits.size(); i++) {
				LOGGER.info("Split " + (i + 1) + "/" + splits.size());
				
				try {
					Conversation conversation;
					if (first) {
						first = false;
						turn = promptSupplier.apply(Boolean.TRUE) + "\n" + splits.get(i);
						conversation = Conversation.create(SYSTEM_PROMPT, turn);
					}
					else {
						conversation = Conversation.create(SYSTEM_PROMPT, turn);
						conversation.addTurn(new Turn(TurnType.ASSISTANT, response));
						turn = promptSupplier.apply(Boolean.FALSE) + "\n" + splits.get(i);
						conversation.addTurn(new Turn(TurnType.USER, turn));
						
						fos.write(PARAGRAPH);
					}
					LOGGER.debug(conversation.toString());
					
					final var completion = client.conversation(this.getModelCategory(properties), conversation);
					response = completion.response();
					fos.write(response.getBytes(StandardCharsets.UTF_8));
				}
				catch (final IOException e) {
					LOGGER.error("IO exception", e);
				}
			}
		});
	}
}
