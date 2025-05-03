package org.example.command;

import org.example.collection.WorkerCollection;

import java.util.Scanner;

/**
 * Абстрактный класс, реализующий общую функциональность для всех команд.
 */
public abstract class AbstractCommand implements Command {
    private final String name;
    private final String description;
    protected final WorkerCollection collection;
    protected final Scanner scanner;

    /**
     * Конструктор.
     *
     * @param name имя команды
     * @param description описание команды
     * @param collection коллекция работников
     * @param scanner сканнер для чтения ввода пользователя
     */
    public AbstractCommand(String name, String description, WorkerCollection collection, Scanner scanner) {
        this.name = name;
        this.description = description;
        this.collection = collection;
        this.scanner = scanner;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }
}