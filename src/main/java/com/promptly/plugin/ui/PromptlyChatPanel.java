package com.promptly.plugin.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.promptly.plugin.models.ChatMessage;
import com.promptly.plugin.models.PromptlySettings;
import com.promptly.plugin.services.LLMService;
import com.promptly.plugin.services.SettingsService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Main chat panel for interacting with LLMs.
 */
public class PromptlyChatPanel {
    private final Project project;
    private final ToolWindow toolWindow;
    private final SimpleToolWindowPanel panel;
    private final List<ChatMessage> chatHistory = new ArrayList<>();
    private final JPanel chatMessagesPanel;
    private Editor inputEditor;
    private JButton sendButton;
    private JButton applyToEditorButton;
    private JButton clearButton;
    private String lastResponse = "";
    
    public PromptlyChatPanel(Project project, ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        this.panel = new SimpleToolWindowPanel(true, true);
        
        chatMessagesPanel = new JPanel();
        chatMessagesPanel.setLayout(new BoxLayout(chatMessagesPanel, BoxLayout.Y_AXIS));
        
        JBScrollPane scrollPane = new JBScrollPane(chatMessagesPanel);
        scrollPane.setHorizontalScrollBarPolicy(JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        // Initialize the input area
        JPanel inputPanel = createInputPanel();
        
        // Add action buttons
        JPanel buttonPanel = createButtonPanel();
        
        // Combine everything into the main panel
        JBSplitter splitter = new JBSplitter(true);
        splitter.setFirstComponent(scrollPane);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputPanel, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        splitter.setSecondComponent(bottomPanel);
        splitter.setProportion(0.7f);
        
        panel.setContent(splitter);
        
        // Add a system welcome message
        addMessage(new ChatMessage(ChatMessage.Role.SYSTEM, "Welcome to Promptly! How can I assist you with your code today?"));
    }
    
    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBorder(JBUI.Borders.empty(5));
        
        Document document = EditorFactory.getInstance().createDocument("");
        inputEditor = EditorFactory.getInstance().createEditor(document, project, EditorFactory.INSTANCE.getEditorTypeById("TEXT"), false);
        ((EditorEx) inputEditor).setPlaceholder("Type your prompt here...");
        
        document.addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(DocumentEvent event) {
                updateSendButton();
            }
        });
        
        inputEditor.getContentComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
        
        inputPanel.add(inputEditor.getComponent(), BorderLayout.CENTER);
        return inputPanel;
    }
    
    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        clearButton = new JButton("Clear Chat");
        clearButton.addActionListener(e -> clearChat());
        
        applyToEditorButton = new JButton("Apply to Editor");
        applyToEditorButton.addActionListener(e -> applyToEditor());
        applyToEditorButton.setEnabled(false);
        
        sendButton = new JButton("Send");
        sendButton.addActionListener(e -> sendMessage());
        sendButton.setEnabled(false);
        
        buttonPanel.add(clearButton);
        buttonPanel.add(applyToEditorButton);
        buttonPanel.add(sendButton);
        
        return buttonPanel;
    }
    
    private void updateSendButton() {
        String text = inputEditor.getDocument().getText();
        sendButton.setEnabled(!text.trim().isEmpty());
    }
    
    private void sendMessage() {
        String prompt = inputEditor.getDocument().getText().trim();
        if (prompt.isEmpty()) {
            return;
        }
        
        // Add user message to the chat
        ChatMessage userMessage = new ChatMessage(ChatMessage.Role.USER, prompt);
        addMessage(userMessage);
        chatHistory.add(userMessage);
        
        // Clear the input
        ApplicationManager.getApplication().runWriteAction(() -> 
            inputEditor.getDocument().setText("")
        );
        
        // Prepare context if enabled
        PromptlySettings settings = SettingsService.getInstance().getSettings();
        if (settings.isSendProjectContext()) {
            addContextToMessages();
        }
        
        // Show loading indicator
        JPanel loadingPanel = createLoadingPanel();
        chatMessagesPanel.add(loadingPanel);
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
        
        // Send to LLM service
        CompletableFuture<String> responseFuture = LLMService.getInstance().sendPrompt(chatHistory);
        responseFuture.thenAccept(response -> {
            // Remove loading indicator
            chatMessagesPanel.remove(loadingPanel);
            
            // Add response to chat
            ChatMessage assistantMessage = new ChatMessage(ChatMessage.Role.ASSISTANT, response);
            addMessage(assistantMessage);
            chatHistory.add(assistantMessage);
            
            // Enable "Apply to Editor" button
            lastResponse = response;
            applyToEditorButton.setEnabled(true);
        }).exceptionally(ex -> {
            // Remove loading indicator
            chatMessagesPanel.remove(loadingPanel);
            
            // Show error message
            ChatMessage errorMessage = new ChatMessage(ChatMessage.Role.SYSTEM, 
                    "Error: " + ex.getMessage());
            addMessage(errorMessage);
            return null;
        });
    }
    
    private void addContextToMessages() {
        // Get the current file content
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            String filePath = FileEditorManager.getInstance(project).getSelectedEditor().getFile().getPath();
            String fileContent = selectedTextEditor.getDocument().getText();
            String selectedText = selectedTextEditor.getSelectionModel().getSelectedText();
            
            StringBuilder contextBuilder = new StringBuilder();
            contextBuilder.append("CONTEXT:\n");
            contextBuilder.append("File: ").append(filePath).append("\n");
            
            if (selectedText != null && !selectedText.isEmpty()) {
                contextBuilder.append("Selected code:\n```\n").append(selectedText).append("\n```\n");
            } else {
                contextBuilder.append("File content:\n```\n").append(fileContent).append("\n```\n");
            }
            
            ChatMessage contextMessage = new ChatMessage(ChatMessage.Role.SYSTEM, contextBuilder.toString());
            chatHistory.add(contextMessage);
        }
    }
    
    private void clearChat() {
        chatHistory.clear();
        chatMessagesPanel.removeAll();
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
        
        // Add a system welcome message
        addMessage(new ChatMessage(ChatMessage.Role.SYSTEM, "Chat cleared. How can I assist you with your code today?"));
    }
    
    private void applyToEditor() {
        if (lastResponse.trim().isEmpty()) {
            return;
        }
        
        Editor selectedTextEditor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (selectedTextEditor != null) {
            // Extract code blocks from markdown: look for ```[language] ... ``` patterns
            String codeToInsert = extractCodeFromMarkdown(lastResponse);
            
            // Insert at current cursor position
            int offset = selectedTextEditor.getCaretModel().getOffset();
            ApplicationManager.getApplication().runWriteAction(() -> 
                selectedTextEditor.getDocument().insertString(offset, codeToInsert)
            );
        }
    }
    
    private String extractCodeFromMarkdown(String markdown) {
        // Simple extraction - find code blocks between ``` markers
        StringBuilder result = new StringBuilder();
        
        int codeBlockStart = markdown.indexOf("```");
        while (codeBlockStart != -1) {
            // Find the end of the opening ``` line
            int lineEnd = markdown.indexOf('\n', codeBlockStart);
            if (lineEnd == -1) break;
            
            // Find the closing ```
            int codeBlockEnd = markdown.indexOf("```", lineEnd);
            if (codeBlockEnd == -1) break;
            
            // Extract the code (without the backticks and language identifier)
            String code = markdown.substring(lineEnd + 1, codeBlockEnd).trim();
            result.append(code).append("\n\n");
            
            // Continue search for more code blocks
            codeBlockStart = markdown.indexOf("```", codeBlockEnd + 3);
        }
        
        // If no code blocks found, return the original text
        return result.length() > 0 ? result.toString() : markdown;
    }
    
    private JPanel createLoadingPanel() {
        JPanel loadingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loadingPanel.setBorder(JBUI.Borders.empty(5));
        
        JBLabel loadingLabel = new JBLabel("Thinking...");
        loadingPanel.add(loadingLabel);
        
        Timer timer = new Timer(500, new AbstractAction() {
            private int dots = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                dots = (dots + 1) % 4;
                StringBuilder sb = new StringBuilder("Thinking");
                for (int i = 0; i < dots; i++) {
                    sb.append(".");
                }
                loadingLabel.setText(sb.toString());
            }
        });
        timer.start();
        
        // Store the timer in the panel's client property to stop it when removed
        loadingPanel.putClientProperty("timer", timer);
        
        return loadingPanel;
    }
    
    private void addMessage(ChatMessage message) {
        JPanel messagePanel = new JPanel(new BorderLayout());
        messagePanel.setBorder(JBUI.Borders.empty(10));
        
        // Role label with timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        JBLabel roleLabel = new JBLabel(message.getRole().getDisplayName() + 
                " [" + formatter.format(message.getTimestamp()) + "]");
        roleLabel.setFont(roleLabel.getFont().deriveFont(Font.BOLD));
        
        // Style based on role
        switch (message.getRole()) {
            case USER:
                messagePanel.setBackground(JBUI.CurrentTheme.Editor.selectedText());
                break;
            case ASSISTANT:
                messagePanel.setBackground(JBUI.CurrentTheme.Editor.searchResults());
                break;
            case SYSTEM:
                messagePanel.setBackground(JBUI.CurrentTheme.Editor.searchMatch());
                roleLabel.setFont(roleLabel.getFont().deriveFont(Font.ITALIC));
                break;
        }
        
        messagePanel.add(roleLabel, BorderLayout.NORTH);
        
        // Message content
        JTextArea contentArea = new JTextArea(message.getContent());
        contentArea.setWrapStyleWord(true);
        contentArea.setLineWrap(true);
        contentArea.setEditable(false);
        contentArea.setOpaque(false);
        contentArea.setBorder(JBUI.Borders.empty(5, 0, 0, 0));
        
        // For non-system messages, use a scroll pane to handle long content
        if (message.getRole() != ChatMessage.Role.SYSTEM) {
            JBScrollPane contentScrollPane = new JBScrollPane(contentArea);
            contentScrollPane.setBorder(null);
            contentScrollPane.setOpaque(false);
            contentScrollPane.getViewport().setOpaque(false);
            contentScrollPane.setPreferredSize(new Dimension(0, 200));
            messagePanel.add(contentScrollPane, BorderLayout.CENTER);
        } else {
            messagePanel.add(contentArea, BorderLayout.CENTER);
        }
        
        chatMessagesPanel.add(messagePanel);
        chatMessagesPanel.revalidate();
        chatMessagesPanel.repaint();
        
        // Scroll to bottom
        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) chatMessagesPanel.getParent().getParent();
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }
    
    public JComponent getContent() {
        return panel;
    }
} 