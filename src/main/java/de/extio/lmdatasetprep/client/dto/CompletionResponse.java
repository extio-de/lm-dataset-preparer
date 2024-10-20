package de.extio.lmdatasetprep.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompletionResponse {
	
	private String id;
	
	private String object;
	
	private Integer created;
	
	private List<Choice> choices;
	
	private String content;
	
	private boolean isStoppedEos;
	
	@JsonProperty("id")
	public String getId() {
		return this.id;
	}
	
	public void setId(final String id) {
		this.id = id;
	}
	
	@JsonProperty("object")
	public String getObject() {
		return this.object;
	}
	
	public void setObject(final String object) {
		this.object = object;
	}
	
	@JsonProperty("created")
	public Integer getCreated() {
		return this.created;
	}
	
	public void setCreated(final Integer created) {
		this.created = created;
	}
	
	@JsonProperty("choices")
	public List<Choice> getChoices() {
		return this.choices;
	}
	
	public void setChoices(final List<Choice> choices) {
		this.choices = choices;
	}
	
	@JsonProperty("content")
	public String getContent() {
		return this.content;
	}
	
	public void setContent(final String content) {
		this.content = content;
	}
	
	@JsonProperty("stopped_eos")
	public boolean isStoppedEos() {
		return this.isStoppedEos;
	}
	
	public void setStoppedEos(final boolean isStoppedEos) {
		this.isStoppedEos = isStoppedEos;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CompletionResponse [id=");
		builder.append(this.id);
		builder.append(", object=");
		builder.append(this.object);
		builder.append(", created=");
		builder.append(this.created);
		builder.append(", choices=");
		builder.append(this.choices);
		builder.append(", content=");
		builder.append(this.content);
		builder.append(", isStoppedEos=");
		builder.append(this.isStoppedEos);
		builder.append("]");
		return builder.toString();
	}
}
