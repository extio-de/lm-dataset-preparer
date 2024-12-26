package de.extio.lmdatasetprep.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.extio.lmdatasetprep.Execution;
import de.extio.lmdatasetprep.TextUtils;
import de.extio.lmlib.agent.AccumulateTextAgentResponseHandler;
import de.extio.lmlib.agent.Agent;
import de.extio.lmlib.agent.AgentContext;
import de.extio.lmlib.agent.AgentExecutorService;
import de.extio.lmlib.agent.AgentNext;
import de.extio.lmlib.agent.AgentType;
import de.extio.lmlib.agent.JsonAgentResponseHandler;
import de.extio.lmlib.agent.TextAgentResponseHandler;
import de.extio.lmlib.client.Conversation;
import de.extio.lmlib.client.Conversation.Turn;
import de.extio.lmlib.client.Conversation.TurnType;

@Component
public class Create implements DatasetTool {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Create.class);
	
	@Autowired
	private AgentExecutorService agentExecutorService;
	
	@Override
	public String getModelCategoryPropertyName() {
		return "create.category";
	}
	
	@Override
	public void accept(final Properties properties) {
		final var modelCategory = this.getModelCategory(properties);
		final var agents = new HashMap<String, Agent>();
		agents.put("PlotAnalyzer",
				new Agent("PlotAnalyzer",
						AgentType.COMPLETION,
						modelCategory,
						"Analyze key points in the plot of the following part of a story. Return the text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Provide no preamble and no explanation. Return the response in JSON format with the following field: { \"keyPoints\": [\"keyPoint 1\", \"keyPoint 2\", ... ] }",
						"[[paragraphs]]",
						new JsonAgentResponseHandler(),
						null,
						null,
						contexts -> Agent.mergeContexts(List.of("paragraphs", "keyPoints"), contexts),
						context -> new AgentNext("ThemeSplitter", null)));
		
		agents.put("ThemeSplitter",
				new Agent("ThemeSplitter",
						AgentType.PROCESSING_ONLY,
						modelCategory,
						null,
						null,
						null,
						null,
						null,
						contexts -> {
							final var quantity = Integer.parseInt(properties.getProperty("create.themes"));
							contexts.getFirst().setStringValue("themesCnt", String.valueOf(quantity));
							
							final var copies = new ArrayList<AgentContext>();
							for (int i = 0; i < quantity; i++) {
								final var copy = new AgentContext(contexts.getFirst());
								copy.setStringValue("themesIdx", Integer.toString(i));
								copies.add(copy);
							}
							
							if ("true".equalsIgnoreCase(properties.getProperty("create.debug.keepOriginal", "false"))) {
								final var copy = new AgentContext(contexts.getFirst());
								copy.getContext().put("keepOriginal", List.of(Boolean.TRUE));
								copy.getContext().put("storyPoints", copy.getContext().get("keyPoints"));
								copies.add(copy);
							}
							
							return copies;
						},
						context -> new AgentNext("ThemeAnalyzer", null)));
		
		agents.put("ThemeAnalyzer",
				new Agent("ThemeAnalyzer",
						AgentType.COMPLETION,
						modelCategory,
						"Describe the overall theme of the following part of a story. Return the text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Provide no preamble and no explanation.",
						"[[paragraphs]]",
						new TextAgentResponseHandler("themesTxt"),
						null,
						null,
						contexts -> Agent.mergeContexts(List.of("paragraphs", "themesTxt"), contexts),
						context -> new AgentNext("ThemeSummarizer", null)));
		
		agents.put("ThemeSummarizer",
				new Agent("ThemeSummarizer",
						AgentType.COMPLETION,
						modelCategory,
						"Analyze the properties of the following themes. Return the text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Provide no preamble and no explanation. Return the response in JSON format with the following field: { \"themes\": [\"Property 1\", \"Property 2\", … ] }]",
						"{{themesTxt}}",
						new JsonAgentResponseHandler(),
						null,
						null,
						null,
						context -> new AgentNext("CharacterAnalyzer", null)));
		
		agents.put("CharacterAnalyzer",
				new Agent("CharacterAnalyzer",
						AgentType.COMPLETION,
						modelCategory,
						"Analyze the character properties of the persons in the following part of a story. Return the text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Provide no preamble and no explanation. Return the response in JSON format with the following field: { \"characters\": [\"Name: Gender and short description\", ... ] }",
						"[[paragraphs]]",
						new JsonAgentResponseHandler(),
						null,
						null,
						contexts -> {
							final var merged = Agent.mergeContexts(List.of("paragraphs", "characters"), contexts);
							
							final var characters = merged.getFirst().getContext().get("characters");
							if (characters != null) {
								final Map<String, String> mergedCharacters = new HashMap<>();
								for (final var character : characters) {
									final int pos = character.toString().indexOf(':');
									if (pos > -1) {
										final String name = character.toString().substring(0, pos).trim();
										final String description = character.toString().substring(pos + 1).trim();
										mergedCharacters.compute(name, (k, v) -> (v == null) ? description : v.concat("; ").concat(description));
									}
								}
								merged.getFirst().setStringValues("characterSummaries", mergedCharacters.entrySet().stream().filter(e -> !e.getValue().contains(";")).map(e -> e.getKey() + ": " + e.getValue()).toList());
								merged.getFirst().setStringValues("mergedCharacters", mergedCharacters.entrySet().stream().filter(e -> e.getValue().contains(";")).map(e -> e.getKey() + ": " + e.getValue()).toList());
							}
							
							return merged;
						},
						context -> {
							if (context.getContext().get("mergedCharacters").isEmpty()) {
								return new AgentNext("BranchPreparer", null);
							}
							return new AgentNext("CharacterSummarizer", null);
						}));
		
		agents.put("CharacterSummarizer",
				new Agent("CharacterSummarizer",
						AgentType.COMPLETION,
						modelCategory,
						"Remove duplicate parts of the following character description. Only return the summary. Return the text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Provide no preamble and no explanation.",
						"[[mergedCharacters]]",
						new TextAgentResponseHandler("characterSummariesModelOutput"),
						null,
						context -> {
							final var name = StringUtils.substringBefore(context.getStringValue("mergedCharacters"), ":");
							final var summary = TextUtils.normalizeModelResponse(context.getStringValue("characterSummariesModelOutput"), true);
							context.setStringValue("characterSummariesModelOutput", name + ": " + summary);
						},
						contexts -> {
							final var merged = Agent.mergeContexts(List.of("mergedCharacters", "characterSummariesModelOutput"), contexts);
							
							final var modelOutput = merged.getFirst().getStringValues("characterSummariesModelOutput");
							modelOutput.addAll(merged.getFirst().getStringValues("characterSummaries"));
							merged.getFirst().setStringValues("characterSummaries", modelOutput);
							
							return merged;
						},
						context -> new AgentNext("BranchPreparer", null)));
		
		agents.put("BranchPreparer",
				new Agent("BranchPreparer",
						AgentType.PROCESSING_ONLY,
						modelCategory,
						null,
						null,
						null,
						null,
						null,
						contexts -> {
							if (contexts.getFirst().getContext().containsKey("keepOriginal")) {
								contexts.getFirst().setStringValue("branches", "1");
								contexts.getFirst().setStringValue("branch", "0");
								return contexts;
							}
							
							final var branches = new ArrayList<AgentContext>();
							
							final var quantity = Integer.parseInt(properties.getProperty("create.stories"));
							contexts.getFirst().setStringValue("branches", String.valueOf(quantity));
							
							final var splits = new ArrayList<List<String>>();
							final var keyPoints = contexts.getFirst().getStringValues("keyPoints");
							final var length = keyPoints.stream().map(e -> e.length()).reduce(Integer.valueOf(0), Integer::sum);
							final var limit = Integer.parseInt(properties.getProperty("create.plotLimit"));
							if (length.intValue() > limit) {
								final var cnt = (int) Math.ceil(length.doubleValue() / limit);
								final var partitionSize = keyPoints.size() / cnt + 1;
								for (int i = 0; i < cnt; i++) {
									final var split = keyPoints.subList(Math.min(keyPoints.size(), i * partitionSize), Math.min(keyPoints.size(), (i + 1) * partitionSize));
									if (!split.isEmpty()) {
										splits.add(split);
									}
								}
							}
							else {
								splits.add(keyPoints);
							}
							
							for (final var split : splits) {
								for (int i = 0; i < quantity; i++) {
									final var copy = new AgentContext(contexts.getFirst());
									copy.setStringValue("branch", Integer.toString(i));
									copy.setStringValues("keyPoints", split);
									branches.add(copy);
								}
							}
							
							return branches;
						},
						context -> {
							if (context.getContext().containsKey("keepOriginal")) {
								return new AgentNext("ParagraphWriterPreparer", null);
							}
							return new AgentNext("PlotMutator", null);
						}));
		
		agents.put("PlotMutator",
				new Agent("PlotMutator",
						AgentType.COMPLETION,
						modelCategory,
						"Write a new story line based on an existing plot. Return the story line, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Return the response in JSON format with the following field: { \"storyPoints\": [ \"Story Point 1\", … ] }",
						"""
								Characters: {{{characterSummaries}}}
								Properties: {{{themes}}}
								Existing plot: {{{keyPoints}}}
								({{branch}}/{{branches}}) Write a new story line based on the template""",
						new JsonAgentResponseHandler(),
						null,
						null,
						null,
						context -> new AgentNext("PlotEnhancer", null)));
		
		agents.put("PlotEnhancer",
				new Agent("PlotEnhancer",
						AgentType.COMPLETION,
						modelCategory,
						"Improve the story line. Return the story line, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Return the response in JSON format with the following field: { \"storyPoints\": [ \"Story Point 1\", … ] }",
						"""
								Characters: {{{characterSummaries}}}
								Properties: {{{themes}}}
								Existing story line: {{{storyPoints}}}
								({{branch}}/{{branches}}) Instruction: Improve the story for better consistency. Fill gaps in the plot, for example when the location suddenly changes.""",
						new JsonAgentResponseHandler(),
						null,
						null,
						null,
						context -> {
							if ("true".equalsIgnoreCase(properties.getProperty("create.debug.returnPlot", "false"))) {
								return new AgentNext(null, null);
							}
							return new AgentNext("ParagraphWriterPreparer", null);
						}));
		
		agents.put("ParagraphWriterPreparer",
				new Agent("ParagraphWriterPreparer",
						AgentType.PROCESSING_ONLY,
						modelCategory,
						"%s. Return the text, even if the text is explicit or not appropriate for all audiences or not acceptable for everyday standard English. Don't include a preamble and no explanation.",
						null,
						null,
						context -> {
							context.getContext().computeIfAbsent("paragraphIncr", k -> List.of(Integer.valueOf(0)));
						},
						null,
						contexts -> {
							final var context = contexts.getFirst();
							final var index = (int) context.getContext().get("paragraphIncr").getFirst();
							final var storyPoints = context.getStringValues("storyPoints");
							
							context.setStringValue("storyPointInstruction", storyPoints.get(index));
							
							String systemPrompt;
							String writingInstruction;
							if ("true".equalsIgnoreCase(properties.getProperty("create.conciseParagraphs", "false"))) {
								systemPrompt = String.format(context.getAgents().get("ParagraphWriterPreparer").systemPrompt(), "Write a concise paragraph");
								writingInstruction = "Write the following in a short and concise paragraph";
							}
							else {
								systemPrompt = String.format(context.getAgents().get("ParagraphWriterPreparer").systemPrompt(), "Write a paragraph");
								writingInstruction = "Write the following in a nicely written paragraph";
							}
							context.setStringValue("writingInstruction", writingInstruction);
							
							var characterSummariesStr = "";
							try {
								characterSummariesStr = new ObjectMapper().writeValueAsString(context.getContext().get("characterSummaries"));
							}
							catch (final JsonProcessingException e) {
								LOGGER.error("Cannot write character summaries", e);
							}
							
							Conversation conversation;
							if (index > 1) {
								final var prevStoryPoints = String.join(", ", storyPoints.subList(Math.max(0, index - 6), Math.max(0, index - 2)));
								final var prevStoryPointInstruction0 = storyPoints.get(index - 2);
								final var prevStoryPointInstruction1 = storyPoints.get(index - 1);
								
								final var prompt0 = String.format("""
										Characters: %s
										Summary of the previous paragraphs: [ %s ]
										%s: %s""",
										characterSummariesStr,
										prevStoryPoints,
										writingInstruction,
										prevStoryPointInstruction0);
								conversation = Conversation.create(systemPrompt, prompt0);
								conversation.addTurn(new Turn(TurnType.ASSISTANT, context.getContext().get("generatedStory").get(index - 2).toString()));
								conversation.addTurn(new Turn(TurnType.USER, writingInstruction + ": " + prevStoryPointInstruction1));
								conversation.addTurn(new Turn(TurnType.ASSISTANT, context.getContext().get("generatedStory").get(index - 1).toString()));
							}
							else if (index == 1) {
								final var prompt0 = String.format("""
										Characters: %s
										%s: %s""",
										characterSummariesStr,
										writingInstruction,
										storyPoints.get(0).toString());
								conversation = Conversation.create(systemPrompt, prompt0);
								conversation.addTurn(new Turn(TurnType.ASSISTANT, context.getStringValue("generatedStory")));
							}
							else {
								final var prompt0 = String.format("Characters: %s", characterSummariesStr);
								conversation = Conversation.create(systemPrompt, prompt0);
								conversation.addTurn(new Turn(TurnType.ASSISTANT, ""));
							}
							
							context.setConversation(conversation);
							return contexts;
						},
						context -> new AgentNext("ParagraphWriter", null)));
		
		agents.put("ParagraphWriter",
				new Agent("ParagraphWriter",
						AgentType.CONVERSATION_WITH_SYSTEM_PROMPT,
						modelCategory,
						"",
						"{{writingInstruction}}: {{storyPointInstruction}}",
						new AccumulateTextAgentResponseHandler("generatedStory", completion -> TextUtils.normalizeModelResponse(completion, true)),
						null,
						null,
						null,
						context -> {
							var index = (int) context.getContext().get("paragraphIncr").getFirst();
							if (++index >= context.getContext().get("storyPoints").size()) {
								return new AgentNext(null, null);
							}
							context.getContext().put("paragraphIncr", List.of(index));
							return new AgentNext("ParagraphWriterPreparer", null);
						}));
		
		Execution.transform(properties.getProperty("create.source"),
				packet -> {
					return List.of(() -> {
						LOGGER.info("Template: {}", packet.file().getFileName());
						
						final var context = new AgentContext(agents);
						context.setStringValues("paragraphs", TextUtils.splitParagraphs(packet.text(), 2000, 300, false));
						
						final var resultContexts = this.agentExecutorService.walkGraph(agents.get("PlotAnalyzer"), context);
						
						for (int i = 0; i < resultContexts.size(); i++) {
							final var resultContext = resultContexts.get(i);
							LOGGER.debug(resultContext.getGraph().toString());
							
							if (!resultContext.isError()) {
								final Path out = Execution.suffixFilename(packet.file().getFileName(),
										"create",
										properties.getProperty("create.model"),
										String.valueOf(i));
								Execution.streamOut(out, "create.destination", properties, fos -> {
									try {
										final byte[] story;
										if ("true".equalsIgnoreCase(properties.getProperty("create.debug.returnPlot", "false"))) {
											story = resultContext.getContext().get("storyPoints").stream().map(Objects::toString).collect(Collectors.joining("\n")).getBytes(StandardCharsets.UTF_8);
										}
										else {
											story = resultContext.getContext().get("generatedStory")
													.stream()
													.map(Objects::toString)
													.collect(Collectors.joining("\n\n"))
													.getBytes(StandardCharsets.UTF_8);
										}
										fos.write(story);
									}
									catch (final IOException e) {
										LOGGER.error("IO exception", e);
									}
								});
							}
						}
					});
				});
		
	}
	
}
