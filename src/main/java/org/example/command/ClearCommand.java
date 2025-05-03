package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для очистки коллекции.
 */
public class ClearCommand extends AbstractCommand {

    /**
     * Конструктор команды clear.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public ClearCommand(WorkerCollection collection, Scanner scanner) {
        super("clear", "очистить коллекцию", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        collection.clear();
        System.out.println("Коллекция очищена.");
        return false;
    }
}