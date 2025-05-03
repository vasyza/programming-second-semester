package org.example.command;

import org.example.collection.WorkerCollection;
import org.example.model.Worker;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * Команда для вывода элементов коллекции в порядке убывания.
 */
public class PrintDescendingCommand extends AbstractCommand {

    /**
     * Конструктор команды print_descending.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public PrintDescendingCommand(WorkerCollection collection, Scanner scanner) {
        super("print_descending", "вывести элементы коллекции в порядке убывания", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        List<Worker> descendingWorkers = collection.getDescending();
        if (descendingWorkers.isEmpty()) {
            System.out.println("Коллекция пуста.");
        } else {
            System.out.println("Элементы коллекции в порядке убывания:");
            for (Worker worker : descendingWorkers) {
                System.out.println(worker);
            }
        }
        return false;
    }
}