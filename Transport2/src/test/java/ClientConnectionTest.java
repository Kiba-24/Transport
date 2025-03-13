
import client2.network.ClientConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ClientConnectionTest {

    private ClientConnection clientConnection;

    @BeforeEach
    void setUp() {
        clientConnection = new ClientConnection();
    }

    @Test
    void testSendRequest() {
        // Тест на отправку запроса и получение ответа
        List<String> response = clientConnection.sendRequest("SEARCH_ALL|Москва|Санкт-Петербург|mix");
        assertNotNull(response, "Ответ от сервера не должен быть null");
        assertFalse(response.isEmpty(), "Ответ от сервера не должен быть пустым");
    }
}