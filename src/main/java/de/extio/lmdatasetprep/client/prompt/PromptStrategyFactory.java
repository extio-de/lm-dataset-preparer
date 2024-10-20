package de.extio.lmdatasetprep.client.prompt;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PromptStrategyFactory {
	
	private final Map<String, PromptStrategy> strategies = new HashMap<>();
	
	public PromptStrategyFactory(final List<PromptStrategy> strategies) {
		strategies.forEach(strategy -> this.strategies.put(strategy.getPromptName(), strategy));
	}
	
	public PromptStrategy getStrategy(final String modelPrompt) {
		return this.strategies.get(modelPrompt);
	}
	
}
