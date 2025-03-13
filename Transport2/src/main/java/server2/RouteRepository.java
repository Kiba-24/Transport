package server2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class RouteRepository {
    private final List<City> cities = new ArrayList<>();
    private final List<Carrier> carriers = new ArrayList<>();
    private final List<Route> routes = new ArrayList<>();
    private final List<Schedule> schedules = new ArrayList<>();
    private static final int SEARCH_TIMEOUT_SECONDS = 10;
    public RouteRepository() {
        loadCities();
        loadCarriers();
        loadRoutes();
        loadSchedules();
    }

    // region Загрузка данных
    private void loadCities() {
        String file = "src/main/resources/cities.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            br.lines().skip(1).forEach(line -> {
                String[] data = line.split(",");
                cities.add(new City(
                        Integer.parseInt(data[0]),
                        data[1],
                        data[2],
                        data[3]
                ));
            });
        } catch (IOException e) {
            System.err.println("Ошибка загрузки городов: " + e.getMessage());
        }
    }

    private void loadCarriers() {
        String file = "src/main/resources/carriers.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            br.lines().skip(1).forEach(line -> {
                String[] data = line.split(",");
                carriers.add(new Carrier(
                        Integer.parseInt(data[0]),
                        data[1],
                        data[2],
                        data[3]
                ));
            });
        } catch (IOException e) {
            System.err.println("Ошибка загрузки перевозчиков: " + e.getMessage());
        }
    }

    private void loadRoutes() {
        String file = "src/main/resources/routes.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            br.lines().skip(1).forEach(line -> {
                String[] data = line.split(",");
                routes.add(new Route(
                        Integer.parseInt(data[0]),
                        findCarrierById(Integer.parseInt(data[1])),
                        findCityById(Integer.parseInt(data[2])),
                        findCityById(Integer.parseInt(data[3])),
                        Integer.parseInt(data[4]),
                        Double.parseDouble(data[5])
                ));
            });
        } catch (IOException | NumberFormatException e) {
            System.err.println("Ошибка загрузки маршрутов: " + e.getMessage());
        }
    }

    private void loadSchedules() {
        String file = "src/main/resources/schedule.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            br.lines().skip(1).forEach(line -> {
                String[] data = line.split(",");
                schedules.add(new Schedule(
                        findRouteById(Integer.parseInt(data[0])),
                        LocalDateTime.parse(data[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        LocalDateTime.parse(data[2], DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        Integer.parseInt(data[3])
                ));
            });
        } catch (IOException | NumberFormatException e) {
            System.err.println("Ошибка загрузки расписаний: " + e.getMessage());
        }
    }
    // endregion

    // region Поисковые методы
    public Carrier findCarrierById(int id) {
        return carriers.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Перевозчик не найден: " + id));
    }

    public City findCityById(int id) {
        return cities.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Город не найден: " + id));
    }

    public Route findRouteById(int id) {
        return routes.stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Маршрут не найден: " + id));
    }

    public List<Schedule> findSchedules(String fromCity, String toCity, LocalDateTime date) {
        return schedules.stream()
                .filter(s -> s.getDepartureTime().toLocalDate().equals(date.toLocalDate()))
                .filter(s -> s.getRoute().getDepartureCity().getName().equalsIgnoreCase(fromCity))
                .filter(s -> s.getRoute().getArrivalCity().getName().equalsIgnoreCase(toCity))
                .collect(Collectors.toList());
    }
    // endregion

    // region Геттеры
    public List<City> getAllCities() {
        return new ArrayList<>(cities);
    }

    public List<Carrier> getAllCarriers() {
        return new ArrayList<>(carriers);
    }

    public List<Route> getAllRoutes() {
        return new ArrayList<>(routes);
    }

    public List<Schedule> getAllSchedules() {
        return new ArrayList<>(schedules);
    }

    private void findRoutesRecursive(City currentCity,
                                     City targetCity,
                                     LocalDateTime lastArrivalTime,
                                     List<Schedule> currentPath,
                                     List<List<Schedule>> results,
                                     int transfers,
                                     int maxTransfers,
                                     long startTime) {

        // Проверка таймаута
        if ((System.currentTimeMillis() - startTime) > SEARCH_TIMEOUT_SECONDS * 1000L) {
            throw new RuntimeException("Превышено время поиска");
        }

        // Оригинальная логика метода
        if (transfers > maxTransfers) return;

        if (currentCity.equals(targetCity)) {
            results.add(new ArrayList<>(currentPath));
            return;
        }

        List<Schedule> nextFlights = schedules.stream()
                .filter(s -> s.getRoute().getDepartureCity().equals(currentCity))
                .filter(s -> lastArrivalTime == null ||
                        s.getDepartureTime().isAfter(lastArrivalTime.plusHours(1)))
                .collect(Collectors.toList());

        for (Schedule flight : nextFlights) {
            if (currentPath.stream().anyMatch(p -> p.getRoute().equals(flight.getRoute()))) continue;

            currentPath.add(flight);
            findRoutesRecursive(
                    flight.getRoute().getArrivalCity(),
                    targetCity,
                    flight.getArrivalTime(),
                    currentPath,
                    results,
                    transfers + 1,
                    maxTransfers,
                    startTime
            );
            currentPath.remove(currentPath.size() - 1);
        }
    }

    // Обновленный метод findComplexRoutes
    public List<List<Schedule>> findComplexRoutes(String fromCityName,
                                                  String toCityName,
                                                  int maxTransfers) {
        City start = cities.stream()
                .filter(c -> c.getName().equalsIgnoreCase(fromCityName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Город отправления не найден"));

        City end = cities.stream()
                .filter(c -> c.getName().equalsIgnoreCase(toCityName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Город назначения не найден"));

        List<List<Schedule>> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try {
            findRoutesRecursive(start, end, null, new ArrayList<>(), results, 0, maxTransfers, startTime);
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
        }

        return results;
    }
    public Schedule findScheduleByRoute(Route route) {
        return schedules.stream()
                .filter(s -> s.getRoute().equals(route))
                .findFirst()
                .orElse(null);
    }
    public Schedule findScheduleByRouteId(int routeId) {
        return schedules.stream()
                .filter(s -> s.getRoute().getId() == routeId)
                .findFirst()
                .orElse(null);
    }
    public void saveSchedules() {
        String file = "src/main/resources/schedule.csv";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            // Записываем заголовок (если нужен)
            bw.write("routeId,departureTime,arrivalTime,availableSeats,serviceClass\n");

            for (Schedule schedule : schedules) {
                bw.write(String.format("%d,%s,%s,%d,%s\n",
                        schedule.getRoute().getId(),
                        schedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        schedule.getArrivalTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        schedule.getAvailableSeats()));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения расписаний: " + e.getMessage());
        }
    }

}