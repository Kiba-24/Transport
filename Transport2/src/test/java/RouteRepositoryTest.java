
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import server2.RouteRepository;
import server2.Schedule;

import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RouteRepositoryTest {

    private RouteRepository routeRepository;

    @BeforeEach
    void setUp() {
        routeRepository = new RouteRepository();
    }


    @Test
    void testFindComplexRoutes() {
        // Тест на поиск сложных маршрутов с пересадками
        List<List<Schedule>> routes = routeRepository.findComplexRoutes("Москва", "Санкт-Петербург", 2);
        assertNotNull(routes, "Список маршрутов не должен быть null");
        assertFalse(routes.isEmpty(), "Список маршрутов не должен быть пустым");
    }
}