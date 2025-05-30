package org.example.client;

import org.example.common.model.Worker;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Client {
    private final Scanner consoleScanner;
    private final UserInputHandler globalInputHandler;
    private final NetworkManager networkManager;
    private final Set<String> executingScripts = new HashSet<>();
    private final List<String> commandHistory = new ArrayList<>();
    private static final int HISTORY_SIZE = 15;

    private String currentUsername = null;
    private String currentPassword = null;
    private boolean isAuthenticated = false;

    public Client() {
        this.consoleScanner = new Scanner(System.in);
        this.globalInputHandler = new UserInputHandler(consoleScanner);
        this.networkManager = new NetworkManager();
    }

    public void run() {
        System.out.println("Клиент запущен. Введите 'help' для списка команд.");

        while (true) {
            System.out.print(isAuthenticated ? currentUsername + "@client> " : "client> ");
            String line;
            try {
                line = consoleScanner.nextLine().trim();
            } catch (NoSuchElementException e) {
                System.out.println("\nВвод завершен (EOF). Выход.");
                break;
            }

            if (line.isEmpty()) {
                continue;
            }

            String[] parts = line.split("\\s+", 2);
            String commandName = parts[0].toLowerCase();
            String argsString = parts.length > 1 ? parts[1] : null;

            if (!commandName.equals("history")) {
                addToHistory(commandName);
            }

            if (commandName.equalsIgnoreCase("exit")) {
                System.out.println("Завершение работы клиента...");
                break;
            }

            if (commandName.equalsIgnoreCase("history")) {
                printHistory();
                continue;
            }

            if (commandName.equalsIgnoreCase("register") || commandName.equalsIgnoreCase("login")) {
                processAuthCommand(commandName, argsString);
                continue;
            }

            if (!isAuthenticated && !commandName.equalsIgnoreCase("help")) {
                System.out.println(
                        "Вы не авторизованы. Пожалуйста, войдите с помощью 'login <username> <password>' или зарегистрируйтесь 'register <username> <password>'.");
                continue;
            }

            if (commandName.equalsIgnoreCase("execute_script")) {
                if (argsString == null) {
                    System.out.println("Ошибка: Не указано имя файла для execute_script.");
                    continue;
                }
                executeScript(argsString);
            } else {
                processAndSendCommand(line, globalInputHandler, false);
            }
        }

        networkManager.closeConnection();
        consoleScanner.close();
        System.out.println("Клиент остановлен.");
    }

    private void processAuthCommand(String commandName, String argsString) {
        String[] credentials = globalInputHandler.readCredentials(argsString, commandName);
        if (credentials == null) {
            return;
        }
        String tempUsername = credentials[0];
        String tempPassword = credentials[1];

        CommandRequest request;
        if (commandName.equalsIgnoreCase("register")) {
            request = new CommandRequest(commandName, new String[] { tempUsername, tempPassword });
        } else {
            request = new CommandRequest(commandName, null, tempUsername, tempPassword);
        }

        Optional<CommandResponse> responseOpt = networkManager.sendRequest(request);

        if (responseOpt.isPresent()) {
            CommandResponse response = responseOpt.get();
            System.out.println("\n--- Ответ Сервера ---");
            System.out.println(response.getMessage());
            System.out.println("---------------------\n");
            if (response.isSuccess()) {
                if (commandName.equalsIgnoreCase("login")) {
                    currentUsername = tempUsername;
                    currentPassword = tempPassword;
                    isAuthenticated = true;
                } else if (commandName.equalsIgnoreCase("register")) {
                    System.out.println(
                            "Теперь вы можете войти с помощью команды 'login " + tempUsername + " <ваш_пароль>'.");
                }
            }
        } else {
            System.out.println("\n--- Ошибка Сети ---");
            System.out.println("Не удалось получить ответ от сервера.");
            System.out.println("-------------------\n");
        }
    }

    private void addToHistory(String commandName) {
        if (commandHistory.size() >= HISTORY_SIZE) {
            commandHistory.remove(0);
        }
        commandHistory.add(commandName);
    }

    private void printHistory() {
        if (commandHistory.isEmpty()) {
            System.out.println("История команд пуста.");
            return;
        }
        System.out.println("Последние " + commandHistory.size() + " команд:");
        for (int i = 0; i < commandHistory.size(); i++) {
            System.out.println((i + 1) + ". " + commandHistory.get(i));
        }
    }

    private void processAndSendCommand(String line, UserInputHandler inputHandler, boolean fromScript) {
        try {
            String[] parts = line.split("\\s+", 2);
            String commandName = parts[0].toLowerCase();
            String argsString = parts.length > 1 ? parts[1] : null;
            Object argument = null;

            switch (commandName) {
                case "add":
                case "add_if_min":
                case "add_if_max":
                    if (!fromScript)
                        System.out.println("Введите данные для работника:");
                    try {
                        argument = inputHandler.readWorker(fromScript);
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        System.out.println("Ошибка ввода данных для работника" + (fromScript ? " в скрипте" : "") + ": "
                                + e.getMessage());
                        return;
                    }
                    break;
                case "update":
                    if (argsString == null) {
                        System.out.println("Ошибка: Требуется ID работника для команды 'update'.");
                        return;
                    }
                    try {
                        Long updateId = inputHandler.parseLong(argsString.trim(), "ID");
                        if (!fromScript)
                            System.out.println("Введите новые данные для работника с ID " + updateId + ":");
                        Worker updateData = inputHandler.readWorker(fromScript);
                        argument = new Object[] { updateId, updateData };
                    } catch (IllegalArgumentException | IllegalStateException e) {
                        System.out.println("Ошибка ввода данных для 'update'" + (fromScript ? " в скрипте" : "") + ": "
                                + e.getMessage());
                        return;
                    }
                    break;
                case "remove_by_id":
                    if (argsString == null) {
                        System.out.println("Ошибка: Требуется ID для команды 'remove_by_id'.");
                        return;
                    }
                    try {
                        argument = inputHandler.parseLong(argsString.trim(), "ID");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка ввода ID для 'remove_by_id'" + (fromScript ? " в скрипте" : "")
                                + ": " + e.getMessage());
                        return;
                    }
                    break;
                default:
                    break;
            }

            CommandRequest request = new CommandRequest(commandName, argument, currentUsername, currentPassword);
            Optional<CommandResponse> responseOpt = networkManager.sendRequest(request);

            if (responseOpt.isPresent()) {
                CommandResponse response = responseOpt.get();
                System.out.println("\n--- Ответ Сервера ---");
                if (response.getMessage() != null && !response.getMessage().isEmpty()) {
                    System.out.println(response.getMessage());
                }
                if (response.getResultData() != null) {
                    if (response.getResultData() instanceof List<?> listResult) {
                        if (!listResult.isEmpty()) {
                            if (listResult.get(0) instanceof Worker) {
                                System.out.println("Данные (включая ID владельца):");
                                listResult.forEach(item -> System.out.println(item.toString()));
                            } else {
                                listResult.forEach(System.out::println);
                            }
                        }
                    } else {
                        System.out.println("Данные: " + response.getResultData());
                    }
                }
                if (!response.isSuccess() && (response.getMessage() == null || response.getMessage().isEmpty())) {
                    System.out.println("Команда не выполнена успешно (дополнительных сообщений нет).");
                }
                if (!response.isSuccess() && response.getMessage() != null &&
                        (response.getMessage().contains("Ошибка аутентификации")
                                || response.getMessage().contains("доступ запрещен"))) {
                    isAuthenticated = false;
                    currentUsername = null;
                    currentPassword = null;
                    System.out.println("Сессия сброшена из-за ошибки аутентификации.");
                }
                System.out.println("---------------------\n");
            } else {
                System.out.println("\n--- Ошибка Сети ---");
                System.out.println(
                        "Не удалось получить ответ от сервера. Сервер может быть недоступен или произошла ошибка сети.");
                System.out.println("Попробуйте войти снова, если проблема не устранена.");
                isAuthenticated = false;
                currentUsername = null;
                currentPassword = null;
                System.out.println("-------------------\n");
            }

        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка ввода на клиенте: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка на клиенте: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void executeScript(String filePath) {
        File scriptFile = new File(filePath);
        String absolutePath = scriptFile.getAbsolutePath();

        if (executingScripts.contains(absolutePath)) {
            System.out.println("Ошибка: Обнаружена рекурсия в скрипте! Файл уже выполняется: " + absolutePath);
            return;
        }

        if (!scriptFile.exists() || !scriptFile.isFile() || !scriptFile.canRead()) {
            System.out.println(
                    "Ошибка: Не удается прочитать файл скрипта: " + filePath + " (Проверьте путь и права доступа)");
            return;
        }

        executingScripts.add(absolutePath);
        System.out.println("--- Начало выполнения скрипта: " + filePath + " ---");

        try (Scanner scriptScanner = new Scanner(scriptFile)) {
            UserInputHandler scriptInputHandler = new UserInputHandler(scriptScanner);
            while (scriptScanner.hasNextLine()) {
                String scriptLine = scriptScanner.nextLine().trim();
                if (scriptLine.isEmpty() || scriptLine.startsWith("#")) {
                    continue;
                }
                System.out.println("(Скрипт '" + filePath + "')> " + scriptLine);

                String commandNameOnly = scriptLine.split("\\s+")[0].toLowerCase();
                if (!commandNameOnly.equals("history")) {
                    addToHistory(commandNameOnly);
                }

                if (commandNameOnly.equalsIgnoreCase("exit")) {
                    System.out.println("Команда 'exit' в скрипте игнорируется.");
                    continue;
                }
                if (commandNameOnly.equalsIgnoreCase("history")) {
                    printHistory();
                    continue;
                }

                if (commandNameOnly.equalsIgnoreCase("register") || commandNameOnly.equalsIgnoreCase("login")) {
                    System.out.println(
                            "Команды 'register' и 'login' в скриптах не поддерживаются в интерактивном режиме.");
                    continue;
                }

                if (!isAuthenticated && !commandNameOnly.equalsIgnoreCase("help")) {
                    System.out.println("Скрипт не может выполнить команду '" + commandNameOnly
                            + "', так как клиент не авторизован.");
                    System.out.println(
                            "--- Прерывание выполнения скрипта: " + filePath + " из-за отсутствия авторизации ---");
                    return;
                }

                if (commandNameOnly.equalsIgnoreCase("execute_script")) {
                    String[] parts = scriptLine.split("\\s+", 2);
                    if (parts.length > 1) {
                        executeScript(parts[1]);
                    } else {
                        System.out.println("Ошибка в скрипте: Не указано имя файла для вложенного execute_script.");
                    }
                } else {
                    processAndSendCommand(scriptLine, scriptInputHandler, true);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(
                    "Файл скрипта не найден во время выполнения: "
                            + filePath);
        } catch (Exception e) {
            System.out.println("Ошибка во время выполнения скрипта '" + filePath + "': " + e.getMessage());
            e.printStackTrace();
        } finally {
            executingScripts.remove(absolutePath);
            System.out.println("--- Конец выполнения скрипта: " + filePath + " ---");
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }
}