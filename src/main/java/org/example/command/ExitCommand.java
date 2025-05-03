package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.IOException;
import java.util.Scanner;

/**
 * Команда для завершения программы.
 */
public class ExitCommand extends AbstractCommand {

    /**
     * Конструктор команды exit.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public ExitCommand(WorkerCollection collection, Scanner scanner) {
        super("exit", "завершить программу (без сохранения в файл)", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        return true; // Возвращаем true, чтобы сигнализировать о завершении программы
    }
}