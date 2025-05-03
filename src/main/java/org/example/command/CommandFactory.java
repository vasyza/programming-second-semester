package org.example.command;

import org.example.collection.WorkerCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Фабрика для создания команд.
 * Отвечает за инициализацию всех доступных команд в приложении.
 */
public class CommandFactory {
    private final WorkerCollection collection;
    private final Scanner scanner;
    private final List<Command> commands;

    /**
     * Конструктор фабрики команд.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public CommandFactory(WorkerCollection collection, Scanner scanner) {
        this.collection = collection;
        this.scanner = scanner;
        this.commands = new ArrayList<>();
        initCommands();
    }

    /**
     * Инициализирует все доступные команды.
     */
    private void initCommands() {
        commands.add(new HelpCommand(collection, scanner, this));
        commands.add(new InfoCommand(collection, scanner));
        commands.add(new ShowCommand(collection, scanner));
        commands.add(new AddCommand(collection, scanner));
        commands.add(new UpdateCommand(collection, scanner));
        commands.add(new RemoveByIdCommand(collection, scanner));
        commands.add(new ClearCommand(collection, scanner));
        commands.add(new SaveCommand(collection, scanner));
        commands.add(new ExecuteScriptCommand(collection, scanner));
        commands.add(new ExitCommand(collection, scanner));
        commands.add(new AddIfMaxCommand(collection, scanner));
        commands.add(new AddIfMinCommand(collection, scanner));
        commands.add(new HistoryCommand(collection, scanner));
        commands.add(new PrintDescendingCommand(collection, scanner));
        commands.add(new PrintFieldAscendingSalaryCommand(collection, scanner));
        commands.add(new PrintFieldDescendingSalaryCommand(collection, scanner));
    }

    /**
     * Возвращает список всех доступных команд.
     *
     * @return список команд
     */
    public List<Command> getCommands() {
        return commands;
    }

    /**
     * Находит команду по её имени.
     *
     * @param name имя команды
     * @return команда или null, если команда не найдена
     */
    public Command getCommandByName(String name) {
        for (Command command : commands) {
            if (command.getName().equalsIgnoreCase(name)) {
                return command;
            }
        }
        return null;
    }
}