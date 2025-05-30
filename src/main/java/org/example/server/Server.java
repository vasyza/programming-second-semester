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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Server {
    private static final int PORT = 12345;
    private static final Logger logger = LogManager.getLogger(Server.class);

    private final RequestHandler requestHandler;
    private ServerSocket serverSocket;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    private final ExecutorService consoleExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ServerConsoleThread");
        t.setDaemon(true);
        return t;
    });

    private final ForkJoinPool readRequestPool = ForkJoinPool.commonPool();
    private final ExecutorService processRequestPool = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final ExecutorService sendResponsePool = Executors.newCachedThreadPool();

    public Server(DatabaseManager dbManager) {
        CollectionManager collectionManager = new CollectionManager(dbManager);
        this.requestHandler = new RequestHandler(collectionManager, dbManager);
    }

    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Server is already running or is in the process of starting.");
            return;
        }

        try {
            serverSocket = new ServerSocket(PORT);
            logger.info("TCP Server started on port {}. Waiting for connections...", PORT);

            consoleExecutor.submit(this::handleServerConsoleCommands);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Server shutdown initiated...");
                stopServer();
            }, "ServerShutdownHook"));

            while (isRunning.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (!isRunning.get()) {
                        clientSocket.close();
                        break;
                    }
                    logger.info("New TCP connection received from client: {}", clientSocket.getRemoteSocketAddress());
                    readRequestPool.submit(new ReadRequestTask(clientSocket));
                } catch (SocketException e) {
                    if (!isRunning.get()) {
                        logger.info("Server socket closed during server shutdown.");
                        break;
                    }
                    logger.warn("SocketException in main server loop (socket may have been closed): {}",
                            e.getMessage());
                } catch (IOException e) {
                    if (isRunning.get()) {
                        logger.error("I/O error while waiting for connection: {}", e.getMessage(), e);
                    } else {
                        logger.info("IOException during server shutdown: {}", e.getMessage());
                        break;
                    }
                }
            }
        } catch (IOException e) {
            logger.fatal("Failed to start server on port {}: {}", PORT, e.getMessage(), e);
        } finally {
            logger.info("Main server loop completed. Calling stopServer().");
            stopServer();
        }
    }

    private class ReadRequestTask implements Runnable {
        private final Socket clientSocket;

        public ReadRequestTask(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            ObjectInputStream ois = null;
            try {
                ois = new ObjectInputStream(clientSocket.getInputStream());
                logger.debug("Reading request from client {} in thread {}", clientSocket.getRemoteSocketAddress(),
                        Thread.currentThread().getName());
                CommandRequest request = (CommandRequest) ois.readObject();
                logger.info("Received request '{}' from client {}", request.getCommandName(),
                        clientSocket.getRemoteSocketAddress());

                processRequestPool.submit(new ProcessRequestTask(request, clientSocket, ois));

            } catch (EOFException e) {
                logger.warn("Client {} closed connection (EOF) before receiving complete request.",
                        clientSocket.getRemoteSocketAddress());
                closeSocketAndStream(clientSocket, ois, null);
            } catch (SocketException e) {
                logger.warn("Connection with client {} was interrupted (SocketException): {}",
                        clientSocket.getRemoteSocketAddress(), e.getMessage());
                closeSocketAndStream(clientSocket, ois, null);
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Error reading/deserializing request from client {}: {}",
                        clientSocket.getRemoteSocketAddress(), e.getMessage(), e);
                closeSocketAndStream(clientSocket, ois, null);
            }
        }
    }

    private class ProcessRequestTask implements Runnable {
        private final CommandRequest request;
        private final Socket clientSocket;
        private final ObjectInputStream ois;

        public ProcessRequestTask(CommandRequest request, Socket clientSocket, ObjectInputStream ois) {
            this.request = request;
            this.clientSocket = clientSocket;
            this.ois = ois;
        }

        @Override
        public void run() {
            logger.debug("Processing request '{}' from client {} in thread {}", request.getCommandName(),
                    clientSocket.getRemoteSocketAddress(), Thread.currentThread().getName());
            CommandResponse response = requestHandler.handleRequest(request);

            sendResponsePool.submit(new SendResponseTask(response, request.getCommandName(), clientSocket, ois));
        }
    }

    private class SendResponseTask implements Runnable {
        private final CommandResponse response;
        private final String commandName;
        private final Socket clientSocket;
        private final ObjectInputStream ois;

        public SendResponseTask(CommandResponse response, String commandName, Socket clientSocket,
                                ObjectInputStream ois) {
            this.response = response;
            this.commandName = commandName;
            this.clientSocket = clientSocket;
            this.ois = ois;
        }

        @Override
        public void run() {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(clientSocket.getOutputStream());
                logger.debug("Sending response to command '{}' to client {} in thread {}", commandName,
                        clientSocket.getRemoteSocketAddress(), Thread.currentThread().getName());
                oos.writeObject(response);
                oos.flush();
                logger.info("Response to command '{}' sent to client {}", commandName,
                        clientSocket.getRemoteSocketAddress());
            } catch (IOException e) {
                logger.error("Error sending response to client {}: {}", clientSocket.getRemoteSocketAddress(),
                        e.getMessage(), e);
            } finally {
                closeSocketAndStream(clientSocket, ois, oos);
            }
        }
    }

    private void closeSocketAndStream(Socket socket, ObjectInputStream ois, ObjectOutputStream oos) {
        String clientAddr = (socket != null && socket.getRemoteSocketAddress() != null)
                ? socket.getRemoteSocketAddress().toString()
                : "unknown client";
        try {
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing ObjectInputStream for client {}: {}", clientAddr, e.getMessage());
        }
        try {
            if (oos != null) {
                oos.close();
            }
        } catch (IOException e) {
            logger.warn("Error closing ObjectOutputStream for client {}: {}", clientAddr, e.getMessage());
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                logger.debug("Connection with client {} closed.", clientAddr);
            }
        } catch (IOException e) {
            logger.warn("Error closing client socket {}: {}", clientAddr, e.getMessage());
        }
    }

    private void handleServerConsoleCommands() {
        logger.info("Server console is active. Available commands: 'save', 'exit', 'set_data_file <path>'");
        try (Scanner consoleScanner = new Scanner(System.in)) {
            while (isRunning.get()) {
                try {
                    System.out.print("server-console> ");
                    if (!consoleScanner.hasNextLine()) {
                        logger.info("Console input completed (EOF).");
                        if (isRunning.get()) {
                            logger.info("EOF in console, initiating server shutdown.");
                            System.out.println("EOF в консоли, инициирую остановку сервера...");
                            isRunning.set(false);
                            if (serverSocket != null && !serverSocket.isClosed())
                                serverSocket.close();
                        }
                        break;
                    }
                    String line = consoleScanner.nextLine().trim();
                    if (line.isEmpty())
                        continue;

                    logger.debug("Command received from server console: '{}'", line);
                    String command = line.toLowerCase();
                    if ("exit".equals(command)) {
                        logger.info("'exit' command received from server console. Initiating shutdown...");
                        System.out.println("Завершение работы сервера...");
                        isRunning.set(false);
                        if (serverSocket != null && !serverSocket.isClosed()) {
                            try {
                                serverSocket.close();
                            } catch (IOException e) {
                                logger.error("Error closing server socket on 'exit' command: {}", e.getMessage());
                            }
                        }
                        return;
                    } else {
                        logger.warn("Unknown command from server console: '{}'", command);
                        System.out.println("Неизвестная команда. Доступно: 'exit'.");
                    }
                } catch (NoSuchElementException e) {
                    logger.info("Console input completed (NoSuchElementException). Initiating server shutdown.");
                    System.out.println("Консольный ввод неожиданно завершен, инициирую остановку сервера...");
                    isRunning.set(false);
                    if (serverSocket != null && !serverSocket.isClosed()) {
                        try {
                            serverSocket.close();
                        } catch (IOException ex) {
                            logger.error("Error closing server socket on 'exit' command: {}", ex.getMessage());
                        }
                    }
                    break;
                } catch (Exception e) {
                    if (isRunning.get()) {
                        logger.error("Error in server console thread: {}", e.getMessage(), e);
                        System.err.println("Произошла ошибка в консоли сервера: " + e.getMessage());
                    } else {
                        break;
                    }
                }
            }
        } finally {
            logger.info("Server console thread is finishing work.");
        }
    }

    public void stopServer() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.info("Server is already stopped or is in the process of stopping.");
            return;
        }
        logger.info("Starting graceful server shutdown procedure...");

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
                logger.info("Server socket successfully closed. New connections are not accepted.");
            } catch (IOException e) {
                logger.error("Error closing server socket: {}", e.getMessage(), e);
            }
        }

        shutdownExecutorService("ConsoleExecutor", consoleExecutor, 5);
        shutdownExecutorService("SendResponsePool", sendResponsePool, 10);
        shutdownExecutorService("ProcessRequestPool", processRequestPool, 10);
        shutdownExecutorService("ReadRequestPool", readRequestPool, 15);

        logger.info("Server completely stopped.");
    }

    private void shutdownExecutorService(String name, ExecutorService executor, int timeoutSeconds) {
        logger.info("Shutting down {}...", name);
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                logger.warn("{} did not terminate within {} sec. Forcing termination...", name, timeoutSeconds);
                executor.shutdownNow();
                if (!executor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
                    logger.error("{} did not terminate even after forced interruption.", name);
                } else {
                    logger.info("{} terminated after forced interruption.", name);
                }
            } else {
                logger.info("{} successfully terminated.", name);
            }
        } catch (InterruptedException ie) {
            logger.warn("Waiting for {} termination interrupted. Forcing termination...", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        String dbHost = System.getenv("DB_HOST");
        String dbName = System.getenv("DB_NAME");
        String dbUser = System.getenv("DB_USER");
        String dbPassword = System.getenv("DB_PASSWORD");

        if (dbHost == null) {
            dbHost = "localhost";
            logger.warn("Environment variable DB_HOST is not set. Using default value 'pg'");
        }
        if (dbName == null) {
            dbName = "studs";
            logger.warn("Environment variable DB_NAME is not set. Using default value 'studs'");
        }
        if (dbUser == null) {
            logger.fatal("Environment variable DB_USER is not set. Server cannot be started.");
            System.err.println("ОШИБКА: Переменная окружения DB_USER обязательна.");
            System.exit(1);
        }
        if (dbPassword == null) {
            logger.fatal("Environment variable DB_PASSWORD is not set. Server cannot be started.");
            System.err.println("ОШИБКА: Переменная окружения DB_PASSWORD обязательна.");
            System.exit(1);
        }

        System.out.println("Попытка подключиться к БД: " + dbHost + "/" + dbName + " от имени " + dbUser);

        DatabaseManager databaseManager;
        try {
            databaseManager = new DatabaseManager(dbHost, dbName, dbUser, dbPassword);
        } catch (RuntimeException e) {
            logger.fatal("Failed to initialize DatabaseManager: {}", e.getMessage(), e);
            System.err.println(
                    "КРИТИЧЕСКАЯ ОШИБКА: Не удалось подключиться к базе данных. Сервер не может быть запущен.");
            System.exit(1);
            return;
        }

        Server server = new Server(databaseManager);
        server.start();
    }
}