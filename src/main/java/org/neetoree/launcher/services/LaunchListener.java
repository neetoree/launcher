package org.neetoree.launcher.services;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-16.
 */
public interface LaunchListener {
    void taskName(String name);
    void taskProgress(double amount);
    void fileName(String urlstr);
    void fileProgress(double amount);
    void done(boolean complete);
    boolean confirmDownload();
    void updateLog(String line);
    void terminate();
}
