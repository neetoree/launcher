package org.neetoree.launcher.modules;

import com.google.inject.AbstractModule;
import org.neetoree.launcher.os.OsSpecific;
import org.neetoree.launcher.os.OsSpecificProvider;
import org.neetoree.launcher.session.SessionConfig;
import org.neetoree.launcher.session.SessionProvider;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class MainModule extends AbstractModule {
    private final boolean gui;

    public MainModule(boolean gui) {
        this.gui = gui;
    }

    @Override
    protected void configure() {
        bind(OsSpecific.class).toProvider(OsSpecificProvider.class);
        bind(SessionConfig.class).toProvider(SessionProvider.class);
        if (gui) {
            install(new GuiModule());
        }
    }
}
