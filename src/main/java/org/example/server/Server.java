package org.example.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private static final int PORT = 12345;
    private static final Logger logger = LogManager.getLogger(Server.class);

    private final CollectionManager collectionManager;
    private final RequestHandler requestHandler;
    private ServerSocket serverSocket;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final ExecutorService consoleExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ServerConsoleThread");
        t.setDaemon(true);
        return t;
    });

    public Server(CollectionManager manager) {
        this.collectionManager = manager;
        this.requestHandler = new RequestHandler(manager);
    }

    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Сервер уже запущен или находится в процессе запуска.");
            return;
        }

        try {
            serverSocket = new ServerSocket(PORT);
            logger.info("TCP Сервер запущен на порту {}. Ожидание подключений...", PORT);

            consoleExecutor.submit(this::handleServerConsoleCommands);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Инициируется остановка сервера...");
                stopServer();
            }, "ServerShutdownHook"));

            // Основной цикл приема подключений
            while (isRunning.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (!isRunning.get()) {
                        clientSocket.close();
                        break;
                    }
                    logger.info("Получено новое TCP подключение от клиента: {}", clientSocket.getRemoteSocketAddress());
                    handleClient(clientSocket);
                } catch (SocketException e) {
                    if (!isRunning.get()) {
                        logger.info("Серверный сокет закрыт во время остановки сервера.");
                        break;
                    }
                    logger.warn("SocketException в основном цикле сервера (возможно, сокет был закрыт): {}", e.getMessage());
                } catch (IOException e) {
                    if (isRunning.get()) {
                        logger.error("Ошибка ввода-вывода при ожидании подключения: {}", e.getMessage(), e);
                    } else {
                        logger.info("IOException при остановке сервера: {}", e.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.fatal("Не удалось запустить сервер на порту {}: {}", PORT, e.getMessage(), e);
        } finally {
            logger.info("Основной цикл сервера завершен. Вызов stopServer().");
            stopServer();
        }
    }

    private void handleClient(Socket clientSocket) {
        logger.debug("Начало обработки клиента {}", clientSocket.getRemoteSocketAddress());
        try (ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream()); ObjectOutputStream oos = new ObjectOutputStream(clientSocket.getOutputStream())) {

            logger.trace("Потоки для клиента {} созданы.", clientSocket.getRemoteSocketAddress());

            CommandRequest request = (CommandRequest) ois.readObject();
            logger.info("Получен запрос '{}' от клиента {}", request.getCommandName(), clientSocket.getRemoteSocketAddress());

            CommandResponse response = requestHandler.handleRequest(request);

            oos.writeObject(response);
            oos.flush();
            logger.info("Отправлен ответ на запрос '{}' клиенту {}", request.getCommandName(), clientSocket.getRemoteSocketAddress());

        } catch (EOFException e) {
            logger.warn("Клиент {} закрыл соединение (EOFException) до получения полного запроса или во время ожидания.", clientSocket.getRemoteSocketAddress());
        } catch (SocketException e) {
            logger.warn("Соединение с клиентом {} было прервано (SocketException): {}", clientSocket.getRemoteSocketAddress(), e.getMessage());
        } catch (IOException e) {
            logger.error("Ошибка ввода-вывода при обработке клиента {}: {}", clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.error("Ошибка десериализации запроса от клиента {}: {}", clientSocket.getRemoteSocketAddress(), e.getMessage());
        } finally {
            try {
                if (!clientSocket.isClosed()) {
                    clientSocket.close();
                }
                logger.debug("Соединение с клиентом {} корректно закрыто.", clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                logger.error("Ошибка при закрытии сокета клиента {}: {}", clientSocket.getRemoteSocketAddress(), e.getMessage());
            }
        }
    }

    private void handleServerConsoleCommands() {
        logger.info("Консоль сервера активна. Доступные команды: 'save', 'exit', 'set_data_file <path>'");
        try (Scanner consoleScanner = new Scanner(System.in)) {
            while (isRunning.get()) {
                try {
                    System.out.print("server-console> ");
                    if (!consoleScanner.hasNextLine()) {
                        logger.info("Консольный ввод завершен (EOF).");
                        if (isRunning.get()) {
                            logger.info("EOF в консоли, инициирую остановку сервера.");
                            System.out.println("EOF в консоли, инициирую остановку сервера...");
                            isRunning.set(false);
                            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
                        }
                        break;
                    }
                    String line = consoleScanner.nextLine().trim();
                    if (line.isEmpty()) continue;

                    logger.debug("Получена команда с консоли сервера: '{}'", line);
                    String[] parts = line.split("\\s+", 2);
                    String command = parts[0].toLowerCase();

                    switch (command) {
                        case "save":
                            try {
                                collectionManager.saveToFile();
                                logger.info("Коллекция сохранена по команде 'save' с консоли сервера.");
                                System.out.println("Коллекция успешно сохранена в файл: " + collectionManager.getFilePath());
                            } catch (IOException | IllegalStateException e) {
                                logger.error("Ошибка при сохранении коллекции по команде с консоли: {}", e.getMessage(), e);
                                System.err.println("Ошибка сохранения: " + e.getMessage());
                            }
                            break;
                        case "set_data_file":
                            if (parts.length > 1) {
                                String newFilePath = parts[1];
                                collectionManager.setFilePath(newFilePath);
                                System.out.println("Путь к файлу данных изменен на: " + newFilePath);
                                System.out.println("Пожалуйста, перезапустите сервер или выполните 'load' (если такая команда будет добавлена) для применения изменений.");
                            } else {
                                System.err.println("Использование: set_data_file <путь_к_файлу>");
                            }
                            break;
                        case "exit":
                            logger.info("Команда 'exit' получена с консоли сервера. Инициируется остановка...");
                            System.out.println("Завершение работы сервера...");
                            isRunning.set(false);
                            if (serverSocket != null && !serverSocket.isClosed()) {
                                serverSocket.close();
                            }
                            return;
                        default:
                            logger.warn("Неизвестная команда с консоли сервера: '{}'", command);
                            System.out.println("Неизвестная команда. Доступно: 'save', 'exit', 'set_data_file <path>'.");
                            break;
                    }
                } catch (NoSuchElementException e) {
                    logger.info("Консольный ввод завершен (NoSuchElementException).");
                    if (isRunning.get()) {
                        logger.info("Консольный ввод неожиданно завершен, инициирую остановку сервера.");
                        System.out.println("Консольный ввод неожиданно завершен, инициирую остановку сервера...");
                        isRunning.set(false);
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            try {
                                serverSocket.close();
                            } catch (IOException er) {
                                logger.error("Ошибка при закрытии серверного сокета по команде 'exit': {}", er.getMessage(), er);
                            }
                            return;
                        }
                    }
                    break;
                } catch (Exception e) {
                    if (isRunning.get()) {
                        logger.error("Ошибка в потоке консоли сервера: {}", e.getMessage(), e);
                        System.err.println("Произошла ошибка в консоли сервера: " + e.getMessage());
                    } else {
                        break;
                    }
                }
            }
        } finally {
            logger.info("Поток консоли сервера завершает работу.");
        }
    }

    public void stopServer() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.info("Сервер уже остановлен или находится в процессе остановки.");
            return;
        }
        logger.info("Начало процедуры штатной остановки сервера...");

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Серверный сокет успешно закрыт. Новые подключения не принимаются.");
            } catch (IOException e) {
                logger.error("Ошибка при закрытии серверного сокета: {}", e.getMessage(), e);
            }
        }

        consoleExecutor.shutdown();
        try {
            if (!consoleExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                consoleExecutor.shutdownNow();
                logger.warn("Поток консоли сервера не завершился штатно, вызван shutdownNow().");
            } else {
                logger.info("Поток консоли сервера успешно завершен.");
            }
        } catch (InterruptedException e) {
            consoleExecutor.shutdownNow();
            logger.error("Ожидание завершения потока консоли прервано.", e);
            Thread.currentThread().interrupt();
        }

        try {
            if (collectionManager.getFilePath() != null && !collectionManager.getFilePath().isEmpty()) {
                collectionManager.saveToFile();
                logger.info("Коллекция успешно сохранена в файл {} перед остановкой сервера.", collectionManager.getFilePath());
            } else {
                logger.warn("Путь к файлу данных не задан, коллекция не будет сохранена при остановке.");
            }
        } catch (IOException | IllegalStateException e) {
            logger.error("Ошибка при сохранении коллекции во время остановки сервера: {}", e.getMessage(), e);
        }

        logger.info("Сервер полностью остановлен.");
    }

    public static void main(String[] args) {
        String filePath = System.getenv("LAB_DATA_PATH");
        if (filePath == null || filePath.isEmpty()) {
            logger.warn("Переменная окружения LAB_DATA_PATH не задана. Используется путь по умолчанию: 'data.csv'");
            filePath = "data.csv";
        }
        System.out.println("Сервер будет использовать файл данных: " + filePath);


        CollectionManager collectionManager = new CollectionManager();
        collectionManager.setFilePath(filePath);

        try {
            collectionManager.loadFromFile();
        } catch (IOException e) {
            logger.error("Критическая ошибка при начальной загрузке коллекции из файла {}: {}", collectionManager.getFilePath(), e.getMessage(), e);
            System.err.println("Не удалось загрузить данные из файла: " + e.getMessage() + ". Проверьте файл и права доступа.");
            System.exit(1);
        }

        Server server = new Server(collectionManager);
        server.start();
    }
}