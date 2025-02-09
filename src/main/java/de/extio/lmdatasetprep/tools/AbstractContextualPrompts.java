package de.extio.lmdatasetprep.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmdatasetprep.Execution;
import de.extio.lmdatasetprep.Execution.WorkPacket;
import de.extio.lmdatasetprep.TextUtils;
import de.extio.lmlib.client.ClientService;
import de.extio.lmlib.client.Completion;
import de.extio.lmlib.client.Conversation;

abstract class AbstractContextualPrompts implements InitializingBean, DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContextualPrompts.class);
	
	private static final byte[] NEWLINE = "\n".getBytes(StandardCharsets.UTF_8);
	
	@Autowired
	protected ClientService clientService;
	
	protected final ObjectMapper mapper = new ObjectMapper();
	
	private List<String> maleNames = new ArrayList<>();
	
	private List<String> femaleNames = new ArrayList<>();
	
	@Override
	public String getModelCategoryPropertyName() {
		return "contextualPrompts.category";
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		try (var in = new ClassPathResource("male.txt").getInputStream()) {
			this.maleNames = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.toList());
		}
		try (var in = new ClassPathResource("female.txt").getInputStream()) {
			this.femaleNames = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.toList());
		}
		
		this.mapper.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
	}
	
	@Override
	public void accept(final Properties properties) {
		final int variations = Integer.parseInt(properties.getProperty("contextualPrompts.variations"));
		final List<ChunkCfg> chunkConfigurations = new ArrayList<>();
		for (int i = 0; i < 99; i++) {
			final String chunkNorm = properties.getProperty("contextualPrompts.chunkNorm." + i, "");
			if (chunkNorm.isBlank()) {
				break;
			}
			final String chunkVar = properties.getProperty("contextualPrompts.chunkVar." + i, "");
			final var cfg = new ChunkCfg(Integer.parseInt(chunkNorm), Integer.parseInt(chunkVar));
			chunkConfigurations.add(cfg);
		}
		
		Execution.transform(properties.getProperty("contextualPrompts.source"), this.createTasks(properties, chunkConfigurations, variations)::createTasks);
	}
	
	@FunctionalInterface
	static interface CreateTasks {
		
		List<Runnable> createTasks(WorkPacket packet);
		
	}
	
	protected abstract CreateTasks createTasks(Properties properties, List<ChunkCfg> chunkConfigurations, int variations);
	
	protected List<String> fileToParagraphs(final WorkPacket packet, final int chunkNorm, final int chunkVar) {
		LOGGER.info("Splitting to jsonl " + packet.file() + " -> " + chunkNorm + "±" + chunkVar);
		final String text = TextUtils.normalizeText(packet.text());
		return TextUtils.splitParagraphs(text, chunkNorm, chunkVar, false);
	}
	
	protected Map<String, String> createCharacterNameMapping(final String paragraph, final Properties properties) {
		final var completion = this.getClient(properties, this.clientService).completion(this.getModelCategory(properties),
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
		try {
			names = this.mapper.readValue(json, Names.class);
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
	
	protected String requestCompletion(final Properties properties, final String paragraph, final String instruction) {
		final var completion = this.getClient(properties, this.clientService).completion(this.getModelCategory(properties),
				"You are an assistant with great text writing skills.",
				instruction,
				paragraph);
		
		return this.processResponse(properties, completion);
	}
	
	protected String requestCompletion(final Properties properties, final Conversation conversation) {
		final var completion = this.getClient(properties, this.clientService).conversation(this.getModelCategory(properties), conversation);
		return this.processResponse(properties, completion);
	}
	
	private String processResponse(final Properties properties, final Completion completion) {
		return TextUtils.normalizeModelResponse(completion.response(), Boolean.parseBoolean(properties.getProperty("contextualPrompts.removePreamble", "false")));
	}
	
	protected void writeJsonLine(final OutputStream fos, final Object line) {
		try {
			this.mapper.writeValue(fos, line);
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
	
	protected record QaLine(String question, String answer) {
	}
	
	protected record HistInstrComplLine(String history, String instruct, String completion) {
	}
	
	protected record ConversationLine(String user, String assistant) {
	}
	
	protected record ConversationsLine(List<ConversationLine> conversation) {
	}
	
	protected record TextLine(String text) {
	}
	
}
