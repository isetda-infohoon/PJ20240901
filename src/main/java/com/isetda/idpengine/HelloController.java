package com.isetda.idpengine;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.util.List;

public class HelloController {
    private ConfigLoader configLoader = ConfigLoader.getInstance();
    List<String> BUCKET_NAMES;
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        BUCKET_NAMES = configLoader.getBucketNames();
        System.out.println(configLoader.getKeyFilePath());
        System.out.println(configLoader.getProjectId());
        System.out.println(configLoader.getBucketNames());
        System.out.println(BUCKET_NAMES);

    }
}