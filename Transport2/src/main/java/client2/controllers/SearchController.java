package client2.controllers;

import client2.network.ClientConnection;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import server2.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class SearchController {
    private final RouteRepository routeRepository;
    private final ClientConnection clientConnection = new ClientConnection();

    // Внедряем зависимость через конструктор
    public SearchController(RouteRepository routeRepository) {
        this.routeRepository = routeRepository;
    }

    @FXML private TextField departureField;
    @FXML private TextField arrivalField;
    @FXML private ComboBox<String> transportTypeComboBox;
    @FXML private ListView<String> routesList;
    @FXML private ComboBox<String> hourComboBox;
    @FXML private ComboBox<String> minuteComboBox;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> transfersSpinner;

    @FXML
    private void initialize() {
        transportTypeComboBox.getItems().addAll("Авиа", "Ж/Д", "Автобус", "Любой");
        // Заполняем часы (00-23)
        for (int i = 0; i < 24; i++) {
            hourComboBox.getItems().add(String.format("%02d", i));
        }
        transfersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 3, 0));

        // Заполняем минуты (00, 15, 30, 45)
        minuteComboBox.getItems().addAll("00", "15", "30", "45");

        // Устанавливаем значения по умолчанию
        hourComboBox.getSelectionModel().selectFirst();
        minuteComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    private void handleSearchClosest() {
        if (!validateInput()) {
            return;
        }
        String departure = departureField.getText();
        String arrival = arrivalField.getText();

        // Получаем выбранную дату и время
        LocalDate selectedDate = datePicker.getValue();
        String hour = hourComboBox.getValue();
        String minute = minuteComboBox.getValue();

        // Формируем строку даты и времени в формате "yyyy-MM-dd HH:mm"
        String desiredDateTime = selectedDate + " " + hour + ":" + minute;

        // Преобразуем дату и время в формат ISO_LOCAL_DATE_TIME
        LocalDateTime dateTime = LocalDateTime.parse(desiredDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        String formattedDateTime = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // Получаем тип транспорта
        String transportType = transportTypeComboBox.getValue();
        if (transportType.equals("Любой")) {
            transportType = "mix";
        }

        // Формируем запрос для сервера
        String request = "SEARCH_CLOSEST|" + departure + "|" + arrival + "|" + formattedDateTime + "|" + transportType;

        // Отправляем запрос на сервер и получаем ответ
        List<String> response = clientConnection.sendRequest(request);

        // Обрабатываем ответ
        List<Schedule> schedules = parseSchedules(response);
        displaySchedules(schedules);
    }

    @FXML
    private void handleSearchAll() {
        if (!validateInput()) {
            return;
        }
        String departure = departureField.getText();
        String arrival = arrivalField.getText();

        // Получаем тип транспорта
        String transportType = transportTypeComboBox.getValue();
        if (transportType.equals("Любой")) {
            transportType = "mix";
        }

        // Формируем запрос для сервера
        String request = "SEARCH_ALL|" + departure + "|" + arrival + "|" + transportType;

        // Отправляем запрос на сервер и получаем ответ
        List<String> response = clientConnection.sendRequest(request);

        // Обрабатываем ответ
        List<Schedule> schedules = parseSchedules(response);
        displaySchedules(schedules);
    }

    /**
     * Преобразует ответ от сервера в список объектов Schedule.
     */
    private List<Schedule> parseSchedules(List<String> response) {
        List<Schedule> schedules = new ArrayList<>();
        for (String line : response) {
            String[] data = line.split("\\|");
            int routeId = Integer.parseInt(data[0]);
            String transportType = data[1];
            String departureCity = data[2];
            String arrivalCity = data[3];
            LocalDateTime departureTime = LocalDateTime.parse(data[4], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            LocalDateTime arrivalTime = LocalDateTime.parse(data[5], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            int availableSeats = Integer.parseInt(data[6]);

            // Создаем объект Schedule
            Route route = new Route(
                    routeId,
                    new Carrier(0, "", transportType, ""), // Заглушка для Carrier
                    new City(0, departureCity, "", ""),    // Заглушка для City
                    new City(0, arrivalCity, "", ""),      // Заглушка для City
                    0, 0.0                                // Заглушка для duration и price
            );
            Schedule schedule = new Schedule(route, departureTime, arrivalTime, availableSeats);
            schedules.add(schedule);
        }
        return schedules;
    }

    /**
     * Отображает список расписаний в ListView.
     */
    private void displaySchedules(List<Schedule> schedules) {
        routesList.getItems().clear();
        for (Schedule schedule : schedules) {
            String scheduleInfo = String.format(
                    "%s → %s (%s, %s - %s, мест: %d)",
                    schedule.getRoute().getDepartureCity().getName(),
                    schedule.getRoute().getArrivalCity().getName(),
                    schedule.getRoute().getTransportType(),
                    schedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    schedule.getArrivalTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    schedule.getAvailableSeats()
            );
            routesList.getItems().add(scheduleInfo);
        }
    }

    private boolean validateInput() {
        if (departureField.getText().isEmpty() || arrivalField.getText().isEmpty()) {
            System.out.println("Ошибка: Заполните все поля");
            return false;
        }

        if (datePicker.getValue() == null) {
            System.out.println("Ошибка: Выберите дату");
            return false;
        }

        if (hourComboBox.getValue() == null || minuteComboBox.getValue() == null) {
            System.out.println("Ошибка: Укажите время");
            return false;
        }
        return true;
    }

    @FXML
    private void handleComplexSearch() {
        if (!validateInput()) return;

        List<List<Schedule>> routes = routeRepository.findComplexRoutes(
                departureField.getText(),
                arrivalField.getText(),
                transfersSpinner.getValue()
        );

        displayComplexRoutes(routes);
    }

    private void displayComplexRoutes(List<List<Schedule>> routes) {
        routesList.getItems().clear();
        for (List<Schedule> routeChain : routes) {
            StringJoiner sj = new StringJoiner(" → ", "[", "]");
            for (Schedule s : routeChain) {
                sj.add(s.getRoute().getDepartureCity().getName() +
                        " (" + s.getDepartureTime().format(DateTimeFormatter.ISO_TIME) + ")");
            }
            routesList.getItems().add(sj.toString());
        }
    }
}