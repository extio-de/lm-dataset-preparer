package de.extio.lmdatasetprep.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Model {
	
	private String id;
	
	private String object;
	
	private int created;
	
	private String ownedBy;
	
	private List<String> permission;
	
	private String root;
	
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
	public int getCreated() {
		return this.created;
	}
	
	public void setCreated(final int created) {
		this.created = created;
	}
	
	@JsonProperty("owned_by")
	public String getOwned_by() {
		return this.ownedBy;
	}
	
	public void setOwned_by(final String owned_by) {
		this.ownedBy = owned_by;
	}
	
	@JsonProperty("permission")
	public List<String> getPermission() {
		return this.permission;
	}
	
	public void setPermission(final List<String> permission) {
		this.permission = permission;
	}
	
	@JsonProperty("root")
	public String getRoot() {
		return this.root;
	}
	
	public void setRoot(final String root) {
		this.root = root;
	}
}
