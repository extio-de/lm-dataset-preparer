package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

@Component
public class Phi3PromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append("<|user|>\n");
		prompt.append(instruction);
		prompt.append(" ");
		prompt.append(question);
		if (!question.isEmpty() && !text.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(text);
		prompt.append(" <|end|>\n<|assistant|>");
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append("\n");
		prompt.append(assistant);
		prompt.append("<|user|>\n");
		prompt.append(user);
		prompt.append("<|end|>\n<|assistant|>");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("<|end|>", "");
	}
	
	@Override
	public String getPromptName() {
		return "phi3";
	}
}
