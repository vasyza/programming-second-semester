package org.example.command;

import org.example.collection.WorkerCollection;
import org.example.input.InputHandler;
import org.example.model.Worker;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для добавления нового работника в коллекцию.
 */
public class AddCommand extends AbstractCommand {

    /**
     * Конструктор команды add.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public AddCommand(WorkerCollection collection, Scanner scanner) {
        super("add", "добавить новый элемент в коллекцию", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        try {
            InputHandler inputHandler = new InputHandler(scanner);
            Worker worker = inputHandler.readWorker(fromScript);
            collection.addWorker(worker);
            System.out.println("Работник успешно добавлен в коллекцию.");
        } catch (Exception e) {
            System.out.println("Ошибка при добавлении работника: " + e.getMessage());
        }
        return false;
    }
}