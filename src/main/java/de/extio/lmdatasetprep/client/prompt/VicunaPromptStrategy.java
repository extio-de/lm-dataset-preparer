package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

@Component
public class VicunaPromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append(instruction);
		prompt.append(" USER: ");
		prompt.append(question);
		if (!question.isEmpty() && !text.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(text);
		prompt.append(" ASSISTANT:");
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append(" ");
		prompt.append(assistant);
		prompt.append("</s>USER: ");
		prompt.append(user);
		prompt.append(" ASSISTANT:");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("</s>", "");
	}
	
	@Override
	public String getPromptName() {
		return "vicuna";
	}
	
}
