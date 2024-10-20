package de.extio.lmdatasetprep.client;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import de.extio.lmdatasetprep.client.profile.ModelProfile;

@Component
public class Tokenizer {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(Tokenizer.class);
	
	@Autowired
	private WebClient.Builder webClientBuilder;
	
	public List<Long> tokenize(final String txt, final ModelProfile modelProfile) {
		LOGGER.debug("Requesting tokenizing");
		final var webClient = this.webClientBuilder.baseUrl(modelProfile.baseUrl()).build();
		final var request = new TokenizeRequest(txt);
		final var response = webClient
				.method(HttpMethod.POST)
				.uri(uriBuilder -> uriBuilder.path("/tokenize").build())
				.bodyValue(request)
				.retrieve()
				.bodyToMono(TokenizeResponse.class)
				.block();
		return response.tokens();
	}
	
	public int count(final String txt, final ModelProfile modelProfile) {
		return this.tokenize(txt, modelProfile).size();
	}
	
	public String detokenize(final List<Long> tokens, final ModelProfile modelProfile) {
		LOGGER.debug("Requesting detokenizing");
		final var webClient = this.webClientBuilder.baseUrl(modelProfile.baseUrl()).build();
		final var request = new DetokenizeRequest(tokens);
		final var response = webClient
				.method(HttpMethod.POST)
				.uri(uriBuilder -> uriBuilder.path("/detokenize").build())
				.bodyValue(request)
				.retrieve()
				.bodyToMono(DetokenizeResponse.class)
				.block();
		return response.content();
	}
	
	private record TokenizeRequest(String content) {
	}
	
	private record TokenizeResponse(List<Long> tokens) {
	}
	
	private record DetokenizeRequest(List<Long> tokens) {
	}
	
	private record DetokenizeResponse(String content) {
	}
	
}
