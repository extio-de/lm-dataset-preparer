package de.extio.lmdatasetprep.client;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;

import de.extio.lmdatasetprep.client.dto.Model;
import de.extio.lmdatasetprep.client.dto.ModelsResponse;

public class ModelNameLoader implements Runnable {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ModelNameLoader.class);
	
	private final WebClient webClient;
	
	private final Consumer<String> consumer;
	
	public ModelNameLoader(final WebClient webClient, final Consumer<String> consumer) {
		this.consumer = consumer;
		this.webClient = webClient;
	}
	
	@Override
	public void run() {
		String modelName = null;
		do {
			try {
				LOGGER.info("Querying model name");
				
				modelName = this.webClient
						.method(HttpMethod.GET)
						.uri(uriBuilder -> uriBuilder.path("/v1/models").build())
						.retrieve()
						.bodyToMono(ModelsResponse.class)
						.block()
						.getData()
						.stream()
						.map(Model::getId)
						.findFirst()
						.orElseThrow();
			}
			catch (final Exception e) {
				LOGGER.error(e.getMessage(), e);
				try {
					Thread.sleep(1000l);
				}
				catch (final InterruptedException e1) {
					throw new RuntimeException("Interrupted", e1);
				}
			}
		}
		while (modelName == null);
		
		LOGGER.info("Model name: {}", modelName);
		this.consumer.accept(modelName);
	}
	
}
