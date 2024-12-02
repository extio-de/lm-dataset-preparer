# lm-dataset-preparer

This program is a comprehensive text processing pipeline designed for preparing and enhancing textual data. 

**Here's a breakdown of its functionality:**

1. **Translation:** The program can translate text files from various languages to English using a language model client.

2. **Text Processing and Manipulation:** The `Utils` class provides tools for normalizing text, splitting it into paragraphs, and managing files and filenames.

3. **Text Rewriting:** This component leverages a language model to improve and enhance the quality of text. It splits text into paragraphs, sends them to the model with specific prompts, and combines the rewritten paragraphs into a new file.

4. **JSON Conversion:** The program can convert text files into JSON Lines (JSONL) format, with each paragraph represented as a JSON object.

5. **Contextual Prompt Generation:** This module generates contextual prompts based on text paragraphs, extracts names from the text, and creates variations of those names. The generated prompts and paragraphs are stored in JSONL files.

6. **File Merging:** The program can merge multiple JSONL files into a single output file, facilitating data aggregation.

**Purpose:**

The overall purpose of this program is to provide a versatile toolkit for preparing text data for various downstream tasks, such as machine learning model training, text analysis, and content creation. The program's capabilities in translation, rewriting, JSON conversion, prompt generation, and file merging make it suitable for a wide range of text processing applications.

*[gemma-2-27b-it-Q4_K_L.gguf; requests=7, requestDuration=PT2M40.547239104S, inTokens=8437, outTokens=605, tps=3.78125]*
