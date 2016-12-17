package org.neetoree.launcher.session;

import com.google.inject.Singleton;

import javax.inject.Provider;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class SessionProvider implements Provider<SessionConfig> {
    private SessionConfig sessionConfig = new SessionConfig();

    public synchronized void invalidate(SessionConfig sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    @Override
    public SessionConfig get() {
        return sessionConfig;
    }
}
