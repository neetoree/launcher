package org.neetoree.launcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import org.neetoree.launcher.modules.MainModule;
import org.neetoree.launcher.ui.UserInterface;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class NEEToree {
    public static Injector injector;

    public static void main(String[] args) throws Exception {
        injector = Guice.createInjector(Stage.DEVELOPMENT, new MainModule(args.length == 0));
        UserInterface instance = injector.getInstance(UserInterface.class);
        instance.run(args);
    }
}
