package server2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RouteRepository {
    // Универсальный класс-хранилище
    public static class GenericStore<T> {
        private final List<T> items = new ArrayList<>();
        public void add(T item) { items.add(item); }
        public List<T> getItems() { return items; }
    }

    // Вложенный универсальный класс для чтения CSV-файлов
    public static class CSVReader<T> {
        public List<T> read(String filePath, Function<String[], T> mapper, int skipLines) {
            List<T> result = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
                result = br.lines()
                        .skip(skipLines)
                        .map(line -> line.split(","))
                        .map(mapper)
                        .collect(Collectors.toList());
            } catch (IOException e) {
                System.err.println("Ошибка чтения файла " + filePath + ": " + e.getMessage());
            }
            return result;
        }
    }

    // Хранилища данных
    private final GenericStore<City> cities = new GenericStore<>();
    private final GenericStore<Carrier> carriers = new GenericStore<>();
    private final GenericStore<Route> routes = new GenericStore<>();
    private final GenericStore<Schedule> schedules = new GenericStore<>();

    private static final int SEARCH_TIMEOUT_SECONDS = 10;

    public RouteRepository() {
        loadCities();
        loadCarriers();
        loadRoutes();
        loadSchedules();
    }

    private void loadCities() {
        String file = "src/main/resources/cities.csv";
        CSVReader<City> reader = new CSVReader<>();
        List<City> list = reader.read(file, data -> {
            try {
                return new City(
                        Integer.parseInt(data[0]),
                        data[1],
                        data[2],
                        data[3]
                );
            } catch (Exception e) {
                System.err.println("Ошибка парсинга города: " + Arrays.toString(data));
                return null;
            }
        }, 1);
        list.stream().filter(Objects::nonNull).forEach(cities::add);
        System.out.println("Загружено городов: " + cities.getItems().size());
    }

    private void loadCarriers() {
        String file = "src/main/resources/carriers.csv";
        CSVReader<Carrier> reader = new CSVReader<>();
        List<Carrier> list = reader.read(file, data -> {
            try {
                return new Carrier(
                        Integer.parseInt(data[0]),
                        data[1],
                        data[2],
                        data[3]
                );
            } catch (Exception e) {
                System.err.println("Ошибка парсинга перевозчика: " + Arrays.toString(data));
                return null;
            }
        }, 1);
        list.stream().filter(Objects::nonNull).forEach(carriers::add);
    }

    private void loadRoutes() {
        String file = "src/main/resources/routes.csv";
        CSVReader<Route> reader = new CSVReader<>();
        List<Route> list = reader.read(file, data -> {
            try {
                return new Route(
                        Integer.parseInt(data[0]),
                        findCarrierById(Integer.parseInt(data[1])),
                        findCityById(Integer.parseInt(data[2])),
                        findCityById(Integer.parseInt(data[3])),
                        Integer.parseInt(data[4]),
                        Double.parseDouble(data[5])
                );
            } catch (Exception e) {
                System.err.println("Ошибка парсинга маршрута: " + Arrays.toString(data));
                return null;
            }
        }, 1);
        list.stream().filter(Objects::nonNull).forEach(routes::add);
    }

    private void loadSchedules() {
        String file = "src/main/resources/schedule.csv";
        CSVReader<Schedule> reader = new CSVReader<>();
        List<Schedule> list = reader.read(file, data -> {
            try {
                int routeId = Integer.parseInt(data[0]);
                Route route = findRouteById(routeId);
                if (route == null) {
                    System.err.println("Ошибка: Маршрут ID=" + routeId + " не найден в schedule.csv");
                    return null;
                }
                return new Schedule(
                        route,
                        LocalDateTime.parse(data[1], DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        LocalDateTime.parse(data[2], DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        Integer.parseInt(data[3])
                );
            } catch (Exception e) {
                System.err.println("Ошибка парсинга расписания: " + Arrays.toString(data) + " " + e.getMessage());
                return null;
            }
        }, 1);
        list.stream().filter(Objects::nonNull).forEach(schedules::add);
    }

    public Route findRouteById(int id) {
        return routes.getItems().stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Carrier findCarrierById(int id) {
        return carriers.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Перевозчик не найден: " + id));
    }

    public City findCityById(int id) {
        return cities.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Город не найден: " + id));
    }

    public List<Schedule> getAllSchedules() {
        return new ArrayList<>(schedules.getItems());
    }

    public List<List<Schedule>> findComplexRoutes(String fromCityName, String toCityName, int maxTransfers) {
        City start = cities.getItems().stream()
                .filter(c -> c.getName().equalsIgnoreCase(fromCityName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Город отправления не найден"));
        City end = cities.getItems().stream()
                .filter(c -> c.getName().equalsIgnoreCase(toCityName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Город назначения не найден"));
        List<List<Schedule>> results = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        findRoutesRecursive(start, end, null, new ArrayList<>(), results, 0, maxTransfers, startTime);
        return results;
    }

    private void findRoutesRecursive(City currentCity,
                                     City targetCity,
                                     LocalDateTime lastArrivalTime,
                                     List<Schedule> currentPath,
                                     List<List<Schedule>> results,
                                     int transfers,
                                     int maxTransfers,
                                     long startTime) {
        if ((System.currentTimeMillis() - startTime) > SEARCH_TIMEOUT_SECONDS * 1000L) {
            throw new RuntimeException("Превышено время поиска");
        }
        if (transfers > maxTransfers) return;
        if (currentCity.equals(targetCity)) {
            results.add(new ArrayList<>(currentPath));
            return;
        }
        List<Schedule> nextFlights = schedules.getItems().stream()
                .filter(s -> s.getRoute().getDepartureCity().equals(currentCity))
                .filter(s -> lastArrivalTime == null || s.getDepartureTime().isAfter(lastArrivalTime.plusHours(1)))
                .collect(Collectors.toList());
        for (Schedule flight : nextFlights) {
            if (currentPath.stream().anyMatch(p -> p.getRoute().equals(flight.getRoute()))) continue;
            currentPath.add(flight);
            findRoutesRecursive(flight.getRoute().getArrivalCity(), targetCity, flight.getArrivalTime(),
                    currentPath, results, transfers + 1, maxTransfers, startTime);
            currentPath.remove(currentPath.size() - 1);
        }
    }

    public void saveSchedules() {
        String file = "src/main/resources/schedule.csv";
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            bw.write("route_id,departure_time,arrival_time,available_seats\n");
            for (Schedule schedule : schedules.getItems()) {
                bw.write(String.format("%d,%s,%s,%d%n",
                        schedule.getRoute().getId(),
                        schedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        schedule.getArrivalTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        schedule.getAvailableSeats()));
            }
        } catch (IOException e) {
            System.err.println("Ошибка сохранения расписаний: " + e.getMessage());
        }
    }

    public Schedule findScheduleByRoute(int routeId, LocalDateTime departureTime) {
        return schedules.getItems().stream()
                .filter(s -> s.getRoute().getId() == routeId && s.getDepartureTime().equals(departureTime))
                .findFirst()
                .orElse(null);
    }
}
