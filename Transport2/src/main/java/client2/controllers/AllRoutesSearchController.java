package client2.controllers;

import client2.network.ClientConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import server2.Route;
import server2.RouteRepository;
import server2.Schedule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AllRoutesSearchController {
    private final RouteRepository routeRepository = new RouteRepository();
    private final ClientConnection clientConnection = new ClientConnection();

    @FXML private ListView<String> allRoutesList;
    @FXML private ProgressIndicator loadingIndicator;
    @FXML private Button backButton;

    @FXML
    private void initialize() {
        allRoutesList.getItems().clear();
        allRoutesList.setPlaceholder(new Label("Здесь будут отображаться маршруты"));

        allRoutesList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setWrapText(true);
            }
        });

        loadAllRoutes();
    }

    private void loadAllRoutes() {
        if (loadingIndicator != null) loadingIndicator.setVisible(true);

        // Получаем все расписания напрямую из репозитория
        List<Schedule> allSchedules = routeRepository.getAllSchedules();

        Platform.runLater(() -> {
            if (loadingIndicator != null) loadingIndicator.setVisible(false);

            if (allSchedules.isEmpty()) {
                allRoutesList.setPlaceholder(new Label("Маршруты не найдены"));
                return;
            }

            List<String> items = allSchedules.stream()
                    .map(s -> String.format(
                            "%s → %s | %s - %s | %s | Мест: %d | Цена: %.2f руб.",
                            s.getRoute().getDepartureCity().getName(),
                            s.getRoute().getArrivalCity().getName(),
                            s.getDepartureTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            s.getArrivalTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                            s.getRoute().getTransportType(),
                            s.getAvailableSeats(),
                            s.getRoute().getBasePrice()
                    ))
                    .collect(Collectors.toList());

            allRoutesList.getItems().setAll(items);
        });
    }

    @FXML
    private void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(text);
        alert.showAndWait();
    }

    // Вспомогательный метод для парсинга расписаний (аналогичный тому, что в SearchController)
    public List<Schedule> parseSchedules(List<String> response) {
        List<Schedule> schedules = new ArrayList<>();
        if (response == null || response.isEmpty()) return schedules;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        for (String line : response) {
            if (line.equals("END") || line.startsWith("ERROR")) continue;

            try {
                String[] data = line.split("\\|");
                if (data.length < 7) {
                    System.err.println("Неверный формат данных: " + line);
                    continue;
                }

                int routeId = Integer.parseInt(data[0]);
                LocalDateTime departureTime = LocalDateTime.parse(data[4], formatter);
                LocalDateTime arrivalTime = LocalDateTime.parse(data[5], formatter);
                int availableSeats = Integer.parseInt(data[6]);

                Route route = routeRepository.findRouteById(routeId);
                if (route == null) {
                    System.err.println("Маршрут не найден: " + routeId);
                    continue;
                }

                Schedule schedule = new Schedule(route, departureTime, arrivalTime, availableSeats);
                schedules.add(schedule);
            } catch (Exception e) {
                System.err.println("Ошибка парсинга строки '" + line + "': " + e.getMessage());
            }
        }
        return schedules;
    }
}