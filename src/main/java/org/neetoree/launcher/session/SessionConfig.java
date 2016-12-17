package org.neetoree.launcher.session;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
public class SessionConfig {
    private final String accessToken;
    private final String clientId;
    private final boolean loggedin;

    public SessionConfig() {
        this.loggedin = false;
        this.accessToken = null;
        this.clientId = null;
    }

    public SessionConfig(String accessToken, String clientId) {
        this.loggedin = true;
        this.accessToken = accessToken;
        this.clientId = clientId;
    }

    public boolean isLoggedin() {
        return loggedin;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientId() {
        return clientId;
    }
}