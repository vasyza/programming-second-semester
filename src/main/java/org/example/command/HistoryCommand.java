package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Команда для вывода последних 15 команд.
 */
public class HistoryCommand extends AbstractCommand {
    private List<String> commandHistory;
    private final int historySize = 15;

    /**
     * Конструктор команды history.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public HistoryCommand(WorkerCollection collection, Scanner scanner) {
        super("history", "вывести последние 15 команд", collection, scanner);
    }
    
    /**
     * Устанавливает историю команд.
     *
     * @param commandHistory список команд в истории
     */
    public void setCommandHistory(List<String> commandHistory) {
        this.commandHistory = commandHistory;
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        if (commandHistory == null || commandHistory.isEmpty()) {
            System.out.println("История команд пуста.");
            return false;
        }

        System.out.println("Последние " + Math.min(historySize, commandHistory.size()) + " команд:");
        for (int i = 0; i < commandHistory.size(); i++) {
            System.out.println((i + 1) + ". " + commandHistory.get(i));
        }
        return false;
    }
}