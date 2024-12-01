package de.extio.lmdatasetprep.preparer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmdatasetprep.XorShift128Random;
import de.extio.lmdatasetprep.client.Client;
import de.extio.lmdatasetprep.client.profile.ModelCategory;

@Component
public class Text2Jsonl2WithContextualPrompts implements Consumer<String[]>, InitializingBean {
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Text2Jsonl2WithContextualPrompts.class);
	
	private List<String> maleNames = new ArrayList<>();
	
	private List<String> femaleNames = new ArrayList<>();
	
	@Autowired
	private Client client;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.maleNames = Files.readAllLines(ResourceUtils.getFile("classpath:male.txt").toPath());
		this.femaleNames = Files.readAllLines(ResourceUtils.getFile("classpath:female.txt").toPath());
	}
	
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
							for (int j = 0; j < paragraphs.size(); j++) {
								LOGGER.info("Paragraph " + (j + 1) + "/" + paragraphs.size());
								
								QaLine qaLine;
								if (j > 2) {
									final var history = String.join("\n", paragraphs.subList(j - 3, j));
									final var mapping = this.createCharacterNameMapping(history);
									final var previous = this.paragraphToContextualPrompt(this.renameCharacters(history, mapping), "Rewrite the following text as a user prompt that asks to continue a story. Include all essential details, enabling the user to better understand the subject matter. Return the prompt, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
									qaLine = new QaLine(previous, this.renameCharacters(paragraphs.get(j), mapping));
								}
								else {
									final var mapping = this.createCharacterNameMapping(paragraphs.get(j));
									final var paragraph = this.renameCharacters(paragraphs.get(j), mapping);
									final var userPrompt = this.paragraphToContextualPrompt(paragraph, "Write a summary of the following story as a user prompt that asks to write the story. Include all essential details, enabling the user to better understand the subject matter. Return the prompt, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't return an explanation:");
									qaLine = new QaLine(userPrompt, paragraph);
								}
								
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
		
		return Utils.splitParagraphs(text, chunkNorm, chunkVar, false);
	}
	
	private Map<String, String> createCharacterNameMapping(final String paragraph) {
		final var completion = this.client.completion("You are a helpful assistant.",
				"Extract all names and genders from the following text. Return the result in json format with the following fields: {\"males\" : [ \"Male name 1\", \"Male name 2\", … ], \"females\" : [ \"Female name 1\", \"Female name 2\", … ]}. Don't return a preamble and no explanation:",
				paragraph,
				ModelCategory.COLD);
		
		final var response = completion.response();
		final var start = response.indexOf('{');
		final var end = response.lastIndexOf('}');
		if (start == -1 || end == -1) {
			return Map.of();
		}
		final var json = response.substring(start, end + 1);
		
		Names names;
		final ObjectMapper mapper = new ObjectMapper();
		try {
			names = mapper.readValue(json, Names.class);
		}
		catch (final JsonProcessingException e) {
			return Map.of();
		}
		
		final Random random = new XorShift128Random();
		final Map<String, String> mapping = new HashMap<>();
		if (names.males != null) {
			names.males.forEach(name -> mapping.put(name, this.maleNames.get(random.nextInt(this.maleNames.size()))));
		}
		if (names.females != null) {
			names.females.forEach(name -> mapping.put(name, this.femaleNames.get(random.nextInt(this.femaleNames.size()))));
		}
		if (mapping.isEmpty()) {
			return Map.of();
		}
		return mapping;
	}
	
	private String renameCharacters(final String paragraph, final Map<String, String> mapping) {
		var result = paragraph;
		for (final var entry : mapping.entrySet()) {
			result = result.replaceAll("(?i)" + entry.getKey(), entry.getValue());
		}
		
		return result;
	}
	
	private String paragraphToContextualPrompt(final String paragraph, final String instruction) {
		final var completion = this.client.completion("You are an assistant with great text writing skills.",
				instruction,
				paragraph,
				ModelCategory.COLD);
		
		String prompt = completion.response();
		final int colon = prompt.indexOf(':');
		if (colon > -1 && colon < prompt.length() + 1) {
			prompt = prompt.substring(colon + 1);
		}
		final int preamble = prompt.indexOf("\n\n");
		if (preamble > -1 && preamble < prompt.length() + 2) {
			prompt = prompt.substring(preamble + 2);
		}
		prompt = prompt.trim();
		
		return prompt;
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
	
	record Names(List<String> males, List<String> females) {
	}
	
}
