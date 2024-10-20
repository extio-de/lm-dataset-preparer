package de.extio.lmdatasetprep.client.prompt;

public interface PromptStrategy {
	
	String getPromptName();
	
	StringBuilder start(String instruction, String question, String text);
	
	void continue_(StringBuilder prompt, String assistant);
	
	void next(StringBuilder prompt, String assistant, String user);
	
	default String removeEOT(final String prompt) {
		return prompt.strip();
	}
	
}
