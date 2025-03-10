# Promptly - JetBrains IDE Plugin

> **Note:** This project is currently under production.

Promptly integrates LLMs directly into your JetBrains IDE, allowing seamless interaction with models like GPT, Claude, and Gemini while coding.

## Key Features

- Connect to any LLM service with your API keys
- Chat with AI assistants within your IDE
- Generate and insert code directly into your project
- Share context from your current file when needed
- Secure local storage of API credentials

## Quick Start

| Step | Action |
|------|--------|
| 1. Install | `Settings/Preferences` → `Plugins` → `Marketplace` → Search "Promptly" → `Install` |
| 2. Configure | `Settings/Preferences` → `Tools` → `Promptly` → Enter API key |
| 3. Use | Open Promptly tab in right sidebar or use keyboard shortcuts |

## Usage Options

**Chat Interface:**
- Open Promptly tab in sidebar
- Type query and press Send (or Ctrl+Enter)
- View response and insert code with "Apply to Editor"

**Code Generation:**
- Select code → Right-click → "Generate Code with AI" (or Shift+Ctrl+G)
- Enter prompt → Generated code replaces selection

## Development

Build from source:
```bash
git clone 
./gradlew build
./gradlew runIde
```

## License

MIT License - see LICENSE file for details.
