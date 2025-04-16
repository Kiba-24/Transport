package client2;

import client2.controllers.SearchController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import server2.RouteRepository;

public class MainApp extends Application {
    private RouteRepository routeRepository = new RouteRepository();

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/search.fxml"));
        loader.setControllerFactory(param -> new SearchController(routeRepository));
        Parent root = loader.load();
        primaryStage.setTitle("Transport Marketplace");
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}