package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
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
	
	private static final byte[] PARAGRAPH = "\n\n".getBytes(StandardCharsets.UTF_8);
	
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
				final Path out = Utils.suffixFilename(Path.of(args[2]).resolve(p.getFileName()), "_en");
				if (Files.exists(out)) {
					LOGGER.info("Skipping " + out);
				}
				else {
					try (var fos = Files.newOutputStream(out)) {
						final AtomicBoolean first = new AtomicBoolean(true);
						this.translate(p, chunk -> {
							try {
								if (!first.getAndSet(false)) {
									fos.write(PARAGRAPH);
								}
								fos.write(chunk.getBytes(StandardCharsets.UTF_8));
							}
							catch (final IOException e) {
								LOGGER.error("IO exception", e);
							}
						});
					}
					catch (final IOException e1) {
						LOGGER.error("IO exception", e1);
					}
				}
			});
		});
	}
	
	void translate(final Path file, final Consumer<String> consumer) {
		LOGGER.info("Translating " + file);
		
		String text;
		try {
			text = Utils.normalizeText(Files.readString(file));
		}
		catch (final IOException e) {
			throw new RuntimeException("Cannot read file", e);
		}
		
		final List<String> splits = Utils.splitParagraphs(text, 1250, 350, false);
		
		for (final String split : splits) {
			LOGGER.info("Split " + splits.indexOf(split) + "/" + splits.size());
			final var completion = this.client.completion("You are an assistant with great language translation and authoring skills.",
					"Translate the following text to English. Return the English translation, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:",
					split,
					ModelCategory.COLD);
			consumer.accept(completion.response());
		}
	}
	
}
