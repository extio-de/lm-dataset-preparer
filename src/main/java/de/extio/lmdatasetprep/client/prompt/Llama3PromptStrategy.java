package de.extio.lmdatasetprep.client.prompt;

import org.springframework.stereotype.Component;

/**
 * @see https://llama.meta.com/docs/model-cards-and-prompt-formats/meta-llama-3
 */
@Component
public class Llama3PromptStrategy implements PromptStrategy {
	
	@Override
	public StringBuilder start(final String instruction, final String question, final String text) {
		final StringBuilder prompt = new StringBuilder();
		prompt.append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n");
		prompt.append(instruction);
		prompt.append("<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n");
		prompt.append(question);
		if (!question.isEmpty() && !text.isEmpty()) {
			prompt.append("\n\n");
		}
		prompt.append(text);
		prompt.append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n");
		
		return prompt;
	}
	
	@Override
	public void continue_(final StringBuilder prompt, final String assistant) {
		this.next(prompt, assistant, "Continue");
	}
	
	@Override
	public void next(final StringBuilder prompt, final String assistant, final String user) {
		prompt.append(assistant);
		prompt.append("<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n" + user + "<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n");
	}
	
	@Override
	public String removeEOT(final String prompt) {
		return prompt.strip().replace("<|eot_id|>", "");
	}
	
	@Override
	public String getPromptName() {
		return "llama3";
	}
}
