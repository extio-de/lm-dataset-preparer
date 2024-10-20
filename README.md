# lm-dataset-preparer

**Program Name:** Text Processing and Translation Program

**Purpose:** The program is designed to process and translate text files, generating new paragraphs and converting them into various formats. It utilizes a client to make API calls to a model, applies text normalization and transformation techniques, and writes the results to new files.

**Key Features:**

1. **Text Translation:** Translates text files from various languages to English using a client-provided completion API.
2. **Model-based Rewriting:** Generates new paragraphs based on input text and model categories, applying multiple models and enhancements.
3. **Text Normalization:** Removes excessive newlines, spaces, and tabs, and replaces multiple consecutive newlines with a single newline.
4. **Paragraph Splitting:** Splits text into paragraphs based on a specified chunk size and tolerance range.
5. **Directory Transformation:** Applies a function to each file in a directory in parallel using a fixed thread pool.
6. **File Conversion:** Converts text files into JSONL (JSON Lines) format, splitting them into paragraphs and chunks based on provided normalization and variation values.

**Components:**

1. A Spring component that translates text files to English.
2. A Spring component that rewrites text files by generating new paragraphs based on input text and model categories.
3. A Spring component that converts text files into JSONL format.
4. A utility class with methods for text normalization, paragraph splitting, and directory transformation.

Overall, the program is designed to efficiently process and translate text files, applying various techniques to generate new paragraphs and convert them into different formats.

*[requests=5, requestDuration=PT14.64756857S, inTokens=3966, outTokens=801, tps=57.214285714285715]*

