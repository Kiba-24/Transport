package client2.controllers;

import client2.network.ClientConnection;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import server2.Route;
import server2.RouteRepository;
import server2.Schedule;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SearchController {
    private RouteRepository routeRepository;
    private final ClientConnection clientConnection = new ClientConnection();
    // Списки найденных прямых маршрутов
    private List<Schedule> currentSchedules = new ArrayList<>();
    // Результаты сложного поиска (каждый элемент — список сегментов)
    private List<List<Schedule>> complexRoutesResult = new ArrayList<>();
    // Флаг типа поиска: прямой или сложный
    private boolean isComplexSearch = false;

    // Список забронированных расписаний
    private List<Schedule> bookedSchedules = new ArrayList<>();

    private static SearchController instance;

    public SearchController(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
        instance = this;
    }

    public SearchController() {
        instance = this;
    }

    // Элементы UI для поиска
    @FXML private TextField departureField;
    @FXML private TextField arrivalField;
    @FXML private ComboBox<String> transportTypeComboBox;
    @FXML private ListView<String> routesList;
    @FXML private ComboBox<String> hourComboBox;
    @FXML private ComboBox<String> minuteComboBox;
    @FXML private DatePicker datePicker;
    @FXML private TextField maxPriceField;
    @FXML private Spinner<Integer> transfersSpinner;
    @FXML private ProgressIndicator loadingIndicator;

    // UI для отображения забронированных мест
    @FXML private ListView<String> bookedList;
    @FXML private Button cancelBookingButton;
    // Кнопка для перехода в окно AllRoutesSearch
    @FXML private Button openAllRoutesButton;
    // Кнопка возврата из окна AllRoutesSearch (при наличии)
    @FXML private Button backButton;

    @FXML
    private void initialize() {
        transportTypeComboBox.getItems().addAll("Авиа", "Ж/Д", "Автобус", "Любой");
        for (int i = 0; i < 24; i++) {
            hourComboBox.getItems().add(String.format("%02d", i));
        }
        minuteComboBox.getItems().addAll("00", "15", "30", "45");
        hourComboBox.getSelectionModel().selectFirst();
        minuteComboBox.getSelectionModel().selectFirst();
        transfersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 0));
        if (loadingIndicator != null) loadingIndicator.setVisible(false);
        routesList.getItems().clear();
        routesList.setPlaceholder(new Label("Здесь будут отображаться маршруты"));
        bookedList.getItems().clear();
        bookedList.setPlaceholder(new Label("Здесь будут отображаться забронированные места"));

        // Используем lambda для cell factory с переносом строк для длинных маршрутов
        routesList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setWrapText(true);
            }
        });
    }

    // Поиск прямых маршрутов
    @FXML
    private void handleSearchClosest() {
        isComplexSearch = false;
        if (!validateInput()) return;
        String departure = departureField.getText().trim();
        String arrival = arrivalField.getText().trim();
        LocalDate selectedDate = datePicker.getValue();
        String hour = hourComboBox.getValue();
        String minute = minuteComboBox.getValue();
        LocalDateTime dateTime = LocalDateTime.parse(
                selectedDate + " " + hour + ":" + minute,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        );
        String transportType = transportTypeComboBox.getValue().equals("Любой") ? "mix" : transportTypeComboBox.getValue();
        String request = "SEARCH_CLOSEST|" + departure + "|" + arrival + "|" +
                dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + transportType;
        routesList.setPlaceholder(new Label("Идет поиск маршрутов..."));
        if (loadingIndicator != null) loadingIndicator.setVisible(true);
        clientConnection.sendRequestAsync(request)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (loadingIndicator != null) loadingIndicator.setVisible(false);
                    currentSchedules = parseSchedules(response);
                    displaySchedules(currentSchedules);
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка поиска: " + e.getMessage());
                        if (loadingIndicator != null) loadingIndicator.setVisible(false);
                    });
                    return null;
                });
    }

    // Поиск сложных маршрутов (с пересадками)
    @FXML
    private void handleSearchComplex() {
        isComplexSearch = true;
        if (!validateInput()) return;
        String departure = departureField.getText().trim();
        String arrival = arrivalField.getText().trim();
        int maxTransfers = transfersSpinner.getValue();
        complexRoutesResult = routeRepository.findComplexRoutes(departure, arrival, maxTransfers);
        List<String> items = complexRoutesResult.stream()
                .map(routeSegments -> routeSegments.stream()
                        .map(Schedule::getRouteInfoWithSeats)
                        .collect(Collectors.joining(" -> ")))
                .collect(Collectors.toList());
        routesList.getItems().setAll(items);
        if (items.isEmpty()) {
            routesList.setPlaceholder(new Label("Маршруты не найдены"));
        } else {
            routesList.setPlaceholder(null);
        }
    }

    // Парсинг ответа сервера в список Schedule
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

    // Отображение найденных маршрутов в ListView
    private void displaySchedules(List<Schedule> schedules) {
        Platform.runLater(() -> {
            List<String> items = schedules.stream()
                    .map(Schedule::getRouteInfoWithSeats)
                    .collect(Collectors.toList());
            routesList.getItems().setAll(items);
            if (items.isEmpty()) {
                routesList.setPlaceholder(new Label("Маршруты не найдены"));
            } else {
                routesList.setPlaceholder(null);
            }
        });
    }

    // Обработка выбора маршрута (прямого или сложного)
    @FXML
    private void handleRouteSelection() {
        int index = routesList.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        if (!isComplexSearch) {
            Schedule selectedSchedule = currentSchedules.stream()
                    .filter(s -> s.getRouteInfoWithSeats().equals(routesList.getSelectionModel().getSelectedItem()))
                    .findFirst().orElse(null);
            if (selectedSchedule != null) {
                openBookingWindow(selectedSchedule);
            } else {
                showAlert(Alert.AlertType.WARNING, "Ошибка", "Маршрут не найден.");
            }
        } else {
            List<Schedule> complexRoute = complexRoutesResult.get(index);
            openComplexBookingWindow(complexRoute);
        }
    }

    // Открытие окна бронирования для прямого маршрута
    private void openBookingWindow(Schedule schedule) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/booking.fxml"));
            Parent root = loader.load();
            BookingController controller = loader.getController();
            controller.initializeWithData(schedule);
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Бронирование");
            stage.show();
            System.out.println("Открыто окно бронирования для: " + schedule.getRouteInfoWithSeats());
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Окно бронирования недоступно.");
            e.printStackTrace();
        }
    }

    // Открытие окна бронирования для сложного маршрута (бронь всех сегментов)
    private void openComplexBookingWindow(List<Schedule> segments) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Подтверждение");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Вы уверены, что хотите забронировать выбранный сложный маршрут?");
        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                bookSegmentsSequentially(segments, 0);
            }
        });
    }

    // Рекурсивное бронирование сегментов сложного маршрута
    private void bookSegmentsSequentially(List<Schedule> segments, int index) {
        if (index >= segments.size()) {
            showAlert(Alert.AlertType.INFORMATION, "Успех", "Бронирование сложного маршрута завершено!");
            refreshSearchResults();
            segments.forEach(s -> addBookedSchedule(s));
            return;
        }
        Schedule s = segments.get(index);
        String request = "BOOK|" + s.getRoute().getId() + "|" +
                s.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        clientConnection.sendRequestAsync(request).thenAccept(response -> Platform.runLater(() -> {
            if (response != null && response.toString().contains("SUCCESS")) {
                // Уменьшаем доступные места для каждого сегмента локально
                s.setAvailableSeats(s.getAvailableSeats() - 1);
                bookSegmentsSequentially(segments, index + 1);
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось забронировать сегмент: " + s.getRouteInfoWithSeats());
            }
        })).exceptionally(e -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка бронирования: " + e.getMessage()));
            return null;
        });
    }

    // Обработка отмены брони для выбранного забронированного маршрута
    @FXML
    private void handleCancelBooking() {
        int index = bookedList.getSelectionModel().getSelectedIndex();
        if (index < 0) return;
        Schedule schedule = bookedSchedules.get(index);
        String request = "CANCEL|" + schedule.getRoute().getId() + "|" +
                schedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        clientConnection.sendRequestAsync(request).thenAccept(response -> Platform.runLater(() -> {
            if (response != null && response.toString().contains("SUCCESS")) {
                // Локальное обновление: увеличиваем количество мест на 1
                schedule.setAvailableSeats(schedule.getAvailableSeats() + 1);
                showAlert(Alert.AlertType.INFORMATION, "Отмена", "Бронь отменена");
                bookedSchedules.remove(index);
                updateBookedList();
                refreshSearchResults();
            } else {
                showAlert(Alert.AlertType.ERROR, "Ошибка", "Не удалось отменить бронь");
            }
        })).exceptionally(e -> {
            Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка отмены бронирования: " + e.getMessage()));
            return null;
        });
    }

    // Добавление забронированного маршрута в список забронированных
    public static void addBookedSchedule(Schedule schedule) {
        if (instance != null) {
            instance.bookedSchedules.add(schedule);
            instance.updateBookedList();
        }
    }

    private void updateBookedList() {
        Platform.runLater(() -> {
            List<String> items = bookedSchedules.stream()
                    .map(Schedule::getRouteInfoWithSeats)
                    .collect(Collectors.toList());
            bookedList.getItems().setAll(items);
            if (items.isEmpty()) {
                bookedList.setPlaceholder(new Label("Здесь будут отображаться забронированные места"));
            } else {
                bookedList.setPlaceholder(null);
            }
        });
    }

    private boolean validateInput() {
        return validateInputBasic() &&
                datePicker.getValue() != null &&
                hourComboBox.getValue() != null &&
                minuteComboBox.getValue() != null;
    }

    private boolean validateInputBasic() {
        if (departureField.getText().trim().isEmpty() ||
                arrivalField.getText().trim().isEmpty() ||
                transportTypeComboBox.getValue() == null) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Заполните все поля");
            return false;
        }
        return true;
    }

    private void showAlert(Alert.AlertType type, String title, String text) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(text);
        alert.showAndWait();
    }

    // Обновление списка маршрутов после бронирования/отмены
    public static void refreshSearchResults() {
        if (instance != null) {
            instance.handleSearchClosest();
        }
    }

    // Метод открытия окна AllRoutesSearch (в новой вкладке/окне)
    @FXML
    private void openAllRoutesSearchWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/allRoutesSearch.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Все доступные маршруты");
            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Ошибка", "Открыть окно всех маршрутов не удалось.");
            e.printStackTrace();
        }
    }
}
