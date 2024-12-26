# lm-dataset-preparer

This program is a comprehensive system for generating and processing text datasets for training language models. 

**Purpose:**

The program aims to create diverse and high-quality text datasets by leveraging various text processing techniques and language models. It enables users to:

* **Prepare and Transform Data:** The program can load, clean, translate, rewrite, and convert text data into different formats (JSON, JSONL).
* **Generate Contextual Prompts:** It can generate question-answer pairs and prompts based on input text, encouraging models to summarize, continue, or analyze the given content.
* **Create Synthetic Stories:** A dedicated tool uses agents to analyze input text, extract plot points, generate new storylines, and write complete stories.

**Workflow:**

The program utilizes a modular design with different classes responsible for specific tasks:

1. **Data Loading and Execution:** The `Execution` class manages file processing, threading, and task execution, allowing for concurrent processing of multiple tools.
2. **Text Processing:** Classes like `TextProcessor` handle text normalization, splitting, and preamble removal.
3. **Translation and Rewriting:** Tools translate text to English and rewrite it using language models for improvement or enhancement.
4. **Prompt Generation:** Classes generate contextual prompts and question-answer pairs based on input text, leveraging character mapping and predefined templates.
5. **Story Generation:** The `Create` class uses agents to analyze input text, extract plot points, generate storylines, and write complete stories.
6. **Data Merging and Output:** Tools merge multiple JSONL files and output processed data in various formats.

**Key Features:**

* **Modularity:** The program is built with reusable components, allowing for easy customization and extension.
* **Concurrency:** It utilizes multi-threading to process data efficiently.
* **Language Model Integration:** It leverages language models for translation, rewriting, and prompt generation.
* **Customizability:** Users can configure various parameters, such as the number of stories to generate, prompt templates, and data formats.

Overall, this program provides a powerful and flexible toolkit for researchers and developers working with text data and language models.

*gemma-2-27b-it-Q4_K_M.gguf, requests=13, requestDuration=PT2M5.366501869S, inTokens=16996, outTokens=1229, tps=9.832*
