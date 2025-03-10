package com.promptly.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UI;
import com.promptly.plugin.models.LLMProvider;
import com.promptly.plugin.models.PromptlySettings;
import com.promptly.plugin.services.SettingsService;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides controller functionality for application settings.
 */
public class PromptlySettingsConfigurable implements Configurable {
    private JPanel mainPanel;
    private JComboBox<LLMProvider> providerComboBox;
    private Map<LLMProvider, JBPasswordField> apiKeyFields = new HashMap<>();
    private Map<LLMProvider, JBTextField> endpointFields = new HashMap<>();
    private Map<LLMProvider, JBTextField> modelNameFields = new HashMap<>();
    private JBCheckBox sendContextCheckBox;
    private JSpinner maxTokensSpinner;
    private JSlider temperatureSlider;
    private JLabel temperatureValueLabel;
    private boolean modified = false;

    @Override
    public String getDisplayName() {
        return "Promptly";
    }

    @Override
    public @Nullable JComponent createComponent() {
        initializeUI();
        return mainPanel;
    }

    private void initializeUI() {
        providerComboBox = new ComboBox<>(LLMProvider.values());
        providerComboBox.addActionListener(e -> {
            LLMProvider selectedProvider = (LLMProvider) providerComboBox.getSelectedItem();
            updateVisibleFields(selectedProvider);
            setModified(true);
        });

        // Create fields for each provider
        for (LLMProvider provider : LLMProvider.values()) {
            apiKeyFields.put(provider, new JBPasswordField());
            apiKeyFields.get(provider).getEmptyText().setText("Enter your API key...");
            apiKeyFields.get(provider).getDocument().addDocumentListener(createModificationListener());

            endpointFields.put(provider, new JBTextField());
            endpointFields.get(provider).getDocument().addDocumentListener(createModificationListener());

            modelNameFields.put(provider, new JBTextField());
            modelNameFields.get(provider).getDocument().addDocumentListener(createModificationListener());
        }

        sendContextCheckBox = new JBCheckBox("Send project context with requests");
        sendContextCheckBox.addChangeListener(e -> setModified(true));

        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(2048, 100, 16000, 100));
        maxTokensSpinner.addChangeListener(e -> setModified(true));

        temperatureSlider = new JSlider(0, 100, 70);
        temperatureSlider.setMajorTickSpacing(25);
        temperatureSlider.setMinorTickSpacing(5);
        temperatureSlider.setPaintTicks(true);
        temperatureSlider.setPaintLabels(true);
        temperatureValueLabel = new JLabel("0.7");
        temperatureSlider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                double value = temperatureSlider.getValue() / 100.0;
                temperatureValueLabel.setText(String.format("%.1f", value));
                setModified(true);
            }
        });

        // Build the main panel
        JPanel providerPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent("LLM Provider:", providerComboBox)
                .getPanel();

        JPanel apiKeyPanel = new JPanel(new CardLayout());
        JPanel endpointPanel = new JPanel(new CardLayout());
        JPanel modelNamePanel = new JPanel(new CardLayout());

        for (LLMProvider provider : LLMProvider.values()) {
            apiKeyPanel.add(createProviderSettingsPanel(provider), provider.name());
        }

        JPanel settingsPanel = FormBuilder.createFormBuilder()
                .addComponent(providerPanel)
                .addComponent(apiKeyPanel)
                .addLabeledComponent("Max Tokens:", maxTokensSpinner)
                .addLabeledComponent("Temperature:", UI.PanelFactory.panel(temperatureSlider)
                        .withComment("Controls creativity (0.0 = deterministic, 1.0 = creative)")
                        .resizeX(true)
                        .createPanel())
                .addLabeledComponent("", temperatureValueLabel)
                .addComponent(sendContextCheckBox)
                .getPanel();

        mainPanel = FormBuilder.createFormBuilder()
                .addComponent(settingsPanel)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private JPanel createProviderSettingsPanel(LLMProvider provider) {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("API Key:", apiKeyFields.get(provider))
                .addLabeledComponent("Endpoint:", endpointFields.get(provider))
                .addLabeledComponent("Model Name:", modelNameFields.get(provider))
                .getPanel();
    }

    private void updateVisibleFields(LLMProvider provider) {
        CardLayout apiKeyCardLayout = (CardLayout) ((JPanel) apiKeyFields.get(provider).getParent().getParent()).getLayout();
        apiKeyCardLayout.show((JPanel) apiKeyFields.get(provider).getParent().getParent(), provider.name());
    }

    private javax.swing.event.DocumentListener createModificationListener() {
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                setModified(true);
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                setModified(true);
            }
        };
    }

    @Override
    public boolean isModified() {
        return modified;
    }

    private void setModified(boolean modified) {
        this.modified = modified;
    }

    @Override
    public void apply() throws ConfigurationException {
        PromptlySettings settings = SettingsService.getInstance().getSettings();
        
        settings.setSelectedProvider((LLMProvider) providerComboBox.getSelectedItem());

        // Save API keys, endpoints, and model names for each provider
        for (LLMProvider provider : LLMProvider.values()) {
            settings.setApiKey(provider, new String(apiKeyFields.get(provider).getPassword()));
            settings.setEndpoint(provider, endpointFields.get(provider).getText());
            settings.setModelName(provider, modelNameFields.get(provider).getText());
        }

        settings.setSendProjectContext(sendContextCheckBox.isSelected());
        settings.setMaxTokens((Integer) maxTokensSpinner.getValue());
        settings.setTemperature(temperatureSlider.getValue() / 100.0);

        SettingsService.getInstance().saveSettings(settings);
        setModified(false);
    }

    @Override
    public void reset() {
        PromptlySettings settings = SettingsService.getInstance().getSettings();
        
        providerComboBox.setSelectedItem(settings.getSelectedProvider());

        // Load API keys, endpoints, and model names for each provider
        for (LLMProvider provider : LLMProvider.values()) {
            apiKeyFields.get(provider).setText(settings.getApiKey(provider));
            endpointFields.get(provider).setText(settings.getEndpoint(provider));
            modelNameFields.get(provider).setText(settings.getModelName(provider));
        }

        sendContextCheckBox.setSelected(settings.isSendProjectContext());
        maxTokensSpinner.setValue(settings.getMaxTokens());
        temperatureSlider.setValue((int) (settings.getTemperature() * 100));

        updateVisibleFields(settings.getSelectedProvider());
        setModified(false);
    }

    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
} 