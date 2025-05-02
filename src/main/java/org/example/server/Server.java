package org.example.server;

import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;
import org.example.common.util.SerializationUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Server {
    private static final int PORT = 12345; // Порт
    private static final int BUFFER_SIZE = 65535; // Размер макета
    private static final Logger logger = LogManager.getLogger(Server.class);


    private final DatagramSocket socket;
    private final RequestHandler requestHandler;
    private final CollectionManager collectionManager;


    public Server(CollectionManager manager) throws SocketException {
        this.collectionManager = manager;
        this.socket = new DatagramSocket(PORT);
        this.requestHandler = new RequestHandler(manager);
        logger.info("Сервер запущен на порту {}", PORT);
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Завершение работы сервера...");
            try {
                collectionManager.saveToFile();
                logger.info("Коллекция успешно сохранена перед выходом.");
            } catch (IOException e) {
                logger.error("Ошибка при сохранении коллекции перед выходом: {}", e.getMessage(), e);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            logger.info("Сервер остановлен.");
        }));


        byte[] buffer = new byte[BUFFER_SIZE];

        while (true) {
            try {
                DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                logger.debug("Ожидание пакета...");
                socket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                logger.info("Получен пакет от {}:{}", clientAddress.getHostAddress(), clientPort);

                byte[] receivedData = Arrays.copyOf(receivePacket.getData(), receivePacket.getLength());


                CommandRequest request = SerializationUtils.deserialize(receivedData);
                logger.debug("Десериализован запрос: {}", request.getCommandName());


                CommandResponse response = requestHandler.handleRequest(request);


                byte[] sendData = SerializationUtils.serialize(response);
                logger.debug("Сериализован ответ размером {} байт", sendData.length);

                if (sendData.length > BUFFER_SIZE) {
                    logger.warn("Размер ответа ({}) превышает размер буфера UDP ({}). Данные могут быть потеряны", sendData.length, BUFFER_SIZE);
                    response = new CommandResponse(false, "Ошибка: Ответ сервера слишком большой для UDP.", null);
                    sendData = SerializationUtils.serialize(response);
                }


                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientAddress, clientPort);
                socket.send(sendPacket);
                logger.info("Отправлен ответ клиенту {}:{}", clientAddress.getHostAddress(), clientPort);

            } catch (SocketException e) {
                logger.info("Сокет закрыт");
                break;
            } catch (IOException | ClassNotFoundException e) {
                logger.error("Ошибка при обработке пакета: {}", e.getMessage(), e);
            } catch (Exception e) {
                logger.error("Неожиданная ошибка сервера: {}", e.getMessage(), e);
            }
        }
    }

    public static void main(String[] args) {
        String filePath = System.getenv("LAB_DATA_PATH");
        if (filePath == null || filePath.isEmpty()) {
            logger.error("Переменная окружения LAB_DATA_PATH не задана.");
            System.exit(1);
        }

        CollectionManager collectionManager = new CollectionManager();
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.exists()) {
                collectionManager.loadFromFile(filePath);
                logger.info("Коллекция загружена из {}", filePath);
            } else {
                logger.warn("Файл {} не найден. Коллекция будет пустой.", filePath);
            }
        } catch (IOException | IllegalArgumentException e) {
            logger.error("Ошибка загрузки коллекции из файла {}: {}", filePath, e.getMessage(), e);
            System.exit(1);
        }


        try {
            Server server = new Server(collectionManager);
            server.run();
        } catch (SocketException e) {
            logger.fatal("Не удалось запустить сервер: {}", e.getMessage(), e);
        }
    }
}