package de.extio.lmdatasetprep.preparer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmlib.client.ClientService;
import de.extio.lmlib.profile.ModelCategory;

abstract class AbstractContextualPrompts implements InitializingBean, Consumer<String[]> {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContextualPrompts.class);
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	@Autowired
	protected ClientService clientService;
	
	private List<String> maleNames = new ArrayList<>();
	
	private List<String> femaleNames = new ArrayList<>();
	
	@Override
	public void afterPropertiesSet() throws Exception {
		this.maleNames = new BufferedReader(new InputStreamReader(new ClassPathResource("male.txt").getInputStream())).lines().collect(Collectors.toList());
		this.femaleNames = new BufferedReader(new InputStreamReader(new ClassPathResource("female.txt").getInputStream())).lines().collect(Collectors.toList());
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
		
		Utils.transformDirectory(args[1], this.createTasks(args, chunkConfigurations, variations)::createTasks);
	}
	
	@FunctionalInterface
	static interface CreateTasks {
		
		List<Runnable> createTasks(Path path);
		
	}
	
	protected abstract CreateTasks createTasks(String[] args, List<ChunkCfg> chunkConfigurations, int variations);
	
	protected List<String> fileToParagraphs(final Path file, final int chunkNorm, final int chunkVar) {
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
	
	protected Map<String, String> createCharacterNameMapping(final String paragraph) {
		final var completion = this.clientService.getClient(ModelCategory.COLD).completion(ModelCategory.COLD,
				"You are a helpful assistant.",
				"Extract all names and genders from the following text. Return the result in json format with the following fields: {\"males\" : [ \"Male name 1\", \"Male name 2\", … ], \"females\" : [ \"Female name 1\", \"Female name 2\", … ]}. Don't return a preamble and no explanation:",
				paragraph);
		
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
		
		final Random random = ThreadLocalRandom.current();
		final Map<String, String> mapping = new HashMap<>();
		if (names.males != null) {
			names.males.forEach(name -> mapping.put(name, this.maleNames.get(random.nextInt(this.maleNames.size()))));
		}
		if (names.females != null) {
			names.females.forEach(name -> mapping.put(name, this.femaleNames.get(random.nextInt(this.femaleNames.size()))));
		}
		mapping.keySet().removeIf(key -> key.length() < 5);
		if (mapping.isEmpty()) {
			return Map.of();
		}
		
		LOGGER.debug(mapping.toString());
		return mapping;
	}
	
	protected String renameCharacters(final String paragraph, final Map<String, String> mapping) {
		var result = paragraph;
		for (final var entry : mapping.entrySet()) {
			result = result.replaceAll("(?i)" + entry.getKey(), entry.getValue());
		}
		
		return result;
	}
	
	protected String paragraphToContextualPrompt(final String paragraph, final String instruction) {
		final var completion = this.clientService.getClient(ModelCategory.COLD).completion(ModelCategory.COLD,
				"You are an assistant with great text writing skills.",
				instruction,
				paragraph);
		
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
	
	protected void writeJsonLine(final ObjectMapper mapper, final OutputStream fos, final Object line) {
		try {
			final var jsonb = mapper
					.writeValueAsString(line)
					.getBytes(StandardCharsets.UTF_8);
			fos.write(jsonb);
			fos.write(NEWLINE);
		}
		catch (final IOException e) {
			LOGGER.error("Cannot convert split to json", e);
		}
	}
	
	protected record ChunkCfg(int chunksNorm, int chunksVar) {
	}
	
	protected record Names(List<String> males, List<String> females) {
	}
}
