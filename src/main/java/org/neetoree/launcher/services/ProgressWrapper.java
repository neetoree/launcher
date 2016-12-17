package org.neetoree.launcher.services;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-16.
 */
public class ProgressWrapper extends InputStream {
    private final int total;
    private final InputStream inputStream;
    private final ProgressListener listener;
    private final HttpURLConnection connection;
    private int count = 0;
    private boolean stop;

    public ProgressWrapper(HttpURLConnection connection, ProgressListener listener) throws IOException {
        this.connection = connection;
        this.total = connection.getContentLength();
        this.inputStream = connection.getInputStream();
        this.listener = listener;
    }

    private int update(int upd) {
        count += upd;
        stop = listener.update((double)count/(double)total);
        if (stop) {
            return -1;
        }
        return upd;
    }

    @Override
    public int read() throws IOException {
        if (stop) {
            return -1;
        }
        int read = inputStream.read();
        update(1);
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (stop) {
            return -1;
        }
        int read = inputStream.read(b);
        return update(read);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (stop) {
            return -1;
        }
        int read = inputStream.read(b, off, len);
        return update(read);
    }

    @Override
    public long skip(long n) throws IOException {
        if (stop) {
            return 0;
        }
        return update((int) inputStream.skip(n));
    }

    @Override
    public int available() throws IOException {
        if (stop) {
            return 0;
        }
        return inputStream.available();
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    public HttpURLConnection getConnection() {
        return connection;
    }
}
