package org.example.command;

import org.example.collection.WorkerCollection;
import org.example.input.InputHandler;
import org.example.model.Worker;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для добавления нового работника, если его значение меньше, чем у наименьшего элемента коллекции.
 */
public class AddIfMinCommand extends AbstractCommand {

    /**
     * Конструктор команды add_if_min.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public AddIfMinCommand(WorkerCollection collection, Scanner scanner) {
        super("add_if_min", "добавить новый элемент, если его значение меньше, чем у наименьшего элемента", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        try {
            InputHandler inputHandler = new InputHandler(scanner);
            Worker worker = inputHandler.readWorker(fromScript);
            if (collection.addIfMin(worker)) {
                System.out.println("Работник успешно добавлен в коллекцию.");
            } else {
                System.out.println(
                        "Работник не добавлен, так как его значение не меньше, чем у наименьшего элемента коллекции.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при добавлении работника: " + e.getMessage());
        }
        return false;
    }
}