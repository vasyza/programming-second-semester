package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для сохранения коллекции в файл.
 */
public class SaveCommand extends AbstractCommand {

    /**
     * Конструктор команды save.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public SaveCommand(WorkerCollection collection, Scanner scanner) {
        super("save", "сохранить коллекцию в файл", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        try {
            collection.saveToFile();
            System.out.println("Коллекция успешно сохранена в файл.");
        } catch (Exception e) {
            System.out.println("Ошибка при сохранении коллекции: " + e.getMessage());
        }
        return false;
    }
}