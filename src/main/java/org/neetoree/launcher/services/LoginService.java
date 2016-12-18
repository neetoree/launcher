package org.neetoree.launcher.services;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.neetoree.launcher.session.SessionConfig;
import org.neetoree.launcher.session.SessionProvider;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class LoginService {
    public static final String CLIENT_ID = "client_id";
    public static final String REFRESH_TOKEN = "refresh_token";

    private static final Set<LoginListener> listeners = new HashSet<>();

    @Inject
    private ConfigService configService;

    @Inject
    private SessionProvider sessionProvider;

    @Inject
    private NEEToreeRepository repository;

    @Inject
    private TextService textService;

    public LoginService() throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ssl = SSLContext.getInstance("SSL");
        ssl.init(null, new TrustManager[]{new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
    }

    public void subscribe(LoginListener listener) {
        listeners.add(listener);
    }

    public void unsubscribe(LoginListener listener) {
        listeners.remove(listener);
    }

    private void broadcast(LoginState state) {
        for (LoginListener listener : listeners) {
            switch (state) {
                case UNLOGINNED:
                    listener.logout();
                    break;
                case LOGGEDIN:
                    listener.login();
                    break;
            }

        }
    }
    private void broadcast(String message) {
        for (LoginListener listener : listeners) {
            listener.error(message);
        }
    }

    public boolean trylogin() {
        String clientId = configService.get(CLIENT_ID);
        String refreshToken = configService.get(REFRESH_TOKEN);

        if (clientId == null || refreshToken == null) {
            broadcast(LoginState.UNLOGINNED);
            return false;
        }

        OAuth20Service service = new ServiceBuilder()
                .apiKey(clientId)
                .apiSecret(clientId)
                .build(repository);

        try {
            OAuth2AccessToken token = service.refreshAccessToken(refreshToken);
            sessionProvider.invalidate(new SessionConfig(token.getAccessToken(), clientId));
            configService.set(REFRESH_TOKEN, token.getRefreshToken());
            broadcast(LoginState.LOGGEDIN);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            broadcast(LoginState.UNLOGINNED);
            return false;
        }
    }

    public void logout() {
        configService.unset(CLIENT_ID);
        configService.unset(REFRESH_TOKEN);
        broadcast(LoginState.UNLOGINNED);
    }

    public boolean login(String username, String password, boolean remember) {
        try {
            OAuth20Service service = new ServiceBuilder()
                    .apiKey(username)
                    .apiSecret(password)
                    .build(repository);

            OAuth2AccessToken token = service.getAccessTokenPasswordGrant(username, password);
            sessionProvider.invalidate(new SessionConfig(token.getAccessToken(), username));
            if (remember) {
                configService.set(CLIENT_ID, username);
                configService.set(REFRESH_TOKEN, token.getRefreshToken());
            }
            broadcast(LoginState.LOGGEDIN);
        } catch (Exception e) {
            broadcast(textService.text("login.failure"));
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
