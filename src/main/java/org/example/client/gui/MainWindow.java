package org.example.client.gui;

import org.example.client.NetworkManager;
import org.example.client.UserInputHandler;
import org.example.client.i18n.LocaleManager;
import org.example.common.model.User;
import org.example.common.model.Worker;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.PatternSyntaxException;

public class MainWindow extends JFrame {
    private final NetworkManager networkManager;
    private final String username;
    private final String password;
    private final int currentUserId;

    private WorkerTableModel tableModel;
    private TableRowSorter<WorkerTableModel> sorter;
    private VisualizationPanel visualizationPanel;
    private Timer refreshTimer;

    private JLabel currentUserLabel, filterLabel, statusLabel;
    private JMenu sessionMenu, commandsMenu, languageMenu;
    private JMenuItem logoutItem, exitItem, addItem, updateItem, deleteItem, clearItem, addIfMinItem, addIfMaxItem, executeScriptItem;
    private JTable workerTable;
    private JTextField filterField;

    public MainWindow(NetworkManager networkManager, User currentUser, String password) {
        this.networkManager = networkManager;
        this.username = currentUser.getUsername();
        this.currentUserId = currentUser.getId();
        this.password = password;
        initUI();
        startAutoRefresh();
    }

    private void initUI() {
        setSize(1200, 800);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        createMenuBar();

        JPanel topPanel = new JPanel(new BorderLayout(10, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        currentUserLabel = new JLabel();
        topPanel.add(currentUserLabel, BorderLayout.WEST);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        filterLabel = new JLabel();
        filterField = new JTextField(20);
        filterPanel.add(filterLabel);
        filterPanel.add(filterField);
        topPanel.add(filterPanel, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        tableModel = new WorkerTableModel();
        workerTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        workerTable.setRowSorter(sorter);
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                try {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + filterField.getText()));
                } catch (PatternSyntaxException ignored) {}
            }
        });

        visualizationPanel = new VisualizationPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(workerTable), visualizationPanel);
        splitPane.setDividerLocation(0.6);
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);

        statusLabel = new JLabel(" ");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));
        add(statusLabel, BorderLayout.SOUTH);

        updateTexts();
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        sessionMenu = new JMenu();
        logoutItem = new JMenuItem();
        exitItem = new JMenuItem();
        logoutItem.addActionListener(e -> logout());
        exitItem.addActionListener(e -> System.exit(0));
        sessionMenu.add(logoutItem);
        sessionMenu.add(exitItem);

        commandsMenu = new JMenu();
        addItem = new JMenuItem();
        updateItem = new JMenuItem();
        deleteItem = new JMenuItem();
        clearItem = new JMenuItem();
        addIfMinItem = new JMenuItem();
        addIfMaxItem = new JMenuItem();
        executeScriptItem = new JMenuItem();

        addItem.addActionListener(e -> addWorker("add"));
        addIfMinItem.addActionListener(e -> addWorker("add_if_min"));
        addIfMaxItem.addActionListener(e -> addWorker("add_if_max"));
        updateItem.addActionListener(e -> updateWorker());
        deleteItem.addActionListener(e -> deleteWorker());
        clearItem.addActionListener(e -> clearWorkers());
        executeScriptItem.addActionListener(e -> executeScript());

        commandsMenu.add(addItem);
        commandsMenu.add(addIfMinItem);
        commandsMenu.add(addIfMaxItem);
        commandsMenu.addSeparator();
        commandsMenu.add(updateItem);
        commandsMenu.add(deleteItem);
        commandsMenu.addSeparator();
        commandsMenu.add(clearItem);
        commandsMenu.addSeparator();
        commandsMenu.add(executeScriptItem);

        languageMenu = new JMenu();
        ButtonGroup langGroup = new ButtonGroup();
        for (Locale locale : new Locale[]{LocaleManager.RU, LocaleManager.DA, LocaleManager.MK, LocaleManager.ES_EC}) {
            JRadioButtonMenuItem langItem = new JRadioButtonMenuItem(locale.getDisplayLanguage(locale));
            langItem.addActionListener(e -> {
                LocaleManager.setLocale(locale);
                updateTexts();
            });
            if (LocaleManager.getCurrentLocale().equals(locale)) langItem.setSelected(true);
            langGroup.add(langItem);
            languageMenu.add(langItem);
        }

        menuBar.add(sessionMenu);
        menuBar.add(commandsMenu);
        menuBar.add(languageMenu);
        setJMenuBar(menuBar);
    }

    private void updateTexts() {
        setTitle(LocaleManager.getString("main.title"));
        currentUserLabel.setText(LocaleManager.getFormattedString("main.currentUserLabel", this.username));
        filterLabel.setText("Фильтр:");
        sessionMenu.setText(LocaleManager.getString("main.menu.session"));
        logoutItem.setText(LocaleManager.getString("main.menu.session.logout"));
        exitItem.setText(LocaleManager.getString("main.menu.session.exit"));
        commandsMenu.setText(LocaleManager.getString("main.menu.commands"));
        addItem.setText(LocaleManager.getString("main.menu.commands.add"));
        updateItem.setText(LocaleManager.getString("main.menu.commands.update"));
        deleteItem.setText(LocaleManager.getString("main.menu.commands.delete"));
        clearItem.setText(LocaleManager.getString("main.menu.commands.clear"));
        addIfMinItem.setText(LocaleManager.getString("main.menu.commands.add_if_min"));
        addIfMaxItem.setText(LocaleManager.getString("main.menu.commands.add_if_max"));
        executeScriptItem.setText(LocaleManager.getString("main.menu.commands.execute_script"));
        languageMenu.setText(LocaleManager.getString("main.menu.language"));
        tableModel.fireTableStructureChanged();
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(5000, e -> refreshData());
        refreshTimer.setInitialDelay(0);
        refreshTimer.start();
    }

    private void refreshData() {
        CommandRequest request = new CommandRequest("show", null, username, password);
        Optional<CommandResponse> responseOpt = networkManager.sendRequest(request);
        responseOpt.ifPresentOrElse(response -> {
            if (response.isSuccess() && response.getResultData() instanceof List) {
                List<Worker> workers = (List<Worker>) response.getResultData();
                SwingUtilities.invokeLater(() -> {
                    int selectedRow = workerTable.getSelectedRow();
                    tableModel.setWorkers(workers);
                    visualizationPanel.setWorkers(workers, this.currentUserId);
                    if (selectedRow != -1 && selectedRow < workerTable.getRowCount()) {
                        workerTable.setRowSelectionInterval(selectedRow, selectedRow);
                    }
                });
            } else if (!response.isSuccess() && response.getMessage().toLowerCase().contains("аутентификации")) {
                logoutAndShowError("Сессия истекла или недействительна. Пожалуйста, войдите снова.");
            }
        }, () -> {});
    }

    private void addWorker(String command) {
        AddWorkerDialog dialog = new AddWorkerDialog(this, LocaleManager.getString("dialog.add.title"), null);
        dialog.setVisible(true);

        if (dialog.isOkPressed()) {
            Worker newWorker = dialog.getWorker();
            CommandRequest request = new CommandRequest(command, newWorker, username, password);
            sendCommand(request, true);
        }
    }

    private void updateWorker() {
        int selectedRowInView = workerTable.getSelectedRow();
        if (selectedRowInView == -1) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("dialog.error.no_selection"), LocaleManager.getString("login.error.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        int modelRow = workerTable.convertRowIndexToModel(selectedRowInView);
        Worker workerToUpdate = tableModel.getWorkerAt(modelRow);

        if (workerToUpdate != null) {
            AddWorkerDialog dialog = new AddWorkerDialog(this, LocaleManager.getFormattedString("dialog.update.title", workerToUpdate.getId()), workerToUpdate);
            dialog.setVisible(true);
            if (dialog.isOkPressed()) {
                Worker updatedWorker = dialog.getWorker();
                Object[] args = {workerToUpdate.getId(), updatedWorker};
                CommandRequest request = new CommandRequest("update", args, username, password);
                sendCommand(request, true);
            }
        }
    }

    private void deleteWorker() {
        int selectedRowInView = workerTable.getSelectedRow();
        if (selectedRowInView == -1) {
            JOptionPane.showMessageDialog(this, LocaleManager.getString("dialog.error.no_selection"), LocaleManager.getString("login.error.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }
        int modelRow = workerTable.convertRowIndexToModel(selectedRowInView);
        Worker workerToDelete = tableModel.getWorkerAt(modelRow);
        if (workerToDelete != null) {
            int confirmation = JOptionPane.showConfirmDialog(this,
                    LocaleManager.getFormattedString("dialog.confirm.delete", workerToDelete.getId()),
                    LocaleManager.getString("dialog.confirm.title"), JOptionPane.YES_NO_OPTION);
            if (confirmation == JOptionPane.YES_OPTION) {
                CommandRequest request = new CommandRequest("remove_by_id", workerToDelete.getId(), username, password);
                sendCommand(request, true);
            }
        }
    }

    private void clearWorkers() {
        int confirmation = JOptionPane.showConfirmDialog(this,
                "Вы уверены, что хотите удалить ВСЕ свои объекты?",
                LocaleManager.getString("dialog.confirm.title"), JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            CommandRequest request = new CommandRequest("clear", null, username, password);
            sendCommand(request, true);
        }
    }

    private void executeScript() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(LocaleManager.getString("script.chooser.title"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File scriptFile = fileChooser.getSelectedFile();
            statusLabel.setText(LocaleManager.getFormattedString("script.status.executing", scriptFile.getName()));

            SwingWorker<Void, String> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    try (Scanner scriptScanner = new Scanner(scriptFile)) {
                        UserInputHandler scriptInputHandler = new UserInputHandler(scriptScanner);
                        while (scriptScanner.hasNextLine()) {
                            String line = scriptScanner.nextLine().trim();
                            if (line.isEmpty() || line.startsWith("#")) continue;

                            String[] parts = line.split("\\s+", 2);
                            String commandName = parts[0].toLowerCase();

                            CommandRequest request = null;
                            switch (commandName) {
                                case "add":
                                case "add_if_min":
                                case "add_if_max":
                                    Worker worker = scriptInputHandler.readWorker(true);
                                    request = new CommandRequest(commandName, worker, username, password);
                                    break;
                                case "clear":
                                    request = new CommandRequest(commandName, null, username, password);
                                    break;
                                default:
                                    System.out.println("Неподдерживаемая в скрипте команда: " + commandName);
                            }
                            if (request != null) {
                                sendCommand(request, false);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException("Файл не найден", e);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        statusLabel.setText(LocaleManager.getString("script.status.finished"));
                    } catch (Exception e) {
                        statusLabel.setText(LocaleManager.getFormattedString("script.status.error", e.getCause().getMessage()));
                    }
                    refreshData();
                }
            };
            worker.execute();
        }
    }

    private void sendCommand(CommandRequest request, boolean showDialog) {
        Optional<CommandResponse> responseOpt = networkManager.sendRequest(request);
        responseOpt.ifPresentOrElse(response -> {
            if (showDialog) JOptionPane.showMessageDialog(this, response.getMessage());
            if (response.isSuccess()) {
                refreshData();
            }
        }, () -> {
            if (showDialog) JOptionPane.showMessageDialog(this, LocaleManager.getString("login.error.network"));
        });
    }

    private void logout() {
        refreshTimer.stop();
        this.dispose();
        new LoginWindow(networkManager).setVisible(true);
    }

    private void logoutAndShowError(String message) {
        refreshTimer.stop();
        this.dispose();
        JOptionPane.showMessageDialog(null, message, "Ошибка сессии", JOptionPane.ERROR_MESSAGE);
        new LoginWindow(networkManager).setVisible(true);
    }
}