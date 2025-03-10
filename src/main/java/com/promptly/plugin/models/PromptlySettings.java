package com.promptly.plugin.models;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings model for Promptly plugin.
 * Stores API keys, preferred LLM settings, and other user preferences.
 */
public class PromptlySettings {
    private LLMProvider selectedProvider = LLMProvider.OPENAI;
    private Map<LLMProvider, String> apiKeys = new HashMap<>();
    private Map<LLMProvider, String> endpoints = new HashMap<>();
    private Map<LLMProvider, String> modelNames = new HashMap<>();
    private boolean sendProjectContext = true;
    private int maxTokens = 2048;
    private double temperature = 0.7;

    public PromptlySettings() {
        // Initialize with default endpoints
        for (LLMProvider provider : LLMProvider.values()) {
            endpoints.put(provider, provider.getDefaultEndpoint());
        }
        
        // Initialize with default model names
        modelNames.put(LLMProvider.OPENAI, "gpt-4");
        modelNames.put(LLMProvider.ANTHROPIC, "claude-3-sonnet-20240229");
        modelNames.put(LLMProvider.GOOGLE, "gemini-pro");
        modelNames.put(LLMProvider.CUSTOM, "");
    }

    public LLMProvider getSelectedProvider() {
        return selectedProvider;
    }

    public void setSelectedProvider(LLMProvider selectedProvider) {
        this.selectedProvider = selectedProvider;
    }

    public String getApiKey(LLMProvider provider) {
        return apiKeys.getOrDefault(provider, "");
    }

    public void setApiKey(LLMProvider provider, String apiKey) {
        apiKeys.put(provider, apiKey);
    }

    public String getEndpoint(LLMProvider provider) {
        return endpoints.getOrDefault(provider, provider.getDefaultEndpoint());
    }

    public void setEndpoint(LLMProvider provider, String endpoint) {
        endpoints.put(provider, endpoint);
    }

    public String getModelName(LLMProvider provider) {
        return modelNames.getOrDefault(provider, "");
    }

    public void setModelName(LLMProvider provider, String modelName) {
        modelNames.put(provider, modelName);
    }

    public boolean isSendProjectContext() {
        return sendProjectContext;
    }

    public void setSendProjectContext(boolean sendProjectContext) {
        this.sendProjectContext = sendProjectContext;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }
} 