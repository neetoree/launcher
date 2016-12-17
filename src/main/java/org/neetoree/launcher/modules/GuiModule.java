package org.neetoree.launcher.modules;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import org.neetoree.launcher.gui.GuiApplication;
import org.neetoree.launcher.ui.UserInterface;

import java.util.ResourceBundle;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class GuiModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(UserInterface.class).to(GuiApplication.class);
        bind(ResourceBundle.class).annotatedWith(Names.named("text")).toInstance(ResourceBundle.getBundle("gui/text"));
    }
}
