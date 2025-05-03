package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для вывода справки по доступным командам.
 */
public class HelpCommand extends AbstractCommand {
    private final CommandFactory commandFactory;

    /**
     * Конструктор команды help.
     *
     * @param collection     коллекция работников
     * @param scanner        сканнер для чтения ввода пользователя
     * @param commandFactory фабрика команд для получения списка всех команд
     */
    public HelpCommand(WorkerCollection collection, Scanner scanner, CommandFactory commandFactory) {
        super("help", "вывести справку по доступным командам", collection, scanner);
        this.commandFactory = commandFactory;
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        System.out.println("Доступные команды:");
        for (Command command : commandFactory.getCommands()) {
            System.out.println(command.getName() + " : " + command.getDescription());
        }
        return false;
    }
}