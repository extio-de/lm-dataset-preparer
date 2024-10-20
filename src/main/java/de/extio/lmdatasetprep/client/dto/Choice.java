package de.extio.lmdatasetprep.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Choice {
	
	public static final String FINISH_REASON_STOP = "stop";
	
	public static final String FINISH_REASON_LENGTH = "length";
	
	public static final String FINISH_REASON_CONTENT_FILTER = "content_filter";
	
	private String text;
	
	private Double score;
	
	private String finishReason;
	
	@JsonProperty("text")
	public String getText() {
		return this.text;
	}
	
	public void setText(final String text) {
		this.text = text;
	}
	
	@JsonProperty("score")
	public Double getScore() {
		return this.score;
	}
	
	public void setScore(final Double score) {
		this.score = score;
	}
	
	@JsonProperty("finish_reason")
	public String getFinishReason() {
		return this.finishReason;
	}
	
	public void setFinishReason(final String finishReason) {
		this.finishReason = finishReason;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Choice [text=");
		builder.append(this.text);
		builder.append(", score=");
		builder.append(this.score);
		builder.append(", finishReason=");
		builder.append(this.finishReason);
		builder.append("]");
		return builder.toString();
	}
	
}
