package org.example.client;

import org.example.common.model.Worker;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;


public class Client {
    private final Scanner consoleScanner;
    private final UserInputHandler inputHandler;
    private final NetworkManager networkManager;
    private static final Set<String> executingScripts = new HashSet<>();
    private List<String> commandHistory;
    private static final int HISTORY_SIZE = 15;


    public Client() throws IOException {
        this.consoleScanner = new Scanner(System.in);
        this.inputHandler = new UserInputHandler(consoleScanner);
        this.networkManager = new NetworkManager();
        this.commandHistory = new ArrayList<>();
    }

    public void run() {
        System.out.println("Клиент запущен. Введите 'help' для списка команд.");

        while (true) {
            System.out.print("> ");
            String line;
            try {
                line = consoleScanner.nextLine().trim();
            } catch (NoSuchElementException e) {
                System.out.println("Ввод завершен. Выход.");
                break;
            }


            if (line.isEmpty()) {
                continue;
            }

            if (line.equalsIgnoreCase("exit")) {
                System.out.println("Завершение работы клиента...");
                break;
            }

            if (line.equalsIgnoreCase("history")) {
                if (commandHistory == null) {
                    System.out.println("История команд пуста.");
                    continue;
                }
                System.out.println("Последние " + Math.min(HISTORY_SIZE, commandHistory.size()) + " команд:");
                for (int i = 0; i < commandHistory.size(); i++) {
                    System.out.println((i + 1) + ". " + commandHistory.get(i));
                }
                continue;
            }

            if (line.toLowerCase().startsWith("execute_script")) {
                String[] parts = line.split("\\s+", 2);
                if (parts.length < 2) {
                    System.out.println("Ошибка: Не указано имя файла для execute_script.");
                    continue;
                }
                executeScript(parts[1]);
            } else {
                processAndSendCommand(line);
            }
        }

        try {
            networkManager.close();
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии сетевых ресурсов: " + e.getMessage());
        }
        consoleScanner.close();
        System.out.println("Клиент остановлен.");
    }


    private void processAndSendCommand(String line) {
        try {
            String[] parts = line.split("\\s+", 2);
            String commandName = parts[0].toLowerCase();
            String argsString = parts.length > 1 ? parts[1] : null;
            Object argument;

            this.commandHistory.add(commandName);

            switch (commandName) {
                case "add":
                case "add_if_min":
                case "add_if_max":
                    System.out.println("Введите данные для работника:");
                    argument = inputHandler.readWorker(false);
                    break;
                case "update":
                    if (argsString == null) throw new IllegalArgumentException("Требуется ID работника для команды update.");
                    Long updateId = inputHandler.parseLong(argsString, "ID");
                    System.out.println("Введите новые данные для работника с ID " + updateId + ":");
                    Worker updateData = inputHandler.readWorker(false);
                    argument = new Object[]{updateId, updateData};
                    break;
                case "remove_by_id":
                    if (argsString == null) throw new IllegalArgumentException("Требуется ID для команды remove_by_id.");
                    argument = inputHandler.parseLong(argsString, "ID");
                    break;
                default:
                    argument = argsString;
                    break;
            }

            CommandRequest request = new CommandRequest(commandName, argument);

            networkManager.sendRequest(request);
            Optional<CommandResponse> responseOpt = networkManager.receiveResponse();

            if (responseOpt.isPresent()) {
                CommandResponse response = responseOpt.get();
                System.out.println("\n--- Ответ Сервера ---");
                System.out.println(response);
                System.out.println("---------------------\n");
            } else {
                System.out.println("\n--- Ошибка ---");
                System.out.println("Сервер не ответил в течение таймаута.");
                System.out.println("--------------\n");
            }


        } catch (IllegalArgumentException e) {
            System.out.println("Ошибка ввода: " + e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Сетевая ошибка или ошибка десериализации: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка клиента: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private void executeScript(String filePath) {
        if (executingScripts.contains(filePath)) {
            System.out.println("Ошибка: Обнаружена рекурсия в скрипте! Файл: " + filePath);
            return;
        }

        File scriptFile = new File(filePath);
        if (!scriptFile.exists() || !scriptFile.canRead()) {
            System.out.println("Ошибка: Не удается прочитать файл скрипта: " + filePath);
            return;
        }

        executingScripts.add(filePath);
        System.out.println("--- Начало выполнения скрипта: " + filePath + " ---");
        try (Scanner scriptScanner = new Scanner(scriptFile)) {
            while (scriptScanner.hasNextLine()) {
                String scriptLine = scriptScanner.nextLine().trim();
                if (scriptLine.isEmpty() || scriptLine.startsWith("#")) {
                    continue;
                }
                System.out.println("> " + scriptLine);


                if (scriptLine.equalsIgnoreCase("exit")) {
                    System.out.println("Команда 'exit' в скрипте игнорируется.");
                    continue;
                }

                if (scriptLine.equalsIgnoreCase("history")) {
                    if (commandHistory == null) {
                        System.out.println("История команд пуста.");
                        continue;
                    }
                    System.out.println("Последние " + Math.min(HISTORY_SIZE, commandHistory.size()) + " команд:");
                    for (int i = 0; i < commandHistory.size(); i++) {
                        System.out.println((i + 1) + ". " + commandHistory.get(i));
                    }
                    continue;
                }

                if (scriptLine.toLowerCase().startsWith("execute_script")) {
                    String[] parts = scriptLine.split("\\s+", 2);
                    if (parts.length > 1) {
                        executeScript(parts[1]);
                    } else {
                        System.out.println("Ошибка в скрипте: Не указано имя файла для execute_script.");
                    }
                } else {
                    processAndSendCommand(scriptLine);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Ошибка: Файл скрипта не найден (хотя он существовал): " + filePath);
        } catch (Exception e) {
            System.out.println("Ошибка во время выполнения скрипта: " + e.getMessage());
        } finally {
            executingScripts.remove(filePath);
            System.out.println("--- Конец выполнения скрипта: " + filePath + " ---");
        }
    }


    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.run();
        } catch (IOException e) {
            System.err.println("Не удалось запустить клиент: " + e.getMessage());
        }
    }
}