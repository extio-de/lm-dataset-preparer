package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

@Component
public class AlpacaShortPromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append("### Instruction:\n");
		prompt.append(instruction);
		prompt.append("\n\n### Input:\n");
		prompt.append(question);
		if (!question.isEmpty() && !text.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(text);
		prompt.append("\n\n### Response:\n");
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append(assistant);
		prompt.append("\n\n### Input:\n");
		prompt.append(user);
		prompt.append("\n\n### Response:\n");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("</s>", "");
	}
	
	@Override
	public String getPromptName() {
		return "alpaca";
	}
	
}
