package org.neetoree.launcher.gui.controller;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import org.neetoree.launcher.gui.service.LoaderService;
import org.neetoree.launcher.gui.service.NavigationDestination;
import org.neetoree.launcher.gui.service.NavigationService;
import org.neetoree.launcher.services.LoginListener;
import org.neetoree.launcher.services.LoginService;
import org.neetoree.launcher.session.SessionConfig;

import javax.inject.Provider;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class MainController implements LoginListener {
    public HBox logout;
    public Label welcome;
    public ProgressIndicator indicator;
    public BorderPane content;

    @Inject
    private LoginService loginService;

    @Inject
    private NavigationService navigationService;

    @Inject
    private LoaderService loaderService;

    @Inject
    private Provider<SessionConfig> session;

    public void postInit() {
        Parent login = loaderService.load("login");
        Parent tabs = loaderService.load("tabs");

        loginService.subscribe(this);
        navigationService.subscribe(destination -> {
            indicator.setVisible(false);
            Platform.runLater(() -> {
                switch (destination) {
                    case LOGIN:
                        content.setCenter(login);
                        break;
                    case SIGNUP:
                        break;
                    case TABS:
                        content.setCenter(tabs);
                        break;
                    case LAUNCH:
                        break;
                }
            });
        });
    }

    public void logout(ActionEvent actionEvent) {
        loginService.logout();
    }

    @Override
    public void login() {
        Platform.runLater(() -> {
            logout.setVisible(true);
            welcome.setText(session.get().getClientId());
        });
        navigationService.navigate(NavigationDestination.TABS);
    }

    @Override
    public void logout() {
        Platform.runLater(() -> logout.setVisible(false));
        navigationService.navigate(NavigationDestination.LOGIN);
    }

    @Override
    public void error(String message) {

    }
}
