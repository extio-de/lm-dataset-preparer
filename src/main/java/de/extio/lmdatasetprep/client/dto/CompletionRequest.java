package de.extio.lmdatasetprep.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompletionRequest {
	
	private String model;
	
	private String prompt;
	
	private Integer maxTokens;
	
	private Double temperature;
	
	private Double topP;
	
	private Boolean stream;
	
	@JsonProperty("model")
	public String getModel() {
		return this.model;
	}
	
	public void setModel(final String model) {
		this.model = model;
	}
	
	@JsonProperty("prompt")
	public String getPrompt() {
		return this.prompt;
	}
	
	public void setPrompt(final String prompt) {
		this.prompt = prompt;
	}
	
	@JsonProperty("max_tokens")
	public Integer getMaxTokens() {
		return this.maxTokens;
	}
	
	public void setMaxTokens(final Integer maxTokens) {
		this.maxTokens = maxTokens;
	}
	
	@JsonProperty("temperature")
	public Double getTemperature() {
		return this.temperature;
	}
	
	public void setTemperature(final Double temperature) {
		this.temperature = temperature;
	}
	
	@JsonProperty("top_p")
	public Double getTopP() {
		return this.topP;
	}
	
	public void setTopP(final Double topP) {
		this.topP = topP;
	}
	
	@JsonProperty("stream")
	public Boolean isStream() {
		return this.stream;
	}
	
	public void setStream(final Boolean stream) {
		this.stream = stream;
	}
}
