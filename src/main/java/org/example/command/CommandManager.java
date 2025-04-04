package org.example.command;

import org.example.collection.IdGenerator;
import org.example.collection.WorkerCollection;
import org.example.model.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Класс для управления командами пользователя.
 * Обрабатывает ввод пользователя и выполняет соответствующие команды.
 */
public class CommandManager {
    private final WorkerCollection collection;
    private final Scanner scanner;
    private final List<String> commandHistory;
    private final int historySize = 15;
    private final Set<String> executingScripts;

    /**
     * Конструктор класса CommandManager.
     *
     * @param collection коллекция работников
     * @param scanner    сканнер для чтения ввода пользователя
     */
    public CommandManager(WorkerCollection collection, Scanner scanner) {
        this.collection = collection;
        this.scanner = scanner;
        this.commandHistory = new ArrayList<>();
        this.executingScripts = new HashSet<>();
    }

    /**
     * Запускает интерактивный режим работы с коллекцией.
     */
    public void interactiveMode() {
        System.out.println("Добро пожаловать в программу управления коллекцией работников!");
        System.out.println("Введите 'help' для получения списка доступных команд.");

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine()) {
                break;
            }

            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                continue;
            }

            try {
                if (executeCommand(input, false)) {
                    break;
                }
            } catch (Exception e) {
                System.out.println("Ошибка при выполнении команды: " + e.getMessage());
            }
        }
    }

    /**
     * Выполняет команду, введенную пользователем.
     *
     * @param input      строка с командой
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @return true, если программа должна завершиться, false - в противном случае
     * @throws IOException если произошла ошибка ввода-вывода
     */
    public boolean executeCommand(String input, boolean fromScript) throws IOException {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        if (!fromScript) {
            addToHistory(command);
        }

        switch (command) {
            case "help":
                printHelp();
                break;
            case "info":
                printInfo();
                break;
            case "show":
                showCollection();
                break;
            case "add":
                addWorker(fromScript);
                break;
            case "update":
                updateWorker(args, fromScript);
                break;
            case "remove_by_id":
                removeById(args);
                break;
            case "clear":
                clearCollection();
                break;
            case "save":
                saveCollection();
                break;
            case "execute_script":
                executeScript(args);
                break;
            case "exit":
                return true;
            case "add_if_max":
                addIfMax(fromScript);
                break;
            case "add_if_min":
                addIfMin(fromScript);
                break;
            case "history":
                printHistory();
                break;
            case "print_descending":
                printDescending();
                break;
            case "print_field_ascending_salary":
                printFieldAscendingSalary();
                break;
            case "print_field_descending_salary":
                printFieldDescendingSalary();
                break;
            default:
                System.out.println("Неизвестная команда. Введите 'help' для получения списка доступных команд.");
        }

        return false;
    }

    /**
     * Добавляет команду в историю команд.
     *
     * @param command команда для добавления в историю
     */
    private void addToHistory(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > historySize) {
            commandHistory.remove(0);
        }
    }

    /**
     * Выводит справку по доступным командам.
     */
    private void printHelp() {
        System.out.println("Доступные команды:");
        System.out.println("help : вывести справку по доступным командам");
        System.out.println("info : вывести информацию о коллекции");
        System.out.println("show : вывести все элементы коллекции");
        System.out.println("add : добавить новый элемент в коллекцию");
        System.out.println("update id : обновить значение элемента коллекции по id");
        System.out.println("remove_by_id id : удалить элемент из коллекции по его id");
        System.out.println("clear : очистить коллекцию");
        System.out.println("save : сохранить коллекцию в файл");
        System.out.println("execute_script file_name : считать и исполнить скрипт из указанного файла");
        System.out.println("exit : завершить программу (без сохранения в файл)");
        System.out.println(
                "add_if_max : добавить новый элемент, если его значение превышает значение наибольшего элемента");
        System.out.println("add_if_min : добавить новый элемент, если его значение меньше, чем у наименьшего элемента");
        System.out.println("history : вывести последние 15 команд");
        System.out.println("print_descending : вывести элементы коллекции в порядке убывания");
        System.out.println("print_field_ascending_salary : вывести значения поля salary в порядке возрастания");
        System.out.println("print_field_descending_salary : вывести значения поля salary в порядке убывания");
    }

    /**
     * Выводит информацию о коллекции.
     */
    private void printInfo() {
        System.out.println("Информация о коллекции:");
        System.out.println("Тип: " + collection.getType());
        System.out.println("Дата инициализации: " + collection.getInitializationDate());
        System.out.println("Количество элементов: " + collection.size());
    }

    /**
     * Выводит все элементы коллекции.
     */
    private void showCollection() {
        List<Worker> workers = collection.getAllWorkers();
        if (workers.isEmpty()) {
            System.out.println("Коллекция пуста.");
            return;
        }

        System.out.println("Элементы коллекции:");
        for (Worker worker : workers) {
            System.out.println(worker);
        }
    }

    /**
     * Добавляет нового работника в коллекцию.
     *
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     */
    private void addWorker(boolean fromScript) {
        try {
            Worker worker = readWorker(fromScript);
            collection.addWorker(worker);
            System.out.println("Работник успешно добавлен в коллекцию.");
        } catch (Exception e) {
            System.out.println("Ошибка при добавлении работника: " + e.getMessage());
        }
    }

    /**
     * Обновляет работника с указанным ID.
     *
     * @param args       аргументы команды
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     */
    private void updateWorker(String args, boolean fromScript) {
        try {
            Long id = parseLong(args, "ID");
            if (!collection.existsById(id)) {
                System.out.println("Работник с ID " + id + " не найден.");
                return;
            }

            Worker worker = readWorker(fromScript, id);
            if (collection.updateWorker(id, worker)) {
                System.out.println("Работник с ID " + id + " успешно обновлен.");
            } else {
                System.out.println("Не удалось обновить работника с ID " + id + ".");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при обновлении работника: " + e.getMessage());
        }
    }

    /**
     * Удаляет работника с указанным ID.
     *
     * @param args аргументы команды
     */
    private void removeById(String args) {
        try {
            Long id = parseLong(args, "ID");
            if (collection.removeWorkerById(id)) {
                System.out.println("Работник с ID " + id + " успешно удален.");
            } else {
                System.out.println("Работник с ID " + id + " не найден.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при удалении работника: " + e.getMessage());
        }
    }

    /**
     * Очищает коллекцию.
     */
    private void clearCollection() {
        collection.clear();
        System.out.println("Коллекция очищена.");
    }

    /**
     * Сохраняет коллекцию в файл.
     *
     * @throws IOException если произошла ошибка при сохранении в файл
     */
    private void saveCollection() throws IOException {
        try {
            collection.saveToFile();
            System.out.println("Коллекция успешно сохранена в файл.");
        } catch (Exception e) {
            System.out.println("Ошибка при сохранении коллекции: " + e.getMessage());
        }
    }

    /**
     * Выполняет скрипт из указанного файла.
     *
     * @param args аргументы команды
     * @throws IOException если произошла ошибка при чтении файла
     */
    private void executeScript(String args) throws IOException {
        if (args.isEmpty()) {
            System.out.println("Не указано имя файла.");
            return;
        }

        String filePath = args.trim();
        File file = new File(filePath);

        if (!file.exists()) {
            System.out.println("Файл не существует: " + filePath);
            return;
        }

        if (!file.canRead()) {
            System.out.println("Нет прав на чтение файла: " + filePath);
            return;
        }

        if (executingScripts.contains(filePath)) {
            System.out.println("Обнаружена рекурсия в скрипте: " + filePath);
            return;
        }

        executingScripts.add(filePath);

        try (Scanner fileScanner = new Scanner(file)) {
            System.out.println("Выполнение скрипта: " + filePath);

            while (fileScanner.hasNextLine()) {
                String line = fileScanner.nextLine().trim();
                if (line.isEmpty())
                    continue;

                System.out.println("> " + line);
                if (executeCommand(line, true)) {
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
    }

    /**
     * Добавляет работника, если его значение превышает значение наибольшего
     * элемента коллекции.
     *
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     */
    private void addIfMax(boolean fromScript) {
        try {
            Worker worker = readWorker(fromScript);
            if (collection.addIfMax(worker)) {
                System.out.println("Работник успешно добавлен в коллекцию.");
            } else {
                System.out.println(
                        "Работник не добавлен, так как его значение не превышает значение наибольшего элемента коллекции.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при добавлении работника: " + e.getMessage());
        }
    }

    /**
     * Добавляет работника, если его значение меньше, чем у наименьшего элемента
     * коллекции.
     *
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     */
    private void addIfMin(boolean fromScript) {
        try {
            Worker worker = readWorker(fromScript);
            if (collection.addIfMin(worker)) {
                System.out.println("Работник успешно добавлен в коллекцию.");
            } else {
                System.out.println(
                        "Работник не добавлен, так как его значение не меньше, чем у наименьшего элемента коллекции.");
            }
        } catch (Exception e) {
            System.out.println("Ошибка при добавлении работника: " + e.getMessage());
        }
    }

    /**
     * Выводит последние 15 команд.
     */
    private void printHistory() {
        if (commandHistory.isEmpty()) {
            System.out.println("История команд пуста.");
            return;
        }

        System.out.println("Последние " + Math.min(historySize, commandHistory.size()) + " команд:");
        for (int i = 0; i < commandHistory.size(); i++) {
            System.out.println((i + 1) + ". " + commandHistory.get(i));
        }
    }

    /**
     * Выводит элементы коллекции в порядке убывания.
     */
    private void printDescending() {
        List<Worker> workers = collection.getDescending();
        if (workers.isEmpty()) {
            System.out.println("Коллекция пуста.");
            return;
        }

        System.out.println("Элементы коллекции в порядке убывания:");
        for (Worker worker : workers) {
            System.out.println(worker);
        }
    }

    /**
     * Выводит значения поля salary всех элементов в порядке возрастания.
     */
    private void printFieldAscendingSalary() {
        List<Long> salaries = collection.getSalariesAscending();
        if (salaries.isEmpty()) {
            System.out.println("В коллекции нет элементов с непустым полем salary.");
            return;
        }

        System.out.println("Значения поля salary в порядке возрастания:");
        for (Long salary : salaries) {
            System.out.println(salary);
        }
    }

    /**
     * Выводит значения поля salary всех элементов в порядке убывания.
     */
    private void printFieldDescendingSalary() {
        List<Long> salaries = collection.getSalariesDescending();
        if (salaries.isEmpty()) {
            System.out.println("В коллекции нет элементов с непустым полем salary.");
            return;
        }

        System.out.println("Значения поля salary в порядке убывания:");
        for (Long salary : salaries) {
            System.out.println(salary);
        }
    }

    /**
     * Читает данные работника из ввода пользователя.
     *
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @return новый объект Worker
     */
    private Worker readWorker(boolean fromScript) {
        return readWorker(fromScript, IdGenerator.getNextId());
    }

    /**
     * Читает данные работника из ввода пользователя с указанным ID.
     *
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param id         ID работника
     * @return новый объект Worker
     */
    private Worker readWorker(boolean fromScript, Long id) {
        String name = readString("Введите имя работника: ", fromScript, false);
        Float x = readFloat("Введите координату x: ", fromScript, false);
        Double y = readDouble("Введите координату y (> -72): ", fromScript, false);
        Long salary = readLong("Введите зарплату (> 0, или пустая строка для null): ", fromScript, true);
        LocalDateTime startDate = readLocalDateTime("Введите дату начала работы (в формате yyyy-MM-ddTHH:mm:ss): ",
                fromScript, false);
        ZonedDateTime endDate = readZonedDateTime(
                "Введите дату окончания работы (в формате yyyy-MM-ddTHH:mm:ss+ZZ:ZZ, или пустая строка для null): ",
                fromScript, true);
        Position position = readPosition(
                "Введите должность " + Position.getAllValues() + " (или пустая строка для null): ", fromScript, true);
        Integer annualTurnover = readInteger("Введите годовой оборот организации (> 0, или пустая строка для null): ",
                fromScript, true);
        OrganizationType organizationType = readOrganizationType(
                "Введите тип организации " + OrganizationType.getAllValues() + ": ", fromScript, false);

        Coordinates coordinates = new Coordinates(x, y);
        Organization organization = new Organization(annualTurnover, organizationType);
        LocalDate creationDate = LocalDate.now();

        return new Worker(id, name, coordinates, creationDate, salary, startDate, endDate, position, organization);
    }

    /**
     * Читает строку из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная строка
     */
    private String readString(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            if (!fromScript) {
                System.out.print(prompt);
            }

            if (!scanner.hasNextLine()) {
                throw new NoSuchElementException("Достигнут конец ввода.");
            }

            String input = scanner.nextLine().trim();
            if (input.isEmpty() && !allowEmpty) {
                if (fromScript) {
                    throw new IllegalArgumentException("Пустая строка не допускается в скрипте.");
                }
                System.out.println("Строка не может быть пустой. Повторите ввод.");
                continue;
            }

            return input.isEmpty() && allowEmpty ? null : input;
        }
    }

    /**
     * Читает целое число типа Long из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    private Long readLong(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                Long value = Long.parseLong(input);
                if (value <= 0) {
                    if (fromScript) {
                        throw new IllegalArgumentException("Значение должно быть больше 0.");
                    }
                    System.out.println("Значение должно быть больше 0. Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает целое число типа Integer из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    private Integer readInteger(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                Integer value = Integer.parseInt(input);
                if (value <= 0) {
                    if (fromScript) {
                        throw new IllegalArgumentException("Значение должно быть больше 0.");
                    }
                    System.out.println("Значение должно быть больше 0. Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает число с плавающей точкой типа Float из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    private Float readFloat(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return Float.parseFloat(input);
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает число с плавающей точкой типа Double из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенное число или null, если ввод пуст и allowEmpty=true
     */
    private Double readDouble(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                Double value = Double.parseDouble(input);
                if (value <= -72) {
                    if (fromScript) {
                        throw new IllegalArgumentException("Значение должно быть больше -72.");
                    }
                    System.out.println("Значение должно быть больше -72. Повторите ввод.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат числа: " + input);
                }
                System.out.println("Некорректный формат числа. Повторите ввод.");
            }
        }
    }

    /**
     * Читает дату и время типа LocalDateTime из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная дата и время или null, если ввод пуст и allowEmpty=true
     */
    private LocalDateTime readLocalDateTime(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return LocalDateTime.parse(input);
            } catch (DateTimeParseException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат даты и времени: " + input);
                }
                System.out.println(
                        "Некорректный формат даты и времени. Используйте формат yyyy-MM-ddTHH:mm:ss. Повторите ввод.");
            }
        }
    }

    /**
     * Читает дату и время с часовым поясом типа ZonedDateTime из ввода
     * пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная дата и время с часовым поясом или null, если ввод пуст и
     *         allowEmpty=true
     */
    private ZonedDateTime readZonedDateTime(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return ZonedDateTime.parse(input);
            } catch (DateTimeParseException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректный формат даты и времени с часовым поясом: " + input);
                }
                System.out.println(
                        "Некорректный формат даты и времени с часовым поясом. Используйте формат yyyy-MM-ddTHH:mm:ss+ZZ:ZZ. Повторите ввод.");
            }
        }
    }

    /**
     * Читает должность типа Position из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенная должность или null, если ввод пуст и allowEmpty=true
     */
    private Position readPosition(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return Position.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректное значение должности: " + input);
                }
                System.out.println("Некорректное значение должности. Допустимые значения: " + Position.getAllValues()
                        + ". Повторите ввод.");
            }
        }
    }

    /**
     * Читает тип организации типа OrganizationType из ввода пользователя.
     *
     * @param prompt     приглашение к вводу
     * @param fromScript флаг, указывающий, выполняется ли команда из скрипта
     * @param allowEmpty флаг, указывающий, разрешена ли пустая строка
     * @return введенный тип организации или null, если ввод пуст и allowEmpty=true
     */
    private OrganizationType readOrganizationType(String prompt, boolean fromScript, boolean allowEmpty) {
        while (true) {
            String input = readString(prompt, fromScript, allowEmpty);
            if (input == null) {
                return null;
            }

            try {
                return OrganizationType.valueOf(input.toUpperCase());
            } catch (IllegalArgumentException e) {
                if (fromScript) {
                    throw new IllegalArgumentException("Некорректное значение типа организации: " + input);
                }
                System.out.println("Некорректное значение типа организации. Допустимые значения: "
                        + OrganizationType.getAllValues() + ". Повторите ввод.");
            }
        }
    }

    /**
     * Парсит Long из строки.
     *
     * @param str       строка для парсинга
     * @param fieldName имя поля для сообщения об ошибке
     * @return распарсенное значение
     * @throws IllegalArgumentException если строка не может быть преобразована в
     *                                  Long
     */
    private Long parseLong(String str, String fieldName) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " не может быть пустым.");
        }
        try {
            return Long.parseLong(str.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректный формат " + fieldName + ": " + str);
        }
    }
}