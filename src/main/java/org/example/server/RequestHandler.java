package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.model.Worker;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import java.util.List;

public class RequestHandler {
    private final CollectionManager collectionManager;
    private static final Logger logger = LogManager.getLogger(RequestHandler.class);

    public RequestHandler(CollectionManager collectionManager) {
        this.collectionManager = collectionManager;
    }

    public CommandResponse handleRequest(CommandRequest request) {
        if (request == null) {
            logger.warn("Получен null запрос от клиента.");
            return new CommandResponse(false, "Ошибка сервера: получен пустой запрос.", null);
        }
        String commandName = request.getCommandName();
        Object argument = request.getArgument();
        logger.info("Обработка запроса: Команда='{}', Аргумент='{}'", commandName, argument != null ? argument.toString().substring(0, Math.min(argument.toString().length(), 100)) : "null");


        String message;
        Object resultData = null;
        boolean success = true;

        try {
            switch (commandName.toLowerCase()) {
                case "help":
                    message = getHelpText();
                    break;
                case "info":
                    message = collectionManager.getInfo();
                    break;
                case "show":
                    List<Worker> workersByLocation = collectionManager.getWorkersSortedByLocation();
                    resultData = workersByLocation;
                    message = workersByLocation.isEmpty() ? "Коллекция пуста." : "Элементы коллекции (отсортированы по местоположению):";
                    break;
                case "add":
                    if (argument instanceof Worker worker) {
                        collectionManager.addWorker(worker);
                        message = "Работник успешно добавлен.";
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для команды 'add'. Ожидался Worker.";
                        logger.warn("Неверный тип аргумента для 'add': {}", argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "update":
                    if (argument instanceof Object[] args && args.length == 2 && args[0] instanceof Long updateId && args[1] instanceof Worker updateWorker) {
                        if (collectionManager.updateWorker(updateId, updateWorker)) {
                            message = "Работник с ID " + updateId + " успешно обновлен.";
                        } else {
                            success = false;
                            message = "Работник с ID " + updateId + " не найден для обновления.";
                        }
                    } else {
                        success = false;
                        message = "Ошибка: неверная структура или тип аргумента для команды 'update'.";
                        logger.warn("Неверный аргумент для 'update': {}", argument);
                    }
                    break;
                case "remove_by_id":
                    if (argument instanceof Long removeId) {
                        if (collectionManager.removeWorkerById(removeId)) {
                            message = "Работник с ID " + removeId + " успешно удален.";
                        } else {
                            success = false;
                            message = "Работник с ID " + removeId + " не найден для удаления.";
                        }
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для команды 'remove_by_id'. Ожидался Long.";
                        logger.warn("Неверный тип аргумента для 'remove_by_id': {}", argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "clear":
                    collectionManager.clear();
                    message = "Коллекция очищена.";
                    break;
                case "execute_script":
                case "history":
                    success = false;
                    message = "Ошибка: команда '" + commandName + "' предназначена для выполнения на клиенте и не должна отправляться на сервер.";
                    logger.warn("Получена команда '{}', предназначенная для клиента.", commandName);
                    break;
                case "add_if_min":
                    if (argument instanceof Worker addMinWorker) {
                        if (collectionManager.addIfMin(addMinWorker)) {
                            message = "Работник успешно добавлен (т.к. его значение ID меньше минимального).";
                        } else {
                            message = "Работник не добавлен (его значение ID не меньше минимального или коллекция пуста).";
                        }
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для команды 'add_if_min'. Ожидался Worker.";
                        logger.warn("Неверный тип аргумента для 'add_if_min': {}", argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "add_if_max":
                    if (argument instanceof Worker addMaxWorker) {
                        if (collectionManager.addIfMax(addMaxWorker)) {
                            message = "Работник успешно добавлен (т.к. его значение ID больше максимального).";
                        } else {
                            message = "Работник не добавлен (его значение ID не больше максимального или коллекция пуста).";
                        }
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для команды 'add_if_max'. Ожидался Worker.";
                        logger.warn("Неверный тип аргумента для 'add_if_max': {}", argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "print_descending":
                    List<Worker> descendingWorkers = collectionManager.getDescendingById();
                    resultData = descendingWorkers;
                    message = descendingWorkers.isEmpty() ? "Коллекция пуста." : "Элементы коллекции в порядке убывания (по ID):";
                    break;
                case "print_field_ascending_salary":
                    List<Long> salariesAsc = collectionManager.getSalariesAscending();
                    resultData = salariesAsc;
                    message = salariesAsc.isEmpty() ? "В коллекции нет работников с указанной зарплатой." : "Значения поля salary в порядке возрастания:";
                    break;
                case "print_field_descending_salary":
                    List<Long> salariesDesc = collectionManager.getSalariesDescending();
                    resultData = salariesDesc;
                    message = salariesDesc.isEmpty() ? "В коллекции нет работников с указанной зарплатой." : "Значения поля salary в порядке убывания:";
                    break;
                default:
                    success = false;
                    message = "Неизвестная команда получена сервером: " + commandName;
                    logger.warn("Получена неизвестная команда от клиента: {}", commandName);
            }
        } catch (IllegalArgumentException e) {
            success = false;
            message = "Ошибка валидации данных на сервере для команды '" + commandName + "': " + e.getMessage();
            logger.warn("Ошибка валидации при обработке команды '{}': {}", commandName, e.getMessage());
        } catch (Exception e) {
            success = false;
            message = "Внутренняя ошибка сервера при выполнении команды '" + commandName + "'. Обратитесь к администратору.";
            logger.error("Исключение при выполнении команды '{}': {}", commandName, e.getMessage(), e);
        }

        CommandResponse response = new CommandResponse(success, message, resultData);
        logger.debug("Подготовлен ответ для команды '{}': успех={}, сообщение='{}', данные={}", commandName, success, message, resultData != null ? "присутствуют" : "отсутствуют");
        return response;
    }

    private String getHelpText() {
        return """
                help : вывести справку по доступным командам
                info : вывести информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)
                show : вывести все элементы коллекции в строковом представлении (отсортированы по местоположению)
                add {element} : добавить новый элемент в коллекцию
                update id {element} : обновить значение элемента коллекции, id которого равен заданному
                remove_by_id id : удалить элемент из коллекции по его id
                clear : очистить коллекцию
                add_if_min {element} : добавить новый элемент в коллекцию, если его значение (ID) меньше, чем у наименьшего элемента этой коллекции
                add_if_max {element} : добавить новый элемент, если его значение (ID) превышает значение наибольшего элемента
                print_descending : вывести элементы коллекции в порядке убывания (по ID)
                print_field_ascending_salary : вывести значения поля salary всех элементов в порядке возрастания
                print_field_descending_salary : вывести значения поля salary всех элементов в порядке убывания
                
                execute_script file_name : считать и исполнить скрипт из указанного файла.
                history : вывести последние 15 команд (без их аргументов).
                exit : завершить программу клиента.
                """;
    }
}