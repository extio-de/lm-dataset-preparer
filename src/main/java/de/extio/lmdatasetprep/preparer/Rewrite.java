package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.XorShift128Random;
import de.extio.lmdatasetprep.client.Client;
import de.extio.lmdatasetprep.client.profile.ModelCategory;

@Component
public class Rewrite implements Consumer<String[]> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Rewrite.class);
	
	private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(XorShift128Random::new);
	
	private static String IMPROVEMENT_PROMPT = "Improve the following text without changing the existing sentences, improve the text in a nuanced and understated way. Return the improved text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:";
	
	private static String ENHANCE_PROMPT = "Improve the following text. %s without changing the existing sentences, focusing on enriching the description of the setting in a nuanced and understated way. Return the improved text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:";
	
	private static List<String> ENHANCEMENTS = List.of(
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
	private Client client;
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 5) {
			LOGGER.error("Arguments are missing. <Path to text files> <Path to output dir> <Texts per input> <Model suffix>");
			return;
		}
		
		Utils.transformDirectory(args[1],
				f -> {
					final List<Runnable> tasks = new ArrayList<>();
					tasks.add(() -> this.rewriteFile(args, f, 0, "impr", ModelCategory.COLD, IMPROVEMENT_PROMPT));
					for (int i = 0; i < Integer.parseInt(args[3]); i++) {
						final int fi = i;
						tasks.add(() -> this.rewriteFile(args, f, fi, "enh", ModelCategory.COLD, String.format(ENHANCE_PROMPT, ENHANCEMENTS.get(RANDOM.get().nextInt(ENHANCEMENTS.size())))));
					}
					return tasks;
				});
	}
	
	private void rewriteFile(final String[] args, final Path f, final int i, final String identifier, final ModelCategory modelCategory, final String prompt) {
		final Path out = Utils.suffixFilename(Path.of(args[2]).resolve(f.getFileName()),
				"rewr",
				args[4],
				identifier,
				modelCategory.toString().toLowerCase(),
				String.valueOf(i));
		if (Files.exists(out)) {
			LOGGER.info("Skipping " + out);
			return;
		}
		
		LOGGER.info("Rewriting " + f.getFileName() + " " + identifier + " " + modelCategory.name() + " " + i);
		
		final List<String> paragraphs = this.rewrite(f, prompt, modelCategory);
		try {
			Files.writeString(out, String.join("\n\n", paragraphs));
		}
		catch (final IOException e) {
			LOGGER.error("Cannot store file", e);
		}
	}
	
	List<String> rewrite(final Path file, final String prompt, final ModelCategory modelCategory) {
		String text;
		try {
			text = Utils.normalizeText(Files.readString(file));
		}
		catch (final IOException e) {
			throw new RuntimeException("Cannot read file", e);
		}
		
		final List<String> splits = Utils.splitParagraphs(text, 1250, 350);
		
		final List<String> paragraphs = new ArrayList<>(splits.size());
		for (final String split : splits) {
			final var completion = this.client.completion("You are a helpful assistant with great authoring skills.",
					prompt,
					split,
					modelCategory);
			paragraphs.add(completion.response());
		}
		
		return paragraphs;
	}
	
}
