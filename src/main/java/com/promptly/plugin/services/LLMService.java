package com.promptly.plugin.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.promptly.plugin.models.ChatMessage;
import com.promptly.plugin.models.LLMProvider;
import com.promptly.plugin.models.PromptlySettings;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Service for interacting with LLM APIs.
 */
@Service(Service.Level.APP)
public final class LLMService {
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    public static LLMService getInstance() {
        return ApplicationManager.getApplication().getService(LLMService.class);
    }

    public LLMService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Send a prompt to the selected LLM provider and get a response asynchronously.
     *
     * @param messages List of chat messages
     * @return CompletableFuture with the response text
     */
    public CompletableFuture<String> sendPrompt(List<ChatMessage> messages) {
        CompletableFuture<String> future = new CompletableFuture<>();
        PromptlySettings settings = SettingsService.getInstance().getSettings();
        LLMProvider provider = settings.getSelectedProvider();

        try {
            String requestBody = formatRequestBody(messages, settings, provider);
            Request request = new Request.Builder()
                    .url(settings.getEndpoint(provider))
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json")))
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + settings.getApiKey(provider))
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful() || responseBody == null) {
                            future.completeExceptionally(
                                    new IOException("Unexpected response " + response)
                            );
                            return;
                        }

                        String responseText = responseBody.string();
                        String result = parseResponse(responseText, provider);
                        future.complete(result);
                    }
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Format the request body according to the provider's API expectations.
     */
    private String formatRequestBody(List<ChatMessage> messages, PromptlySettings settings, LLMProvider provider) throws Exception {
        ObjectNode rootNode = objectMapper.createObjectNode();

        switch (provider) {
            case OPENAI:
                // Format for OpenAI API
                rootNode.put("model", settings.getModelName(provider));
                rootNode.put("max_tokens", settings.getMaxTokens());
                rootNode.put("temperature", settings.getTemperature());
                
                ArrayNode messagesNode = rootNode.putArray("messages");
                for (ChatMessage message : messages) {
                    ObjectNode messageNode = messagesNode.addObject();
                    messageNode.put("role", message.getRole().name().toLowerCase());
                    messageNode.put("content", message.getContent());
                }
                break;
                
            case ANTHROPIC:
                // Format for Anthropic API
                rootNode.put("model", settings.getModelName(provider));
                rootNode.put("max_tokens", settings.getMaxTokens());
                rootNode.put("temperature", settings.getTemperature());
                
                ArrayNode messagesArray = rootNode.putArray("messages");
                for (ChatMessage message : messages) {
                    ObjectNode messageObj = messagesArray.addObject();
                    messageObj.put("role", message.getRole() == ChatMessage.Role.USER ? "user" : "assistant");
                    messageObj.put("content", message.getContent());
                }
                break;
                
            case GOOGLE:
                // Format for Google Gemini API
                rootNode.put("model", settings.getModelName(provider));
                
                ObjectNode generationConfig = rootNode.putObject("generationConfig");
                generationConfig.put("maxOutputTokens", settings.getMaxTokens());
                generationConfig.put("temperature", settings.getTemperature());
                
                ArrayNode contentsArray = rootNode.putArray("contents");
                for (ChatMessage message : messages) {
                    ObjectNode contentObj = contentsArray.addObject();
                    contentObj.put("role", message.getRole() == ChatMessage.Role.USER ? "user" : "model");
                    
                    ArrayNode partsArray = contentObj.putArray("parts");
                    ObjectNode partObj = partsArray.addObject();
                    partObj.put("text", message.getContent());
                }
                break;
                
            case CUSTOM:
                // Generic format - may need customization based on the specific API
                rootNode.put("model", settings.getModelName(provider));
                rootNode.put("max_tokens", settings.getMaxTokens());
                rootNode.put("temperature", settings.getTemperature());
                
                ArrayNode messagesArray2 = rootNode.putArray("messages");
                for (ChatMessage message : messages) {
                    ObjectNode messageObj = messagesArray2.addObject();
                    messageObj.put("role", message.getRole().name().toLowerCase());
                    messageObj.put("content", message.getContent());
                }
                break;
                
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }

        return objectMapper.writeValueAsString(rootNode);
    }

    /**
     * Parse the API response based on the provider.
     */
    private String parseResponse(String responseJson, LLMProvider provider) throws IOException {
        JsonNode rootNode = objectMapper.readTree(responseJson);

        switch (provider) {
            case OPENAI:
                return rootNode.path("choices").path(0).path("message").path("content").asText();
                
            case ANTHROPIC:
                return rootNode.path("content").path(0).path("text").asText();
                
            case GOOGLE:
                return rootNode.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText();
                
            case CUSTOM:
                // Default parsing - adjust based on the actual API response structure
                if (rootNode.has("response")) {
                    return rootNode.path("response").asText();
                } else if (rootNode.has("output")) {
                    return rootNode.path("output").asText();
                } else if (rootNode.has("content")) {
                    return rootNode.path("content").asText();
                } else if (rootNode.has("message")) {
                    return rootNode.path("message").asText();
                } else if (rootNode.has("text")) {
                    return rootNode.path("text").asText();
                } else {
                    return responseJson; // Return the entire response if structure is unknown
                }
                
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }
} 