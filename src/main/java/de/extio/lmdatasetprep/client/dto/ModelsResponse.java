package de.extio.lmdatasetprep.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ModelsResponse {
	
	private String object;
	
	private List<Model> data;
	
	@JsonProperty("object")
	public String getObject() {
		return this.object;
	}
	
	public void setObject(final String object) {
		this.object = object;
	}
	
	@JsonProperty("data")
	public List<Model> getData() {
		return this.data;
	}
	
	public void setData(final List<Model> data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ModelsResponse [object=");
		builder.append(this.object);
		builder.append(", data=");
		builder.append(this.data);
		builder.append("]");
		return builder.toString();
	}
	
}
