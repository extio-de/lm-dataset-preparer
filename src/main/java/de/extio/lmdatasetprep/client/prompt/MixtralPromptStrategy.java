package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

@Component
public class MixtralPromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append("<s>[INST] ");
		prompt.append(instruction);
		if (!instruction.isEmpty() && !question.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(question);
		if ((!instruction.isEmpty() || !question.isEmpty()) && !text.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(text);
		prompt.append(" [/INST]");
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append("\n\"");
		prompt.append(assistant);
		prompt.append("\"</s> [INST] ");
		prompt.append(user);
		prompt.append(" [/INST]");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("</s>", "");
	}
	
	@Override
	public String getPromptName() {
		return "mixtral";
	}
	
}
