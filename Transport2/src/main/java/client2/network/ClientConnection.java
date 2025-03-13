package client2.network;

import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ClientConnection {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 12345;

    public List<String> sendRequest(String request) {
        List<String> response = new ArrayList<>();
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // Отправляем запрос на сервер
            out.println(request);

            // Читаем ответ от сервера
            String line;
            while ((line = in.readLine()) != null && !line.equals("END")) {
                response.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }
}