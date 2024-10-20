package de.extio.lmdatasetprep.client;

import java.time.Duration;

public record Completion(String response, int requests, Duration duration, long inTokens, long outTokens, boolean cached) {
	
}
