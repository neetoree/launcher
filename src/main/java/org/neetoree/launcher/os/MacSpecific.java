package org.neetoree.launcher.os;

import java.io.File;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class MacSpecific implements OsSpecific {
    @Override
    public boolean current() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    @Override
    public String name() {
        return "osx";
    }

    @Override
    public File configdir(String name) {
        return new File(System.getProperty("user.home"), "Library/Application Support/" + name);
    }
}
