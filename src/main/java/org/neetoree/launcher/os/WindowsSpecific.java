package org.neetoree.launcher.os;

import java.io.File;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class WindowsSpecific implements OsSpecific {
    @Override
    public boolean current() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    @Override
    public String name() {
        return "windows";
    }

    @Override
    public File configdir(String name) {
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            return new File(appData, name);
        } else {
            return new File(System.getProperty("user.home"), name);
        }
    }
}
