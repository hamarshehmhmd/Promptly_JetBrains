package com.promptly.plugin.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.promptly.plugin.models.ChatMessage;
import com.promptly.plugin.models.PromptlySettings;
import com.promptly.plugin.services.LLMService;
import com.promptly.plugin.services.SettingsService;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Action for generating code directly from the editor.
 */
public class GenerateCodeAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }

        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        
        if (selectedText == null || selectedText.isEmpty()) {
            Messages.showWarningDialog(
                    "Please select some text to generate code.", 
                    "No Selection");
            return;
        }

        // Show input dialog for the prompt
        String prompt = Messages.showInputDialog(
                "What would you like to do with this code?",
                "Promptly - Generate Code",
                Messages.getQuestionIcon());
        
        if (prompt == null || prompt.trim().isEmpty()) {
            return;
        }

        // Prepare messages
        List<ChatMessage> messages = new ArrayList<>();
        
        // Add context
        String filePath = e.getData(CommonDataKeys.VIRTUAL_FILE).getPath();
        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("CONTEXT:\n");
        contextBuilder.append("File: ").append(filePath).append("\n");
        contextBuilder.append("Selected code:\n```\n").append(selectedText).append("\n```\n");
        
        messages.add(new ChatMessage(ChatMessage.Role.SYSTEM, contextBuilder.toString()));
        
        // Add user prompt
        messages.add(new ChatMessage(ChatMessage.Role.USER, prompt));

        // Show a progress dialog
        Messages.showInfoMessage("Generating code... Please wait.", "Promptly");

        // Call LLM service
        CompletableFuture<String> responseFuture = LLMService.getInstance().sendPrompt(messages);
        responseFuture.thenAccept(response -> {
            // Extract code blocks from the response
            String codeToInsert = extractCodeFromMarkdown(response);
            
            // Replace selected text
            ApplicationManager.getApplication().invokeLater(() -> {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    Document document = editor.getDocument();
                    int start = selectionModel.getSelectionStart();
                    int end = selectionModel.getSelectionEnd();
                    document.replaceString(start, end, codeToInsert);
                });
                
                // Show the generated code in a dialog
                Messages.showInfoMessage(
                        "Code generated and inserted.\n\nFull response available in the Promptly tool window.",
                        "Promptly - Code Generated");
                
                // Open the Promptly tool window
                ApplicationManager.getApplication().invokeLater(() -> {
                    ToolWindow toolWindow = ToolWindowManager.getInstance(e.getProject())
                            .getToolWindow("Promptly");
                    if (toolWindow != null) {
                        toolWindow.show();
                    }
                });
            });
        }).exceptionally(ex -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(
                        "Error generating code: " + ex.getMessage(),
                        "Promptly Error"
                );
            });
            return null;
        });
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
    
    @Override
    public void update(@NotNull AnActionEvent e) {
        // Only enable this action if text is selected
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        e.getPresentation().setEnabledAndVisible(
                editor != null && 
                editor.getSelectionModel().hasSelection());
    }
} 