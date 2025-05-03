package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Класс для управления командами пользователя.
 * Обрабатывает ввод пользователя и выполняет соответствующие команды.
 */
public class CommandManager {
    private final Scanner scanner;
    private final List<String> commandHistory;
    private final CommandFactory commandFactory;

    /**
     * Конструктор класса CommandManager.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public CommandManager(WorkerCollection collection, Scanner scanner) {
        this.scanner = scanner;
        this.commandHistory = new ArrayList<>();
        this.commandFactory = new CommandFactory(collection, scanner);
        
        // Устанавливаем историю команд для команды history
        HistoryCommand historyCommand = (HistoryCommand) commandFactory.getCommandByName("history");
        if (historyCommand != null) {
            historyCommand.setCommandHistory(commandHistory);
        }
    }

    /**
     * Запускает интерактивный режим работы с коллекцией.
     */
    public void interactiveMode() {
        System.out.println("Введите 'help' для получения списка доступных команд.");

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            try {
                if (executeCommand(input, false)) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Ошибка при выполнении команды: " + e.getMessage());
            }
        }
    }

    /**
     * Выполняет команду, введенную пользователем.
     *
     * @param input      строка с командой
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @return true, если программа должна завершиться, false - в противном случае
     * @throws IOException если произошла ошибка ввода-вывода
     */
    public boolean executeCommand(String input, boolean fromScript) throws IOException {
        String[] parts = input.split("\\s+", 2);
        String commandName = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        if (!fromScript) {
            addToHistory(commandName);
        }

        Command command = commandFactory.getCommandByName(commandName);
        if (command != null) {
            return command.execute(args, fromScript);
        } else {
            System.out.println("Неизвестная команда. Введите 'help' для получения списка доступных команд.");
            return false;
        }
    }

    /**
     * Добавляет команду в историю команд.
     *
     * @param command команда для добавления в историю
     */
    private void addToHistory(String command) {
        commandHistory.add(command);
        int historySize = 15;
        if (commandHistory.size() > historySize) {
            commandHistory.remove(0);
        }
    }
}