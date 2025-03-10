# Promptly - JetBrains IDE Plugin

Promptly is a JetBrains IDE plugin that integrates Large Language Models (LLMs) directly into your coding environment. It enables developers to interact seamlessly with their preferred LLMs (such as GPT models from OpenAI, Claude from Anthropic, Gemini from Google, or any other accessible LLM) directly within their IDE.

## Features

- **Flexible LLM Integration**: Connect to any LLM service using your API keys
- **Interactive Chat Interface**: Ask questions and get coding assistance within your IDE
- **Code Generation and Editing**: Generate code and insert it directly into your project
- **Context Awareness**: Optionally share your current file or selection with the LLM
- **Local Security**: API keys are stored securely on your local machine

## Installation

1. In your JetBrains IDE (IntelliJ IDEA, PyCharm, etc.), go to `Settings/Preferences` → `Plugins`
2. Click on the `Marketplace` tab
3. Search for "Promptly"
4. Click `Install`
5. Restart your IDE when prompted

## Setup

1. Go to `Settings/Preferences` → `Tools` → `Promptly`
2. Select your preferred LLM provider (OpenAI, Anthropic, Google, or Custom)
3. Enter your API key for the selected provider
4. Optionally adjust the endpoint URL, model name, and other settings
5. Click `Apply` to save your settings

## Usage

### Chat Interface

1. Open the Promptly tool window by clicking on the "Promptly" tab in the right sidebar
2. Type your query in the input field and press "Send" or use Ctrl+Enter
3. View the response from the LLM in the chat window
4. Optionally click "Apply to Editor" to insert code blocks from the response into your current file

### Code Generation

1. Select code in your editor that you want to modify or get help with
2. Right-click and select "Generate Code with AI" or use the shortcut Shift+Ctrl+G
3. Enter your prompt describing what you want to do with the code
4. The generated code will replace your selection, and the full response will be available in the Promptly tool window

## Development

### Building from Source

1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Run `./gradlew build` to build the plugin
4. Run `./gradlew runIde` to test the plugin in a development instance of IntelliJ IDEA

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- JetBrains for the IntelliJ Platform SDK
- All contributors to the project 