package de.extio.lmdatasetprep.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import de.extio.lmdatasetprep.client.profile.ModelProfileService;

@Component
public class ModelNameSupplier implements InitializingBean {
	
	@Autowired
	private WebClient.Builder webClientBuilder;
	
	private final Map<String, CompletableFuture<String>> modelNames = new ConcurrentHashMap<>();
	
	private final Map<String, CountDownLatch> countDownLatches = new ConcurrentHashMap<>();
	
	@Autowired
	private ModelProfileService modelProfileService;
	
	@Override
	public void afterPropertiesSet() throws Exception {
		final var urls = this.modelProfileService.getModelProfileUrls();
		for (final var url : urls) {
			this.countDownLatches.put(url, new CountDownLatch(1));
			this.fetchModelName(url);
		}
	}
	
	private void fetchModelName(final String url) {
		final var webClient = this.webClientBuilder.baseUrl(url).build();
		CompletableFuture.runAsync(() -> {
			final var modelNameLoader = new ModelNameLoader(webClient, modelName -> {
				this.modelNames.put(url, CompletableFuture.completedFuture(modelName));
				this.countDownLatches.get(url).countDown();
			});
			modelNameLoader.run();
		});
	}
	
	public String getModelName(final String url) {
		try {
			this.countDownLatches.get(url).await();
		}
		catch (final InterruptedException e) {
			throw new RuntimeException("Interrupted", e);
		}
		return this.modelNames.get(url).join();
	}
	
}
