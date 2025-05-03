package org.example.command;

import org.example.collection.WorkerCollection;
import org.example.input.InputHandler;
import org.example.model.Worker;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для обновления работника по его ID.
 */
public class UpdateCommand extends AbstractCommand {

    /**
     * Конструктор команды update.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public UpdateCommand(WorkerCollection collection, Scanner scanner) {
        super("update", "обновить значение элемента коллекции по id", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        try {
            InputHandler inputHandler = new InputHandler(scanner);
            Long id = inputHandler.parseLong(args, "ID");
            if (!collection.existsById(id)) {
                System.out.println("Работник с ID " + id + " не найден.");
                return false;
            }

            Worker worker = inputHandler.readWorker(fromScript, id);
            if (collection.updateWorker(id, worker)) {
                System.out.println("Работник с ID " + id + " успешно обновлен.");
            } else {
                System.out.println("Не удалось обновить работника с ID " + id + ".");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при обновлении работника: " + e.getMessage());
        }
        return false;
    }
}