package de.extio.lmdatasetprep.client.profile;

public enum ModelCategory {
	HOT("profile.hot"),
	COLD("profile.cold");
	
	private final String modelProfile;
	
	ModelCategory(final String modelProfile) {
		this.modelProfile = modelProfile;
	}
	
	public String getModelProfile() {
		return this.modelProfile;
	}
}
