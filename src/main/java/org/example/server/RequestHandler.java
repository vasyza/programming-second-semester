package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.model.Worker;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


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
                case "execute_script":
                    success = false;
                    message = "Команда 'execute_script' выполняется на клиенте.";
                    break;
                case "remove_head":
                    Optional<Worker> removedHead = collectionManager.removeHead();
                    if (removedHead.isPresent()) {
                        message = "Первый элемент удален:";
                        resultData = removedHead.get();
                    } else {
                        message = "Коллекция пуста, нечего удалять.";
                    }
                    break;
                case "add_if_max":
                    if (argument instanceof Worker addMaxWorker) {
                        if (collectionManager.addIfMax(addMaxWorker)) {
                            message = "Работник успешно добавлен (как максимальный).";
                        } else {
                            message = "Работник не добавлен (не максимальный).";
                        }
                    } else { throw new IllegalArgumentException("Invalid argument type for add_if_max"); }
                    break;
                case "remove_lower":
                    if (argument instanceof Worker) {
                        long removedCount = collectionManager.removeLower((Worker) argument);
                        message = "Удалено " + removedCount + " элементов, меньших чем заданный.";
                    } else { throw new IllegalArgumentException("Invalid argument type for remove_lower"); }
                    break;
                case "remove_any_by_start_date":
                    if (argument instanceof LocalDateTime) {
                        if (collectionManager.removeAnyByStartDate((LocalDateTime) argument)) {
                            message = "Один работник с указанной датой начала удален.";
                        } else {
                            message = "Работники с указанной датой начала не найдены.";
                        }
                    } else { throw new IllegalArgumentException("Invalid argument type for remove_any_by_start_date"); }
                    break;
                case "group_counting_by_start_date":
                    Map<LocalDateTime, Long> groups = collectionManager.groupCountingByStartDate();
                    message = "Группировка по дате начала:";
                    resultData = groups.entrySet().stream()
                            .map(entry -> entry.getKey() + ": " + entry.getValue() + " элемент(а)")
                            .collect(Collectors.toList());
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
               execute_script file_name : (Выполняется клиентом) считать и исполнить скрипт из указанного файла.
               exit : завершить программу клиента
               remove_head : вывести первый элемент коллекции и удалить его
               add_if_max {element} : добавить новый элемент, если его значение превышает значение наибольшего элемента
               remove_lower {element} : удалить из коллекции все элементы, меньшие, чем заданный
               remove_any_by_start_date startDate : удалить из коллекции один элемент по дате начала
               group_counting_by_start_date : сгруппировать элементы по дате начала и вывести количество в каждой группе
               print_field_ascending_salary : вывести значения поля salary всех элементов в порядке возрастания
               """;
    }
}