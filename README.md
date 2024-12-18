# lm-dataset-preparer

This program suite is designed for processing and transforming text data, primarily focusing on tasks related to translation, rewriting, and question-answer generation. 

**Core Functionality:**

* **Text Translation:** Translates text files from various languages to English using a translation service.
* **Text Rewriting:** Rewrites text using a language model, offering different modes like improvement and enhancement.
* **Text Structuring:** Converts text files into JSON Lines format, splitting text into paragraphs and serializing them as JSON objects.
* **Contextual Prompt Generation:** Generates contextual prompts based on text paragraphs, useful for tasks like summarization or question answering.

**Additional Features:**

* **File Manipulation:** Includes utilities for file processing, such as renaming, directory transformation, and suffix addition.
* **Character Name Processing:**  Allows renaming characters within text and generating prompts based on the modified text using a language model.
* **Question-Answer Generation:** Creates question-answer pairs from text, framing each paragraph as a concise instruction.
* **Dataset Processing:** Provides a framework for tools to process datasets in a standardized manner.

**Purpose:**

The program suite aims to streamline text processing workflows for various NLP tasks. Its modular design allows for flexible customization and integration into larger applications. The focus on JSON Lines output facilitates compatibility with other tools and downstream processing pipelines.

*[gemma-2-27b-it-Q4_K_L.gguf; requests=10, requestDuration=PT1M13.710250739S, inTokens=9963, outTokens=734, tps=10.054794520547945]*
