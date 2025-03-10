package com.promptly.plugin.models;

/**
 * Enum representing supported LLM providers.
 */
public enum LLMProvider {
    OPENAI("OpenAI", "https://api.openai.com/v1/chat/completions"),
    ANTHROPIC("Anthropic Claude", "https://api.anthropic.com/v1/messages"),
    GOOGLE("Google Gemini", "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent"),
    CUSTOM("Custom", "");

    private final String displayName;
    private final String defaultEndpoint;

    LLMProvider(String displayName, String defaultEndpoint) {
        this.displayName = displayName;
        this.defaultEndpoint = defaultEndpoint;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultEndpoint() {
        return defaultEndpoint;
    }

    @Override
    public String toString() {
        return displayName;
    }
} 