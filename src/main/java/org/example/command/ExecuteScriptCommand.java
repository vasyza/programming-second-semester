package org.example.command;

import org.example.collection.WorkerCollection;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Команда для выполнения скрипта из указанного файла.
 */
public class ExecuteScriptCommand extends AbstractCommand {
    private static final Set<String> executingScripts = new HashSet<>();

    /**
     * Конструктор команды execute_script.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public ExecuteScriptCommand(WorkerCollection collection, Scanner scanner) {
        super("execute_script", "считать и исполнить скрипт из указанного файла", collection, scanner);
    }

    @Override
    public boolean execute(String args, boolean fromScript) throws IOException {
        if (args.isEmpty()) {
            System.out.println("Не указано имя файла.");
            return false;
        }

        String filePath = args.trim();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Файл не существует: " + filePath);
            return false;
        }

        if (!file.canRead()) {
            System.out.println("Нет прав на чтение файла: " + filePath);
            return false;
        }

        if (executingScripts.contains(filePath)) {
            System.out.println("Обнаружена рекурсия в скрипте: " + filePath);
            return false;
        }

        executingScripts.add(filePath);

        try (Scanner fileScanner = new Scanner(file)) {
            System.out.println("Выполнение скрипта: " + filePath);
            CommandManager commandManager = new CommandManager(collection, fileScanner);

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty())
                    continue;

                System.out.println("> " + line);
                if (commandManager.executeCommand(line, true)) {
                    System.out.println("Выполнение скрипта прервано командой exit.");
                    break;
                }
            }

            System.out.println("Выполнение скрипта завершено: " + filePath);
        } catch (Exception e) {
            System.out.println("Ошибка при выполнении скрипта: " + e.getMessage());
        } finally {
            executingScripts.remove(filePath);
        }

        return false;
    }
}