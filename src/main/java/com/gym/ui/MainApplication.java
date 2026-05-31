package com.gym.ui;

import com.gym.SpringContext;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApplication extends Application {

    private ConfigurableApplicationContext springContext;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(com.gym.GymApplication.class)
                .headless(false)
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        // Use Spring context for dependency injection in controllers
        loader.setControllerFactory(springContext::getBean);
        
        Parent root = loader.load();
        Scene scene = new Scene(root, 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/dark-theme.css").toExternalForm());
        
        primaryStage.setTitle("Gym Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(800);
        primaryStage.show();
    }

    @Override
    public void stop() {
        springContext.close();
        Platform.exit();
    }
}
