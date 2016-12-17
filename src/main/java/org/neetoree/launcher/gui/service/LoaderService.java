package org.neetoree.launcher.gui.service;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.neetoree.launcher.services.LoginService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ResourceBundle;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class LoaderService {
    @Inject
    private Injector injector;

    @Inject
    @Named("text")
    private ResourceBundle textBundle;

    public Parent load(String res) {
        FXMLLoader loader = new FXMLLoader(LoginService.class.getResource("/gui/" + res + ".fxml"), textBundle);
        try {
            Parent parent = loader.load();
            injector.injectMembers(loader.getController());
            try {
                Method method = loader.getController().getClass().getDeclaredMethod("postInit");
                if (method != null) {
                    method.invoke(loader.getController());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return parent;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
