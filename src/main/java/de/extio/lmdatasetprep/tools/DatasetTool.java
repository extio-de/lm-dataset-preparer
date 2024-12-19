package de.extio.lmdatasetprep.tools;

import java.util.Properties;
import java.util.function.Consumer;

import de.extio.lmlib.client.Client;
import de.extio.lmlib.client.ClientService;
import de.extio.lmlib.profile.ModelCategory;

public interface DatasetTool extends Consumer<Properties> {
	
	default String getModelCategoryPropertyName() {
		return null;
	}
	
	default ModelCategory getModelCategory(final Properties properties) {
		return ModelCategory.valueOf(properties.getProperty(this.getModelCategoryPropertyName()));
	}
	
	default Client getClient(final Properties properties, final ClientService clientService) {
		return clientService.getClient(this.getModelCategory(properties));
	}
	
}
