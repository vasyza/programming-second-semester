package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.model.User;
import org.example.common.model.Worker;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;
import org.example.server.util.PasswordHasher;

import java.util.List;
import java.util.Optional;

public class RequestHandler {
    private final CollectionManager collectionManager;
    private final DatabaseManager databaseManager;
    private static final Logger logger = LogManager.getLogger(RequestHandler.class);

    public RequestHandler(CollectionManager collectionManager, DatabaseManager databaseManager) {
        this.collectionManager = collectionManager;
        this.databaseManager = databaseManager;
    }

    private Optional<User> authenticateUser(String username, String plainPassword) {
        if (username == null || plainPassword == null) {
            return Optional.empty();
        }
        Optional<User> userOpt = databaseManager.getUserByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (PasswordHasher.verifyPassword(plainPassword, user.getHashedPassword())) {
                return Optional.of(user);
            } else {
                logger.warn("Authentication failed for user {}: Incorrect password.", username);
            }
        } else {
            logger.warn("Authentication failed: User {} not found.", username);
        }
        return Optional.empty();
    }

    public CommandResponse handleRequest(CommandRequest request) {
        if (request == null) {
            logger.warn("Received null request.");
            return new CommandResponse(false, "Ошибка сервера: получен пустой запрос.", null);
        }
        String commandName = request.getCommandName();
        Object argument = request.getArgument();
        String username = request.getUsername();
        String password = request.getPassword();

        logger.info("Handling request: Command='{}', User='{}', ArgType='{}'", commandName, username,
                argument != null ? argument.getClass().getSimpleName() : "null");

        if ("register".equalsIgnoreCase(commandName)) {
            if (argument instanceof String[] args && args.length == 2) {
                String regUsername = args[0];
                String regPassword = args[1];
                if (regUsername == null || regUsername.trim().isEmpty() || regPassword == null
                        || regPassword.isEmpty()) {
                    return new CommandResponse(false,
                            "Имя пользователя и пароль не могут быть пустыми для регистрации.", null);
                }
                Optional<User> newUser = databaseManager.registerUser(regUsername, regPassword);
                if (newUser.isPresent()) {
                    return new CommandResponse(true, "Пользователь " + regUsername + " успешно зарегистрирован.", null);
                } else {
                    return new CommandResponse(false, "Не удалось зарегистрировать пользователя " + regUsername
                            + ". Возможно, имя пользователя уже занято.", null);
                }
            } else {
                return new CommandResponse(false,
                        "Ошибка: неверный формат аргументов для 'register'. Ожидается [username, password].", null);
            }
        }

        Optional<User> authenticatedUserOpt = authenticateUser(username, password);

        if ("login".equalsIgnoreCase(commandName)) {
            if (authenticatedUserOpt.isPresent()) {
                User user = authenticatedUserOpt.get();
                user.setHashedPassword(null);
                return new CommandResponse(true, "Пользователь " + username + " успешно вошел в систему.", user);
            } else {
                return new CommandResponse(false, "Ошибка входа: неверное имя пользователя или пароль.", null);
            }
        }

        if (authenticatedUserOpt.isEmpty()) {
            logger.warn(
                    "Unauthorized access attempt: Command='{}', User='{}'. Credentials provided: username='{}', password provided: {}",
                    commandName, username, username != null, password != null && !password.isEmpty());
            return new CommandResponse(false, "Ошибка аутентификации: доступ запрещен. Войдите или зарегистрируйтесь.",
                    null);
        }

        User authenticatedUser = authenticatedUserOpt.get();
        int userId = authenticatedUser.getId();
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
                    message = workersByLocation.isEmpty() ? "Коллекция пуста."
                            : "Элементы коллекции (отсортированы по местоположению):";
                    break;
                case "add":
                    if (argument instanceof Worker worker) {
                        message = collectionManager.addWorker(worker, userId);
                        success = message.startsWith("Работник успешно добавлен");
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для команды 'add'. Ожидался Worker.";
                        logger.warn("Invalid argument type for 'add': {}",
                                argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "update":
                    if (argument instanceof Object[] args && args.length == 2 && args[0] instanceof Long updateId
                            && args[1] instanceof Worker updateWorker) {
                        message = collectionManager.updateWorker(updateId, updateWorker, userId);
                        success = message.startsWith("Работник с ID " + updateId + " успешно обновлен");
                    } else {
                        success = false;
                        message = "Ошибка: неверная структура или тип аргумента для команды 'update'.";
                        logger.warn("Invalid argument for 'update': {}", argument);
                    }
                    break;
                case "remove_by_id":
                    if (argument instanceof Long removeId) {
                        message = collectionManager.removeWorkerById(removeId, userId);
                        success = message.startsWith("Работник с ID " + removeId + " успешно удален");
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для 'remove_by_id'. Ожидался Long.";
                        logger.warn("Invalid argument type for 'remove_by_id': {}",
                                argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "clear":
                    message = collectionManager.clear(userId);
                    success = message.contains("были удалены") || message.contains("Работники пользователя очищены");
                    break;
                case "add_if_min":
                    if (argument instanceof Worker addMinWorker) {
                        message = collectionManager.addIfMin(addMinWorker, userId);
                        success = message.contains("добавлен (add_if_min)");
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для 'add_if_min'. Ожидался Worker.";
                        logger.warn("Invalid argument type for 'add_if_min': {}",
                                argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "add_if_max":
                    if (argument instanceof Worker addMaxWorker) {
                        message = collectionManager.addIfMax(addMaxWorker, userId);
                        success = message.contains("добавлен (add_if_max)");
                    } else {
                        success = false;
                        message = "Ошибка: неверный тип аргумента для 'add_if_max'. Ожидался Worker.";
                        logger.warn("Invalid argument type for 'add_if_max': {}",
                                argument != null ? argument.getClass().getName() : "null");
                    }
                    break;
                case "print_descending":
                    List<Worker> descendingWorkers = collectionManager.getDescendingById();
                    resultData = descendingWorkers;
                    message = descendingWorkers.isEmpty() ? "Коллекция пуста."
                            : "Элементы коллекции в порядке убывания (по ID):";
                    break;
                case "print_field_ascending_salary":
                    List<Long> salariesAsc = collectionManager.getSalariesAscending();
                    resultData = salariesAsc;
                    message = salariesAsc.isEmpty() ? "В коллекции нет работников с указанной зарплатой."
                            : "Значения поля salary в порядке возрастания:";
                    break;
                case "print_field_descending_salary":
                    List<Long> salariesDesc = collectionManager.getSalariesDescending();
                    resultData = salariesDesc;
                    message = salariesDesc.isEmpty() ? "В коллекции нет работников с указанной зарплатой."
                            : "Значения поля salary в порядке убывания:";
                    break;
                default:
                    success = false;
                    message = "Неизвестная команда получена сервером: " + commandName;
                    logger.warn("Unknown command received from client {}: {}", username, commandName);
            }
        } catch (IllegalArgumentException e) {
            success = false;
            message = "Ошибка валидации данных на сервере для команды '" + commandName + "': " + e.getMessage();
            logger.warn("Validation error during command '{}' for user {}: {}", commandName, username, e.getMessage());
        } catch (Exception e) {
            success = false;
            message = "Внутренняя ошибка сервера при выполнении команды '" + commandName
                    + "'. Обратитесь к администратору.";
            logger.error("Exception during command '{}' for user {}: {}", commandName, username, e.getMessage(), e);
        }

        CommandResponse response = new CommandResponse(success, message, resultData);
        logger.debug("Prepared response for command '{}' (User {}): success={}, message='{}', data={}", commandName,
                username, success, message, resultData != null ? "present" : "absent");
        return response;
    }

    private String getHelpText() {
        return """
                register <username> <password> : зарегистрировать нового пользователя
                login <username> <password> : войти в систему
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