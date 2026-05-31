package com.gym;

import com.gym.ui.MainApplication;
import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GymApplication {

    public static void main(String[] args) {
        Application.launch(MainApplication.class, args);
    }
}
