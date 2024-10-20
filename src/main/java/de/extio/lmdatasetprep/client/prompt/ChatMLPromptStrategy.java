package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

@Component
public class ChatMLPromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append("<|im_start|>system\n");
		prompt.append(instruction);
		prompt.append("<|im_end|>\n<|im_start|>user\n");
		prompt.append(question);
		if (!question.isEmpty() && !text.isEmpty()) {
			prompt.append("\n");
		}
		prompt.append(text);
		prompt.append("<|im_end|>\n<|im_start|>assistant\n");
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append(assistant);
		prompt.append("<|im_end|>\n<|im_start|>user\n");
		prompt.append(user);
		prompt.append("<|im_end|>\n<|im_start|>assistant\n");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("<|im_end|>", "");
	}
	
	@Override
	public String getPromptName() {
		return "chatml";
	}
}
