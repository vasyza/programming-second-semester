package org.example.client;

import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Optional;

public class NetworkManager {
    private static final String SERVER_ADDRESS_DEFAULT = "localhost";
    private static final int SERVER_PORT_DEFAULT = 12345;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int RESPONSE_TIMEOUT_MS = 15000;
    private static final int MAX_CONNECTION_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 3000;

    private final String serverHost;
    private final int serverPort;
    private Socket socket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;

    public NetworkManager() {
        this(SERVER_ADDRESS_DEFAULT, SERVER_PORT_DEFAULT);
    }

    public NetworkManager(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    public Optional<CommandResponse> sendRequest(CommandRequest request) {
        Socket socket = null;
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;

        for (int attempt = 1; attempt <= MAX_CONNECTION_ATTEMPTS; attempt++) {
            try {
                System.out.println("Попытка подключения и отправки запроса '" + request.getCommandName() + "' (попытка " + attempt + "/" + MAX_CONNECTION_ATTEMPTS + ")...");
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverHost, serverPort), CONNECTION_TIMEOUT_MS);
                socket.setSoTimeout(RESPONSE_TIMEOUT_MS);

                oos = new ObjectOutputStream(socket.getOutputStream());
                System.out.println("Отправка запроса '" + request.getCommandName() + "' на сервер...");
                oos.writeObject(request);
                oos.flush();
                System.out.println("Запрос отправлен. Ожидание ответа от сервера...");

                ois = new ObjectInputStream(socket.getInputStream());
                CommandResponse response = (CommandResponse) ois.readObject();
                System.out.println("Ответ от сервера получен.");
                return Optional.of(response);

            } catch (SocketTimeoutException e) {
                System.err.println("Таймаут при обмене данными с сервером (попытка " + attempt + "): " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Ошибка ввода-вывода при обмене данными с сервером (попытка " + attempt + "): " + e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println("Ошибка: не удалось десериализовать ответ от сервера: " + e.getMessage());
            } finally {
                try {
                    if (ois != null) ois.close();
                } catch (IOException ignored) {
                }
                try {
                    if (oos != null) oos.close();
                } catch (IOException ignored) {
                }
                try {
                    if (socket != null && !socket.isClosed()) socket.close();
                } catch (IOException ignored) {
                }
                ois = null;
                oos = null;
                socket = null;
            }

            if (attempt < MAX_CONNECTION_ATTEMPTS) {
                System.out.println("Следующая попытка через " + (RETRY_DELAY_MS / 1000) + " сек.");
                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    System.err.println("Попытка подключения прервана.");
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
            }
        }
        System.err.println("Не удалось отправить запрос и получить ответ от сервера " + serverHost + ":" + serverPort + " после " + MAX_CONNECTION_ATTEMPTS + " попыток.");
        return Optional.empty();
    }


    public void closeConnection() {
        closeResources();
    }

    private void closeResources() {
        try {
            if (ois != null) ois.close();
        } catch (IOException ignored) {
        }
        try {
            if (oos != null) oos.close();
        } catch (IOException ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
                System.out.println("Соединение с сервером закрыто клиентом.");
            }
        } catch (IOException e) {
            System.err.println("Ошибка при закрытии сокета клиентом: " + e.getMessage());
        } finally {
            ois = null;
            oos = null;
            socket = null;
        }
    }
}