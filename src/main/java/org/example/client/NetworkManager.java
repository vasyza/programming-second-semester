package org.example.client;

import org.example.common.request.CommandRequest;
import org.example.common.response.CommandResponse;
import org.example.common.util.SerializationUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Optional;

public class NetworkManager {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final int TIMEOUT_MS = 5000;
    private static final int BUFFER_SIZE = 65535;

    private final DatagramChannel channel;
    private final Selector selector;
    private final InetSocketAddress serverAddress;
    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);


    public NetworkManager() throws IOException {
        channel = DatagramChannel.open();
        channel.configureBlocking(false);
        selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);
        serverAddress = new InetSocketAddress(SERVER_ADDRESS, SERVER_PORT);
    }

    public void sendRequest(CommandRequest request) throws IOException {
        System.out.println("Отправка запроса '" + request.getCommandName() + "' на сервер..."); // Debug
        byte[] data = SerializationUtils.serialize(request);
        buffer.clear();
        buffer.put(data);
        buffer.flip();
        channel.send(buffer, serverAddress);
        System.out.println("Запрос отправлен."); // Debug
    }

    public Optional<CommandResponse> receiveResponse() throws IOException, ClassNotFoundException {
        System.out.println("Ожидание ответа от сервера (timeout " + TIMEOUT_MS + "ms)..."); // Debug
        int readyChannels = selector.select(TIMEOUT_MS);

        if (readyChannels == 0) {
            System.out.println("Таймаут ожидания ответа от сервера."); // Debug
            return Optional.empty(); // Timeout
        }

        Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
        while (keys.hasNext()) {
            SelectionKey key = keys.next();
            keys.remove();

            if (key.isReadable()) {
                buffer.clear();
                InetSocketAddress sender = (InetSocketAddress) channel.receive(buffer);

                if (sender == null) {
                    System.out.println("Получен null sender."); // Debug
                    continue;
                }

                buffer.flip();
                byte[] receivedData = new byte[buffer.limit()];
                buffer.get(receivedData);

                System.out.println("Получен ответ размером " + receivedData.length + " байт."); // Debug
                CommandResponse response = SerializationUtils.deserialize(receivedData);
                return Optional.of(response);
            }
        }
        System.out.println("Нет читаемых ключей после select > 0."); // Debug
        return Optional.empty();
    }

    public void close() throws IOException {
        if (selector != null) selector.close();
        if (channel != null) channel.close();
    }
}