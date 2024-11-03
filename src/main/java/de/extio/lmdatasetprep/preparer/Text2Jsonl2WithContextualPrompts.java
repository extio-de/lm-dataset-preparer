package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmdatasetprep.client.Client;
import de.extio.lmdatasetprep.client.profile.ModelCategory;

@Component
public class Text2Jsonl2WithContextualPrompts implements Consumer<String[]> {
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl2WithContextualPrompts.class);
	
	@Autowired
	private Client client;
	
	@Override
	public void accept(final String[] args) {
		if (args.length < 6) {
			LOGGER.error("Arguments are missing. <Path to text files> <Path to output dir> <variations> [<chunk norm> <chunk var>, ...]");
			return;
		}
		final int variations = Integer.parseInt(args[3]);
		final List<ChunkCfg> chunkConfigurations = new ArrayList<>();
		for (int i = 4; i < args.length; i += 2) {
			final var cfg = new ChunkCfg(Integer.parseInt(args[i]), Integer.parseInt(args[i + 1]));
			chunkConfigurations.add(cfg);
		}
		
		final ObjectMapper mapper = new ObjectMapper();
		
		Utils.transformDirectory(args[1], p -> {
			final List<Runnable> tasks = new ArrayList<>();
			
			for (final ChunkCfg chunkCfg : chunkConfigurations) {
				final List<String> paragraphs = this.fileToParagraphs(p, chunkCfg.chunksNorm(), chunkCfg.chunksVar());
				
				for (int i = 0; i < variations; i++) {
					final Path out = Utils.suffixFilename(Path.of(args[2]).resolve(p.getFileName()),
							"contextualds",
							String.valueOf(chunkCfg.chunksNorm()),
							String.valueOf(i),
							".jsonl");
					if (Files.exists(out)) {
						LOGGER.info("Skipping {}", out);
						continue;
					}
					
					tasks.add(() -> {
						try (var fos = Files.newOutputStream(out)) {
							for (final String paragraph : paragraphs) {
								LOGGER.info("Paragraph " + paragraphs.indexOf(paragraph) + "/" + paragraphs.size());
								final var qaLine = this.paragraphToContextualPrompt(paragraph);
								this.writeJsonLine(mapper, fos, qaLine);
							}
						}
						catch (final IOException e1) {
							LOGGER.error("IO exception", e1);
						}
					});
				}
			}
			
			return tasks;
		});
	}
	
	List<String> fileToParagraphs(final Path file, final int chunkNorm, final int chunkVar) {
		LOGGER.info("Splitting to jsonl " + file + " -> " + chunkNorm + "±" + chunkVar);
		
		String text;
		try {
			text = Utils.normalizeText(Files.readString(file));
		}
		catch (final IOException e) {
			throw new RuntimeException("Cannot read file", e);
		}
		
		return Utils.splitParagraphs(text, chunkNorm, chunkVar, true);
	}
	
	private QaLine paragraphToContextualPrompt(final String paragraph) {
		final var completion = this.client.completion("You are an assistant with great text rewriting skills.",
				"Rewrite the following text as a user prompt. Include all essential details, enabling the user to better understand the subject matter. Return the prompt, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation:",
				paragraph,
				ModelCategory.COLD);
		
		String prompt = completion.response();
		final int colon = prompt.indexOf(':');
		if (colon > -1 && colon > prompt.length() + 1) {
			prompt = prompt.substring(colon + 1);
		}
		prompt = prompt.trim();
		
		final var qaLine = new QaLine(prompt, paragraph);
		return qaLine;
	}
	
	private void writeJsonLine(final ObjectMapper mapper, final OutputStream fos, final QaLine qaLine) {
		try {
			final var jsonb = mapper
					.writeValueAsString(qaLine)
					.getBytes(StandardCharsets.UTF_8);
			fos.write(jsonb);
			fos.write(NEWLINE);
		}
		catch (final IOException e) {
			LOGGER.error("Cannot convert split to json", e);
		}
	}
	
	record QaLine(String question, String answer) {
	}
	
	record ChunkCfg(int chunksNorm, int chunksVar) {
	}
	
}
