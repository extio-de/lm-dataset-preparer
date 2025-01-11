# lm-dataset-preparer

This program is a comprehensive toolkit for generating and manipulating text data, designed for use with large language models. 

**Purpose:**

The program aims to facilitate the creation of diverse text-based datasets for training and evaluating language models. It achieves this through a modular system of tools that perform various tasks:

* **Text Processing:** The program offers tools for translating, rewriting, splitting, and converting text into different formats (JSON, JSONL).
* **Dataset Creation:** It enables the generation of various types of datasets, including:
    * Story datasets: Using a chain of agents, the program can analyze input text, extract plot points, generate new storylines, and write complete stories.
    * Instruction completion datasets: It can create question-answer pairs, instruction-text pairs, and conversation data based on input text.
    * Contextual prompt datasets: It generates prompts based on text, encouraging users to write stories or complete instructions.
* **Dataset Merging:** The program can merge multiple JSONL files into a single output file, allowing for the aggregation of datasets from different sources.

**Workflow:**

The program utilizes a Spring Boot application as its entry point. This application manages the execution of various DatasetTool instances, each responsible for a specific task. These tools are configurable through properties and can interact with language models for tasks like translation and text generation.

**Key Features:**

* **Modularity:** The program is built with a modular design, allowing for easy extension and customization.
* **Concurrency:** It leverages multi-threading to process tasks concurrently, improving efficiency.
* **Configuration:** The program is highly configurable, allowing users to tailor its behavior to their specific needs.
* **Language Model Integration:** It seamlessly integrates with language models, enabling advanced text processing capabilities.

Overall, this program provides a powerful and versatile toolkit for researchers and developers working with text data and language models.

*gemma-2-27b-it-Q4_K_M.gguf, requests=15, requestDuration=PT2M12.702353502S, inTokens=19033, outTokens=1280, tps=9.7*
