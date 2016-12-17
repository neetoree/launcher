package org.neetoree.launcher.services;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public interface LoginListener {
    void login();
    void logout();
    void error(String message);
}
