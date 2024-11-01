package de.extio.lmdatasetprep.client;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import de.extio.lmdatasetprep.client.dto.Choice;
import de.extio.lmdatasetprep.client.dto.CompletionRequest;
import de.extio.lmdatasetprep.client.dto.CompletionResponse;
import de.extio.lmdatasetprep.client.profile.ModelCategory;
import de.extio.lmdatasetprep.client.profile.ModelProfile;
import de.extio.lmdatasetprep.client.profile.ModelProfileService;
import de.extio.lmdatasetprep.client.prompt.PromptStrategy;
import de.extio.lmdatasetprep.client.prompt.PromptStrategyFactory;

@Component
public class Client {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);
	
	@Autowired
	private WebClient.Builder webClientBuilder;
	
	@Autowired
	private PromptStrategyFactory promptStrategyFactory;
	
	@Autowired
	private ModelNameSupplier modelNameSupplier;
	
	@Autowired
	private Tokenizer tokenizer;
	
	@Autowired
	private ModelProfileService modelProfileService;
	
	@Value("${client.collectStatistics}")
	private boolean collectStatistics;
	
	private final Map<String, Integer> promptTemplateTokenLengths = new ConcurrentHashMap<>();
	
	public Completion completion(final String instruction, final String question, final String fullText, final ModelCategory modelCategory) {
		final var modelProfile = this.modelProfileService.getModelProfile(modelCategory.getModelProfile());
		final PromptStrategy promptStrategy = this.promptStrategyFactory.getStrategy(modelProfile.prompt());
		
		final List<String> texts = new ArrayList<>();
		final List<Long> tokenizedInstrAndQuestion = this.tokenizer.tokenize(instruction + question, modelProfile);
		final List<Long> tokenizedFullText = this.tokenizer.tokenize(fullText, modelProfile);
		final int inputTokens = tokenizedInstrAndQuestion.size() + tokenizedFullText.size() + this.getPromptTemplateTokenLength(modelProfile);
		if (inputTokens > modelProfile.maxContextLength() - modelProfile.maxTokens()) {
			final int steps = modelProfile.maxContextLength() - modelProfile.maxTokens() - tokenizedInstrAndQuestion.size() - this.getPromptTemplateTokenLength(modelProfile);
			for (int i = 0; i < tokenizedFullText.size(); i += steps) {
				final List<Long> split = tokenizedFullText.subList(i, Math.min(i + steps, tokenizedFullText.size()));
				texts.add(this.tokenizer.detokenize(split, modelProfile));
			}
		}
		else {
			texts.add(fullText);
		}
		LOGGER.info("Completion request. Input tokens: {}", inputTokens);
		LOGGER.debug("Splitted text into parts: " + texts.size());
		
		final List<StringBuilder> answers = new ArrayList<>();
		
		final CompletionStatistics statistics = new CompletionStatistics();
		for (final String text : texts) {
			final StringBuilder answer = new StringBuilder();
			
			final StringBuilder prompt = promptStrategy.start(instruction, question, text);
			this.requestCompletionsContinuations(prompt, answer, statistics, modelProfile, promptStrategy);
			
			answers.add(answer);
		}
		
		if (answers.size() > 1) {
			LOGGER.debug("Summarizing answers");
			
			final StringBuilder summary = new StringBuilder();
			
			final StringBuilder prompt = promptStrategy.start(instruction, question, "");
			answers.forEach(answer -> promptStrategy.next(prompt, answer.toString(), "Continue"));
			promptStrategy.next(prompt, "", "Generate now a full summary");
			
			this.requestCompletionsContinuations(prompt, summary, statistics, modelProfile, promptStrategy);
			
			answers.clear();
			answers.add(summary);
		}
		
		return new Completion(
				answers.stream().collect(Collectors.joining()),
				statistics.requests,
				statistics.duration,
				statistics.inTokens,
				statistics.outTokens,
				false);
	}
	
	private void requestCompletionsContinuations(final StringBuilder prompt, final StringBuilder answer, final CompletionStatistics statistics,
			final ModelProfile modelProfile, final PromptStrategy promptStrategy) {
		for (int continuation = 0; continuation < modelProfile.maxContinuations(); continuation++) {
			final LocalDateTime start = LocalDateTime.now();
			
			final String promptStr = prompt.toString();
			
			final var response = this.requestCompletions(promptStr, modelProfile);
			
			String content = null;
			var complete = false;
			if (response.getContent() != null) {
				content = promptStrategy.removeEOT(response.getContent());
				complete = response.isStoppedEos();
			}
			else {
				final var choice = response.getChoices().getFirst();
				content = promptStrategy.removeEOT(choice.getText());
				complete = !Choice.FINISH_REASON_LENGTH.equals(choice.getFinishReason());
			}
			
			if (this.collectStatistics) {
				statistics.add(Duration.between(start, LocalDateTime.now()), this.tokenizer.count(promptStr, modelProfile), this.tokenizer.count(content, modelProfile));
			}
			
			answer.append(content);
			
			if (complete) {
				LOGGER.debug("Prompt response complete");
				break;
			}
			
			answer.append('\n');
			promptStrategy.continue_(prompt, content);
		}
	}
	
	private CompletionResponse requestCompletions(final String prompt, final ModelProfile modelProfile) {
		LOGGER.debug("Requesting completion");
		final var webClient = this.webClientBuilder.baseUrl(modelProfile.baseUrl()).build();
		final var request = new CompletionRequest();
		request.setModel(this.modelNameSupplier.getModelName(modelProfile.baseUrl()));
		request.setPrompt(prompt);
		request.setMaxTokens(modelProfile.maxTokens());
		request.setTemperature(modelProfile.temperature());
		request.setTopP(modelProfile.topP());
		request.setStream(false);
		
		return webClient
				.method(HttpMethod.POST)
				.uri(uriBuilder -> uriBuilder.path("/v1/completions").build())
				.bodyValue(request)
				.retrieve()
				.bodyToMono(CompletionResponse.class)
				.block();
	}
	
	private int getPromptTemplateTokenLength(final ModelProfile modelProfile) {
		return this.promptTemplateTokenLengths.computeIfAbsent(modelProfile.prompt(), key -> {
			final PromptStrategy promptStrategy = this.promptStrategyFactory.getStrategy(modelProfile.prompt());
			final var prompt = promptStrategy.start(" ", " ", " ");
			return this.tokenizer.count(prompt.toString(), modelProfile);
		});
	}
	
	static class CompletionStatistics {
		
		int requests;
		
		Duration duration = Duration.ofMillis(0l);
		
		long inTokens;
		
		long outTokens;
		
		void add(final Duration duration, final int inTokens, final int outTokens) {
			LOGGER.debug("Request duration: " + duration + "; in tokens: " + inTokens + "; out tokens: " + outTokens);
			
			this.requests++;
			this.duration = this.duration.plus(duration);
			this.inTokens += inTokens;
			this.outTokens += outTokens;
		}
	}
	
}
