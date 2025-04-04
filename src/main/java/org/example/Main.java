package org.example;

import org.example.collection.WorkerCollection;
import org.example.command.CommandManager;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Главный класс приложения для управления коллекцией работников.
 */
public class Main {
    private static final String ENV_VAR_NAME = "LAB_DATA_PATH";

    /**
     * Точка входа в приложение.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        // Получаем путь к файлу из переменной окружения
        String filePath = System.getenv(ENV_VAR_NAME);
        if (filePath == null || filePath.isEmpty()) {
            System.err.println("Ошибка: Переменная окружения " + ENV_VAR_NAME + " не задана или пуста.");
            System.err.println(
                    "Пожалуйста, установите переменную окружения " + ENV_VAR_NAME + " с путем к файлу данных.");
            return;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Ошибка: Файл " + filePath + " не существует.");
            System.err.println("Будет создан новый файл при сохранении коллекции.");
        } else if (!file.canRead() || !file.canWrite()) {
            System.err.println("Ошибка: Недостаточно прав для чтения/записи файла " + filePath);
            return;
        }

        // Инициализируем коллекцию
        WorkerCollection collection = new WorkerCollection();

        // Если файл существует, пытаемся загрузить данные
        if (file.exists()) {
            try {
                collection.loadFromFile(filePath);
                System.out.println("Данные успешно загружены из файла: " + filePath);
            } catch (IOException e) {
                System.err.println("Ошибка при чтении файла: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.err.println("Ошибка в формате данных: " + e.getMessage());
            }
        }

        // Создаем менеджер команд и запускаем интерактивный режим
        try (Scanner scanner = new Scanner(System.in)) {
            CommandManager commandManager = new CommandManager(collection, scanner);
            commandManager.interactiveMode();
        } catch (Exception e) {
            System.err.println("Произошла непредвиденная ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}