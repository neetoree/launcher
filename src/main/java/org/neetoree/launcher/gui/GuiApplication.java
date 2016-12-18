package org.neetoree.launcher.gui;

import com.google.inject.Inject;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.neetoree.launcher.NEEToree;
import org.neetoree.launcher.gui.service.LoaderService;
import org.neetoree.launcher.services.LoginService;
import org.neetoree.launcher.ui.UserInterface;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class GuiApplication extends Application implements UserInterface {
    @Inject
    private LoaderService loader;

    @Inject
    private LoginService loginService;

    public static Application APPLICATION;

    @Override
    public void run(String[] args) throws Exception {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        NEEToree.injector.injectMembers(this);
        Parent main = loader.load("main");
        primaryStage.setScene(new Scene(main));
        primaryStage.show();

        loginService.trylogin();

        APPLICATION = this;
    }
}
