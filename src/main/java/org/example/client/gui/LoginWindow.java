package org.example.client.gui;

import org.example.client.NetworkManager;
import org.example.client.i18n.LocaleManager;
import org.example.common.model.User;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Optional;

public class LoginWindow extends JFrame {
    private final NetworkManager networkManager;
    private JComboBox<String> languageComboBox;
    private JTabbedPane tabbedPane;
    private JLabel loginUserLabel, loginPassLabel, regUserLabel, regPassLabel, languageLabel;
    private JButton loginButton, registerButton;

    public LoginWindow(NetworkManager networkManager) {
        this.networkManager = networkManager;
        initUI();
        updateTexts();
    }

    private void initUI() {
        setSize(400, 250);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel languagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        languageLabel = new JLabel();
        languagePanel.add(languageLabel);
        String[] languages = {"Русский", "Dansk", "Македонски", "Español (Ecuador)"};
        languageComboBox = new JComboBox<>(languages);
        languagePanel.add(languageComboBox);

        tabbedPane = new JTabbedPane();
        JPanel loginPanel = createLoginPanel();
        JPanel registerPanel = createRegisterPanel();
        tabbedPane.add(loginPanel);
        tabbedPane.add(registerPanel);

        add(languagePanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);

        languageComboBox.addActionListener(e -> switchLanguage());
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        loginUserLabel = new JLabel();
        loginPassLabel = new JLabel();
        loginButton = new JButton();
        panel.add(loginUserLabel);
        panel.add(userField);
        panel.add(loginPassLabel);
        panel.add(passField);
        panel.add(new JLabel());
        panel.add(loginButton);
        loginButton.addActionListener(e -> onLogin(userField.getText(), new String(passField.getPassword())));
        return panel;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        regUserLabel = new JLabel();
        regPassLabel = new JLabel();
        registerButton = new JButton();
        panel.add(regUserLabel);
        panel.add(userField);
        panel.add(regPassLabel);
        panel.add(passField);
        panel.add(new JLabel());
        panel.add(registerButton);
        registerButton.addActionListener(e -> onRegister(userField.getText(), new String(passField.getPassword())));
        return panel;
    }

    private void onLogin(String username, String password) {
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            showError("login.error.emptyFields");
            return;
        }
        CommandRequest request = new CommandRequest("login", null, username, password);
        Optional<CommandResponse> responseOpt = networkManager.sendRequest(request);
        responseOpt.ifPresentOrElse(response -> {
            if (response.isSuccess() && response.getResultData() instanceof User currentUser) {
                this.dispose();
                new MainWindow(networkManager, currentUser, password).setVisible(true);
            } else {
                showError(response.getMessage());
            }
        }, () -> showError("login.error.network"));
    }

    private void onRegister(String username, String password) {
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            showError("login.error.emptyFields");
            return;
        }
        CommandRequest request = new CommandRequest("register", new String[]{username, password});
        Optional<CommandResponse> responseOpt = networkManager.sendRequest(request);
        responseOpt.ifPresentOrElse(response -> {
            if (response.isSuccess()) {
                JOptionPane.showMessageDialog(this,
                        LocaleManager.getString("login.register.success"),
                        LocaleManager.getString("login.success.title"),
                        JOptionPane.INFORMATION_MESSAGE);
                tabbedPane.setSelectedIndex(0);
            } else {
                showError(response.getMessage());
            }
        }, () -> showError("login.error.network"));
    }

    private void switchLanguage() {
        Locale newLocale = switch (languageComboBox.getSelectedIndex()) {
            case 0 -> LocaleManager.RU;
            case 1 -> LocaleManager.DA;
            case 2 -> LocaleManager.MK;
            case 3 -> LocaleManager.ES_EC;
            default -> LocaleManager.getCurrentLocale();
        };
        LocaleManager.setLocale(newLocale);
        updateTexts();
    }

    private void updateTexts() {
        setTitle(LocaleManager.getString("login.title"));
        languageLabel.setText(LocaleManager.getString("login.language") + ":");
        tabbedPane.setTitleAt(0, LocaleManager.getString("login.tab.login"));
        tabbedPane.setTitleAt(1, LocaleManager.getString("login.tab.register"));
        loginUserLabel.setText(LocaleManager.getString("login.label.username"));
        loginPassLabel.setText(LocaleManager.getString("login.label.password"));
        loginButton.setText(LocaleManager.getString("login.button.login"));
        regUserLabel.setText(LocaleManager.getString("login.label.username"));
        regPassLabel.setText(LocaleManager.getString("login.label.password"));
        registerButton.setText(LocaleManager.getString("login.button.register"));
    }

    private void showError(String messageKeyOrText) {
        String message = messageKeyOrText;
        try { message = LocaleManager.getString(messageKeyOrText); } catch (Exception e) { /* use as is */ }
        JOptionPane.showMessageDialog(this, message, LocaleManager.getString("login.error.title"), JOptionPane.ERROR_MESSAGE);
    }
}