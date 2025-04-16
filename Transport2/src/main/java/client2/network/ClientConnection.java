package client2.network;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ClientConnection {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean isConnected = false;

    private void connect() throws IOException {
        if (!isConnected) {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            isConnected = true;
        }
    }

    public CompletableFuture<List<String>> sendRequestAsync(String request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connect(); // Подключение при первом запросе
                out.println(request);
                List<String> response = new ArrayList<>();
                String line;
                while ((line = in.readLine()) != null && !line.equals("END")) {
                    response.add(line);
                }
                return response;
            } catch (IOException e) {
                isConnected = false;
                throw new RuntimeException("Ошибка подключения: " + e.getMessage());
            }
        });
    }
}