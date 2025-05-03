package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для вывода информации о коллекции.
 */
public class InfoCommand extends AbstractCommand {

    /**
     * Конструктор команды info.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public InfoCommand(WorkerCollection collection, Scanner scanner) {
        super("info", "вывести информацию о коллекции", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        System.out.println("Информация о коллекции:");
        System.out.println("Тип: " + collection.getType());
        System.out.println("Дата инициализации: " + collection.getInitializationDate());
        System.out.println("Количество элементов: " + collection.size());
        return false;
    }
}