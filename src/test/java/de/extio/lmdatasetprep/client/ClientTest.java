package de.extio.lmdatasetprep.client;

import java.time.LocalDateTime;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import de.extio.lmdatasetprep.client.profile.ModelCategory;

@Disabled
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public class ClientTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ClientTest.class);
	
	@Autowired
	private Client client;
	
	@Test
	void multiPrompts() throws Exception {
		final var completion = this.client.completion(
				"You are a clever story teller",
				"Continue this story:",
				"Once upon a time, there was a spaceship travelling to a black hole. The crew were cats!",
				ModelCategory.HOT);
		LOGGER.info(completion.response());
	}
	
	@Test
	void hugePrompt() throws Exception {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 10000; i++) {
			sb.append(i);
			sb.append(", ");
		}
		sb.delete(sb.length() - 2, sb.length());
		
		final var completion = this.client.completion(
				"You are counting machine",
				"How many numbers do you count?",
				sb.toString(),
				ModelCategory.HOT);
		LOGGER.info(completion.response());
	}
	
	@Test
	void parallelRequests() throws Exception {
		final LocalDateTime start = LocalDateTime.now();
		
		final Tasks tasks = this.createTasks();
		
		final ExecutorService executor = Executors.newFixedThreadPool(4);
		try {
			CompletableFuture.allOf(
					CompletableFuture.runAsync(tasks.task1, executor),
					CompletableFuture.runAsync(tasks.task2, executor),
					CompletableFuture.runAsync(tasks.task3, executor),
					CompletableFuture.runAsync(tasks.task4, executor))
					.join();
		}
		finally {
			executor.shutdown();
		}
		
		LOGGER.info("Duration: " + java.time.Duration.between(start, LocalDateTime.now()));
	}
	
	@Test
	void serialRequests() throws Exception {
		final LocalDateTime start = LocalDateTime.now();
		
		final Tasks tasks = this.createTasks();
		
		tasks.task1.run();
		tasks.task2.run();
		tasks.task3.run();
		tasks.task4.run();
		
		LOGGER.info("Duration: " + java.time.Duration.between(start, LocalDateTime.now()));
	}
	
	private Tasks createTasks() {
		return new Tasks(() -> {
			try {
				final var completion = this.client.completion(
						"You are a helpful assistant",
						"",
						"How do I calculate the annual profit margin?",
						ModelCategory.COLD);
				LOGGER.info(completion.response());
			}
			catch (final Exception e) {
				LOGGER.error("Exception", e);
			}
		}, () -> {
			try {
				final var completion = this.client.completion(
						"You are a helpful assistant",
						"",
						"How do I calculate the operating profit ratio?",
						ModelCategory.COLD);
				LOGGER.info(completion.response());
			}
			catch (final Exception e) {
				LOGGER.error("Exception", e);
			}
		}, () -> {
			try {
				final var completion = this.client.completion(
						"You are a helpful assistant",
						"",
						"How do I calculate the return of investment?",
						ModelCategory.COLD);
				LOGGER.info(completion.response());
			}
			catch (final Exception e) {
				LOGGER.error("Exception", e);
			}
		}, () -> {
			try {
				final var completion = this.client.completion(
						"You are a helpful assistant",
						"",
						"How do I calculate the return on net worth?",
						ModelCategory.COLD);
				LOGGER.info(completion.response());
			}
			catch (final Exception e) {
				LOGGER.error("Exception", e);
			}
		});
	}
	
	static record Tasks(Runnable task1, Runnable task2, Runnable task3, Runnable task4) {
	}
}
