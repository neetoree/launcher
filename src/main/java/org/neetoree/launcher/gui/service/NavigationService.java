package org.neetoree.launcher.gui.service;

import com.google.inject.Singleton;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class NavigationService {
    private Set<NavigationListener> listeners = new HashSet<>();

    public void subscribe(NavigationListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(NavigationListener listener) {
        listeners.remove(listener);
    }

    public void navigate(NavigationDestination destination) {
        for (NavigationListener listener : listeners) {
            listener.navigate(destination);
        }
    }
}
