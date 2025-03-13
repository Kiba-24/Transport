package client2.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import server2.Route;

public class BookingController {
    @FXML private Label routeInfoLabel;

    public void initializeWithData(Route route) {
        routeInfoLabel.setText(route.toString());
    }

    @FXML
    private void handleConfirmBooking() {
        // Логика бронирования
    }

    @FXML
    private void handleCancel() {
        // Закрытие окна
        routeInfoLabel.getScene().getWindow().hide();
    }
}