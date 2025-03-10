package com.promptly.plugin.models;

import java.time.LocalDateTime;

/**
 * Represents a single message in the chat interface.
 */
public class ChatMessage {
    public enum Role {
        USER("User"),
        ASSISTANT("Assistant"),
        SYSTEM("System");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private final Role role;
    private final String content;
    private final LocalDateTime timestamp;

    public ChatMessage(Role role, String content) {
        this.role = role;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    public Role getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Converts this message to a format suitable for API requests.
     * Different LLM providers might need different formats.
     * @return A map with role and content, suitable for JSON conversion.
     */
    public java.util.Map<String, String> toApiFormat() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("role", role.name().toLowerCase());
        map.put("content", content);
        return map;
    }
} 