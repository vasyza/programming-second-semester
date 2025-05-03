package org.example.command;

import org.example.collection.WorkerCollection;
import org.example.model.Worker;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Команда для вывода всех элементов коллекции.
 */
public class ShowCommand extends AbstractCommand {

    /**
     * Конструктор команды show.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public ShowCommand(WorkerCollection collection, Scanner scanner) {
        super("show", "вывести все элементы коллекции", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        List<Worker> workers = collection.getAllWorkers();
        if (workers.isEmpty()) {
            System.out.println("Коллекция пуста.");
            return false;
        }

        System.out.println("Элементы коллекции:");
        for (Worker worker : workers) {
            System.out.println(worker);
        }
        return false;
    }
}