package com.promptly.plugin.services;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.promptly.plugin.models.LLMProvider;
import com.promptly.plugin.models.PromptlySettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for persisting and retrieving Promptly settings.
 */
@Service(Service.Level.APP)
@State(
        name = "com.promptly.plugin.services.SettingsService",
        storages = {@Storage("promptly-settings.xml")}
)
public final class SettingsService implements PersistentStateComponent<SettingsService.State> {
    private State myState = new State();

    public static SettingsService getInstance() {
        return ApplicationManager.getApplication().getService(SettingsService.class);
    }

    /**
     * Internal state class for XML serialization
     */
    public static class State {
        public String selectedProvider = LLMProvider.OPENAI.name();
        public Map<String, String> apiKeys = new HashMap<>();
        public Map<String, String> endpoints = new HashMap<>();
        public Map<String, String> modelNames = new HashMap<>();
        public boolean sendProjectContext = true;
        public int maxTokens = 2048;
        public double temperature = 0.7;
    }

    @Override
    public @Nullable State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }

    /**
     * Converts the internal state to a PromptlySettings object
     */
    public PromptlySettings getSettings() {
        PromptlySettings settings = new PromptlySettings();

        try {
            settings.setSelectedProvider(LLMProvider.valueOf(myState.selectedProvider));
        } catch (IllegalArgumentException e) {
            settings.setSelectedProvider(LLMProvider.OPENAI); // Default if invalid
        }

        // Load API keys
        for (LLMProvider provider : LLMProvider.values()) {
            String key = myState.apiKeys.get(provider.name());
            if (key != null) {
                settings.setApiKey(provider, key);
            }
        }

        // Load endpoints
        for (LLMProvider provider : LLMProvider.values()) {
            String endpoint = myState.endpoints.get(provider.name());
            if (endpoint != null) {
                settings.setEndpoint(provider, endpoint);
            }
        }

        // Load model names
        for (LLMProvider provider : LLMProvider.values()) {
            String modelName = myState.modelNames.get(provider.name());
            if (modelName != null) {
                settings.setModelName(provider, modelName);
            }
        }

        settings.setSendProjectContext(myState.sendProjectContext);
        settings.setMaxTokens(myState.maxTokens);
        settings.setTemperature(myState.temperature);

        return settings;
    }

    /**
     * Saves the settings to the persistent state
     */
    public void saveSettings(PromptlySettings settings) {
        myState.selectedProvider = settings.getSelectedProvider().name();
        
        // Save API keys
        for (LLMProvider provider : LLMProvider.values()) {
            myState.apiKeys.put(provider.name(), settings.getApiKey(provider));
        }

        // Save endpoints
        for (LLMProvider provider : LLMProvider.values()) {
            myState.endpoints.put(provider.name(), settings.getEndpoint(provider));
        }

        // Save model names
        for (LLMProvider provider : LLMProvider.values()) {
            myState.modelNames.put(provider.name(), settings.getModelName(provider));
        }

        myState.sendProjectContext = settings.isSendProjectContext();
        myState.maxTokens = settings.getMaxTokens();
        myState.temperature = settings.getTemperature();
    }
} 