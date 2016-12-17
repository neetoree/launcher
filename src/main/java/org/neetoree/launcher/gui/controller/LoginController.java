package org.neetoree.launcher.gui.controller;

import com.google.inject.Inject;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.neetoree.launcher.gui.service.NavigationDestination;
import org.neetoree.launcher.gui.service.NavigationService;
import org.neetoree.launcher.services.LoginListener;
import org.neetoree.launcher.services.LoginService;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class LoginController implements LoginListener {
    public Label error;
    public TextField username;
    public PasswordField password;
    public CheckBox remember;

    @Inject
    private LoginService loginService;

    @Inject
    private NavigationService navigationService;

    public void postInit() {
        loginService.subscribe(this);
    }

    @Override
    public void login() {
        navigationService.navigate(NavigationDestination.TABS);
    }

    @Override
    public void logout() {
        navigationService.navigate(NavigationDestination.LOGIN);
    }

    @Override
    public void error(String message) {
        Platform.runLater(() -> error.setText(message));
    }

    public void sendLogin(ActionEvent actionEvent) {
        Platform.runLater(() -> {
            error.setText(null);
            loginService.login(username.getText(), password.getText(), remember.isSelected());
        });

    }
}
