package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.client.Client;
import de.extio.lmdatasetprep.client.profile.ModelCategory;

@Component
public class Translate implements Consumer<String[]> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Translate.class);
	
	@Autowired
	private Client client;
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 3) {
			LOGGER.error("Arguments are missing. <Path to text files> <Path to output dir>");
			return;
		}
		
		Utils.transformDirectory(args[1], p -> {
			return List.of(() -> {
				try {
					Path out = Utils.suffixFilename(Path.of(args[2]).resolve(p.getFileName()), "_en");
					if (Files.exists(out)) {
						LOGGER.info("Skipping " + out);
					}
					else {
						final var translations = this.translate(p);
						Files.writeString(out, String.join("\n\n", translations));
					}
				}
				catch (final IOException e) {
					LOGGER.error("Cannot store file", e);
				}
			});
		});
	}
	
	List<String> translate(final Path file) {
		LOGGER.info("Translating " + file);
		
		String text;
		try {
			text = Utils.normalizeText(Files.readString(file));
		}
		catch (final IOException e) {
			throw new RuntimeException("Cannot read file", e);
		}
		
		final List<String> splits = Utils.splitParagraphs(text, 1250, 350);
		
		final List<String> translations = new ArrayList<>(splits.size());
		for (final String split : splits) {
			final var completion = this.client.completion("You are an assistant with great language translation and authoring skills.",
					"Translate the following text to English. Return the English translation, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:",
					split,
					ModelCategory.COLD);
			translations.add(completion.response());
		}
		
		return translations;
	}
	
}
