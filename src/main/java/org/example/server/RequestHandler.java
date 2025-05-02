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
        logger.info("Handling request: {}", request.getCommandName());
        String commandName = request.getCommandName();
        Object argument = request.getArgument();
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
                    resultData = collectionManager.getSortedWorkers();
                    message = resultData == null || ((List<?>)resultData).isEmpty() ? "Коллекция пуста." : "Элементы коллекции:";
                    break;
                case "add":
                    if (argument instanceof Worker) {
                        collectionManager.addWorker((Worker) argument);
                        message = "Работник успешно добавлен.";
                    } else { throw new IllegalArgumentException("Invalid argument type for add"); }
                    break;
                case "update":
                    if (argument instanceof Object[] args && args.length == 2 && args[0] instanceof Long updateId && args[1] instanceof Worker updateWorker) {
                        if (collectionManager.updateWorker(updateId, updateWorker)) {
                            message = "Работник с ID " + updateId + " успешно обновлен.";
                        } else {
                            success = false;
                            message = "Работник с ID " + updateId + " не найден.";
                        }
                    } else { throw new IllegalArgumentException("Invalid argument type/structure for update"); }
                    break;
                case "remove_by_id":
                    if (argument instanceof Long removeId) {
                        if (collectionManager.removeWorkerById(removeId)) {
                            message = "Работник с ID " + removeId + " успешно удален.";
                        } else {
                            success = false;
                            message = "Работник с ID " + removeId + " не найден.";
                        }
                    } else { throw new IllegalArgumentException("Invalid argument type for remove_by_id"); }
                    break;
                case "clear":
                    collectionManager.clear();
                    message = "Коллекция очищена.";
                    break;
                case "save":
                    collectionManager.saveToFile();
                    message = "Коллекция успешно сохранена.";
                    break;
                case "execute_script":
                    success = false;
                    message = "Команда 'execute_script' выполняется на клиенте.";
                    break;
                case "add_if_min":
                    if (argument instanceof Worker addMinWorker) {
                        if (collectionManager.addIfMin(addMinWorker)) {
                            message = "Работник успешно добавлен.";
                        } else {
                            message = "Работник не добавлен.";
                        }
                    } else { throw new IllegalArgumentException("Invalid argument type for add_if_min"); }
                    break;
                case "add_if_max":
                    if (argument instanceof Worker addMaxWorker) {
                        if (collectionManager.addIfMax(addMaxWorker)) {
                            message = "Работник успешно добавлен.";
                        } else {
                            message = "Работник не добавлен.";
                        }
                    } else { throw new IllegalArgumentException("Invalid argument type for add_if_max"); }
                    break;
                case "print_descending":
                    List<Worker> descendingWorkers = collectionManager.getDescending();
                    message = descendingWorkers.isEmpty() ? "Коллекция пуста." : "Элементы коллекции в порядке убывания:";
                    resultData = descendingWorkers;
                    break;
                case "print_field_descending_salary":
                    List<Long> salariesDescending = collectionManager.getSalariesDescending();
                    message = salariesDescending.isEmpty() ? "Зарплаты не найдены." : "Зарплаты по убыванию:";
                    resultData = salariesDescending;
                    break;
                case "print_field_ascending_salary":
                    List<Long> salaries = collectionManager.getSalariesAscending();
                    message = salaries.isEmpty() ? "Зарплаты не найдены." : "Зарплаты по возрастанию:";
                    resultData = salaries;
                    break;
                default:
                    success = false;
                    message = "Неизвестная команда: " + commandName;
                    logger.warn("Unknown command received: {}", commandName);
            }
        } catch (Exception e) {
            success = false;
            message = "Ошибка на сервере при выполнении команды '" + commandName + "': " + e.getMessage();
            logger.error("Error executing command '{}'", commandName, e);
        }

        CommandResponse response = new CommandResponse(success, message, resultData);
        logger.info("Sending response for command {}: success={}, message='{}'", commandName, success, message);
        return response;
    }

    private String getHelpText() {
        return """
               Доступные команды:
               help : вывести справку по доступным командам
               info : вывести информацию о коллекции (тип, дата инициализации, количество элементов и т.д.)
               show : вывести все элементы коллекции в строковом представлении
               add {element} : добавить новый элемент в коллекцию
               update id {element} : обновить значение элемента коллекции, id которого равен заданному
               remove_by_id id : удалить элемент из коллекции по его id
               clear : очистить коллекцию
               save : сохранить коллекцию в файл
               execute_script file_name : (Выполняется клиентом) считать и исполнить скрипт из указанного файла.
               exit : завершить программу клиента
               add_if_min {element} : добавить новый элемент в коллекцию, если его значение меньше, чем у наименьшего элемента этой коллекции
               add_if_max {element} : добавить новый элемент, если его значение превышает значение наибольшего элемента
               history : вывести последние 15 команд (без их аргументов)
               print_descending : вывести элементы коллекции в порядке убывания
               print_field_ascending_salary : вывести значения поля salary всех элементов в порядке возрастания
               print_field_descending_salary : вывести значения поля salary всех элементов в порядке убывания
               """;
    }
}