package org.neetoree.launcher.os;

import java.io.File;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class LinuxSpecific implements OsSpecific {
    @Override
    public boolean current() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("unix") || os.contains("linux");
    }

    @Override
    public String name() {
        return "linux";
    }

    @Override
    public File configdir(String name) {
        return new File(System.getProperty("user.home"), name);
    }
}
