package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

@Component
public class Gemma2PromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append("<start_of_turn>user\n");
		if (!instruction.isEmpty()) {
			prompt.append(instruction);
			prompt.append("\n");
		}
		prompt.append(question);
		if (!question.isEmpty() && !text.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(text);
		prompt.append("<end_of_turn>\n<start_of_turn>model\n");
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append(assistant);
		prompt.append("<end_of_turn>\n<start_of_turn>user\n");
		prompt.append(user);
		prompt.append("<end_of_turn>\n<start_of_turn>model\n");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("<end_of_turn>", "");
	}
	
	@Override
	public String getPromptName() {
		return "gemma2";
	}
	
}
