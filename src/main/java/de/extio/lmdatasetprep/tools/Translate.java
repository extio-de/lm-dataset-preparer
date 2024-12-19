package de.extio.lmdatasetprep.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import de.extio.lmdatasetprep.Execution;
import de.extio.lmdatasetprep.Execution.WorkPacket;
import de.extio.lmdatasetprep.TextUtils;
import de.extio.lmlib.client.ClientService;

@Component
public class Translate implements DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Translate.class);
	
	private static final byte[] PARAGRAPH = "\n\n".getBytes(StandardCharsets.UTF_8);
	
	@Autowired
	private ClientService clientService;
	
	@Override
	public String getModelCategoryPropertyName() {
		return "translate.category";
	}
	
	@Override
	public void accept(final Properties properties) {
		Execution.transform(properties.getProperty("translate.source"), p -> {
			return List.of(() -> {
				final Path out = Execution.suffixFilename(p.file().getFileName(), "en");
				Execution.streamOut(out, "translate.destination", properties, fos -> {
					final AtomicBoolean first = new AtomicBoolean(true);
					this.translate(p, properties, chunk -> {
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
				});
			});
		});
	}
	
	void translate(final WorkPacket packet, final Properties properties, final Consumer<String> consumer) {
		LOGGER.info("Translating " + packet.file());
		
		final String text = TextUtils.normalizeText(packet.text());
		
		final List<String> splits = TextUtils.splitParagraphs(text, 1250, 350, false);
		
		final var client = this.getClient(properties, this.clientService);
		
		for (final String split : splits) {
			LOGGER.info("Split " + (splits.indexOf(split) + 1) + "/" + splits.size());
			final var completion = client.completion(this.getModelCategory(properties),
					"You are an assistant with great language translation and authoring skills.",
					"Translate the following text to English. Return the English translation, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:",
					split);
			consumer.accept(completion.response());
		}
	}
	
}
