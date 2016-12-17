package org.neetoree.launcher.services;

import com.google.common.io.ByteStreams;
import com.google.inject.Singleton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class FetcherService {
    public ProgressWrapper fetch(String url, ProgressListener listener) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
            return new ProgressWrapper(connection, listener);
        } catch (Exception e) {
            return null;
        }
    }

    public void download(String url, File dest, int size, ProgressListener listener) {
        ProgressWrapper uis = null;
        if (size == -1) {
            try {
                uis = fetch(url, listener);
                size = uis.getConnection().getContentLength();
            } catch (Exception ignore) {
                return;
            }
        }

        if (dest.exists() && dest.length() == size) {
            if (uis != null) {
                try {
                    uis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return;
        }

        if (uis == null) {
            uis = fetch(url, listener);
        }

        try {
            dest.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(dest);
            ByteStreams.copy(uis, fos);
            uis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
