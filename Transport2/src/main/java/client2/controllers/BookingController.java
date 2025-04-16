package client2.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import client2.network.ClientConnection;
import server2.Schedule;
import javafx.stage.Stage;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;

public class BookingController {
    @FXML private Label routeInfoLabel;
    @FXML private Label seatsInfoLabel;
    @FXML private Button confirmButton;
    private Schedule currentSchedule;
    private final ClientConnection clientConnection = new ClientConnection();

    // Инициализация для прямого маршрута
    public void initializeWithData(Schedule schedule) {
        this.currentSchedule = schedule;
        updateScheduleInfo();
        checkAvailability();
    }

    private void updateScheduleInfo() {
        long travelMinutes = Duration.between(currentSchedule.getDepartureTime(), currentSchedule.getArrivalTime()).toMinutes();
        String info = String.format("%s → %s (%s)%nВремя отправления: %s, прибытия: %s%nВ пути: %d мин%nЦена: %.2f руб",
                currentSchedule.getRoute().getDepartureCity().getName(),
                currentSchedule.getRoute().getArrivalCity().getName(),
                currentSchedule.getRoute().getTransportType(),
                currentSchedule.getDepartureTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                currentSchedule.getArrivalTime().format(DateTimeFormatter.ofPattern("HH:mm")),
                travelMinutes,
                currentSchedule.getRoute().getBasePrice());
        routeInfoLabel.setText(info);
    }

    private void checkAvailability() {
        confirmButton.setDisable(true);
        seatsInfoLabel.setText("Проверка доступности мест...");
        String request = "CHECK_SEATS|" + currentSchedule.getRoute().getId() + "|" +
                currentSchedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        clientConnection.sendRequestAsync(request)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response == null || response.isEmpty()) {
                        seatsInfoLabel.setText("Ошибка подключения");
                        return;
                    }
                    if (response.get(0).startsWith("SEATS|")) {
                        int availableSeats = Integer.parseInt(response.get(0).split("\\|")[1]);
                        seatsInfoLabel.setText("Доступно мест: " + availableSeats);
                        confirmButton.setDisable(availableSeats <= 0);
                    } else {
                        seatsInfoLabel.setText("Ошибка проверки мест");
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        seatsInfoLabel.setText("Ошибка: " + e.getCause().getMessage());
                        confirmButton.setDisable(true);
                    });
                    return null;
                });
    }

    @FXML
    private void handleConfirmBooking() {
        confirmButton.setDisable(true);
        seatsInfoLabel.setText("Идет бронирование...");
        String request = "BOOK|" + currentSchedule.getRoute().getId() + "|" +
                currentSchedule.getDepartureTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        clientConnection.sendRequestAsync(request)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response != null && response.toString().contains("SUCCESS")) {
                        showAlert(Alert.AlertType.INFORMATION, "Успех", "Бронирование завершено!");
                        // Обновляем информацию в списке маршрутов
                        SearchController.refreshSearchResults();
                        // Добавляем текущее расписание в список забронированных
                        SearchController.addBookedSchedule(currentSchedule);
                        // Закрываем окно бронирования
                        Stage stage = (Stage) confirmButton.getScene().getWindow();
                        stage.close();
                    } else {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Мест нет");
                        confirmButton.setDisable(false);
                    }
                }))
                .exceptionally(e -> {
                    Platform.runLater(() -> {
                        showAlert(Alert.AlertType.ERROR, "Ошибка", "Ошибка бронирования: " + e.getMessage());
                        confirmButton.setDisable(false);
                    });
                    return null;
                });
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) routeInfoLabel.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
