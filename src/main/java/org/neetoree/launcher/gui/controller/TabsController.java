package org.neetoree.launcher.gui.controller;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import org.neetoree.launcher.gui.service.NavigationService;
import org.neetoree.launcher.services.*;

import java.util.Optional;
import java.util.concurrent.FutureTask;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class TabsController implements LaunchListener {
    public Button launch;
    public Button stop;
    public Tab news;
    public Tab configs;
    public Tab logs;
    public WebView web;
    public Label fileName;
    public ProgressBar fileProg;
    public Label totalName;
    public ProgressBar totalProg;
    public TabPane tabs;
    public VBox progress;
    public TextField minmem;
    public TextField maxmem;
    public TextArea logarea;

    private LaunchDownloader process;

    @Inject
    private LaunchService launchService;

    @Inject
    private NavigationService navigationService;

    @Inject
    private TextService textService;

    @Inject
    private NEEToreeRepository repository;

    @Inject
    private ConfigService configService;

    public void initialize() {
        minmem.textProperty().addListener((observable, oldValue, newValue) -> {
            minmem.setText(newValue.replaceAll("\\D", ""));
            configService.set("minmem", minmem.getText());
        });
        maxmem.textProperty().addListener((observable, oldValue, newValue) -> {
            maxmem.setText(newValue.replaceAll("\\D", ""));
            configService.set("maxmem", maxmem.getText());
        });
    }

    public void postInit() {
        web.getEngine().load(repository.getNewstUrl());
        minmem.setText(configService.get("minmem", "512"));
        maxmem.setText(configService.get("maxmem", "1300"));
        minmem.textProperty().addListener((observable, oldValue, newValue) -> configService.set("minmem", newValue));
        maxmem.textProperty().addListener((observable, oldValue, newValue) -> configService.set("maxmem", newValue));
    }

    public void launch(ActionEvent actionEvent) {
        if (process == null) {
            logarea.clear();
            process = launchService.launch(this);
            Platform.runLater(() -> {
                launch.setVisible(false);
                stop.setDisable(false);
                stop.setVisible(true);
                tabs.setVisible(false);
                progress.setVisible(true);
            });
        }
    }

    public void stop(ActionEvent actionEvent) {
        if (process != null) {
            stop.setDisable(true);
            process.stop();
        }
    }

    @Override
    public void taskName(String name) {
        Platform.runLater(() -> totalName.setText(name));
    }

    @Override
    public void taskProgress(double amount) {
        Platform.runLater(() -> totalProg.setProgress(amount));
    }

    @Override
    public void fileName(String urlstr) {
        Platform.runLater(() -> fileName.setText(urlstr));
    }

    @Override
    public void fileProgress(double amount) {
        Platform.runLater(() -> fileProg.setProgress(amount));
    }

    @Override
    public void done(boolean complete) {
        process = null;
        Platform.runLater(() -> {
            launch.setVisible(true);
            stop.setVisible(false);
            tabs.setVisible(true);
            progress.setVisible(false);
            if (complete) {
                tabs.getSelectionModel().select(logs);
            }
        });
    }

    @Override
    public boolean confirmDownload() {
        FutureTask<Boolean> result = new FutureTask<Boolean>(() -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            Platform.runLater(() -> confirm.setContentText(textService.text("selfupdate.message")));
            Optional<ButtonType> types = confirm.showAndWait();
            return types.isPresent() && types.get() == ButtonType.OK;
        });

        Platform.runLater(result);

        try {
            return result.get();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void updateLog(String line) {
        Platform.runLater(() -> logarea.appendText(line + "\n"));
    }

    @Override
    public void terminate() {
        Platform.runLater(() -> tabs.getSelectionModel().select(news));
    }
}
