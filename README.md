# lm-dataset-preparer

This program is designed for processing and rewriting text files using a language model. 

Here's a breakdown of its functionality:

1. **Text Preparation:** The program utilizes several classes to prepare the text for processing. It can translate text files to English, normalize text, split text into paragraphs, and convert paragraphs into JSON format.

2. **Contextual Prompt Generation:** For each paragraph, the program generates contextual prompts using an external AI service. These prompts help guide the language model in understanding the context of the paragraph.

3. **Text Rewriting:** The program leverages a language model to rewrite text paragraphs. It supports two types of rewriting: "improvement" and "enhancement," each with its own set of prompts. Multiple enhancement variations are possible by randomly selecting from a list of prompts.

4. **Output:** The rewritten text, along with the generated prompts, is saved in a JSONL file.

**Purpose:**

The overall purpose of this program is to facilitate the rewriting and improvement of text files using a language model. The contextual prompts and different rewriting modes allow for flexible and nuanced text manipulation. This could be useful for tasks such as fine-tuning of large language models (LLMs).

*[gemma-2-27b-it-Q4_K_L.gguf; requests=6, requestDuration=PT1M36.661188325S, inTokens=6473, outTokens=506, tps=5.270833333333333]*
