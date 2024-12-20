# lm-dataset-preparer

This program is a comprehensive text processing and data generation pipeline designed for training and evaluating language models. 

**Here's a breakdown of its functionality:**

1. **Data Ingestion and Preparation:** The program reads text files, splits them into paragraphs, and performs various preprocessing steps like normalization and cleaning. It can also extract names from the text and replace them with randomly selected alternatives.

2. **Prompt Generation:**  The program generates different types of prompts based on the input text:
    * **Contextual Prompts:** It creates question-answer pairs that encourage summarization or continuation of the text, incorporating context from previous paragraphs.
    * **Instruction-Completion Pairs:** It uses predefined templates to generate instructions based on text chunks, suitable for training instruction-following models.

3. **Text Rewriting and Translation:** The program can rewrite text using a language model, applying different rewriting strategies. It also offers text translation capabilities, converting text from a source language to English.

4. **Data Formatting and Output:** The program formats the processed data into JSON Lines (JSONL) format, a common standard for machine learning datasets. It can merge multiple JSONL files, shuffle data, and write output to specified files.

5. **Language Model Interaction:** The program provides a standardized way to interact with different language models, allowing users to configure and access them based on predefined categories.

6. **Execution and Task Management:** The program utilizes a thread pool for efficient parallel processing of tasks, managing queues and scheduling for optimal performance.

**Purpose:**

The primary purpose of this program is to generate high-quality, diverse datasets for training and evaluating language models. By providing various text processing, prompt generation, and data formatting capabilities, the program enables researchers and developers to create customized datasets tailored to their specific needs.

*gemma-2-27b-it-Q4_K_M.gguf; requests=12, requestDuration=PT1M28.244075765S, inTokens=12090, outTokens=880, tps=10.0*
