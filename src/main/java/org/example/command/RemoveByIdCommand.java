package org.example.command;

import org.example.collection.WorkerCollection;
import org.example.input.InputHandler;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для удаления работника по его ID.
 */
public class RemoveByIdCommand extends AbstractCommand {

    /**
     * Конструктор команды remove_by_id.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public RemoveByIdCommand(WorkerCollection collection, Scanner scanner) {
        super("remove_by_id", "удалить элемент из коллекции по его id", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        try {
            InputHandler inputHandler = new InputHandler(scanner);
            Long id = inputHandler.parseLong(args, "ID");
            if (collection.removeWorkerById(id)) {
                System.out.println("Работник с ID " + id + " успешно удален.");
            } else {
                System.out.println("Работник с ID " + id + " не найден.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при удалении работника: " + e.getMessage());
        }
        return false;
    }

}