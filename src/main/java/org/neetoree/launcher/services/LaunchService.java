package org.neetoree.launcher.services;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class LaunchService {
    @Inject
    private Injector injector;

    public LaunchDownloader launch(LaunchListener launchListener) {
        LaunchDownloader launchDownloader = new LaunchDownloader(launchListener);
        injector.injectMembers(launchDownloader);
        Thread thread = new Thread(launchDownloader);
        thread.setDaemon(true);
        thread.start();
        return launchDownloader;
    }
}
