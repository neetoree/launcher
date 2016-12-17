package org.neetoree.launcher.os;

import java.io.File;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public interface OsSpecific {
    boolean current();
    String name();
    File configdir(String name);
    default String architecure() {
        return System.getProperty("sun.arch.data.model");
    }
}
