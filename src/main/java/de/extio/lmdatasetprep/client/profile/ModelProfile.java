package de.extio.lmdatasetprep.client.profile;

public record ModelProfile(
		String prompt,
		int maxTokens,
		int maxContextLength,
		double temperature,
		double topP,
		int maxContinuations,
		String baseUrl) {
}
