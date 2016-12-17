package org.neetoree.launcher.services;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.neetoree.launcher.os.OsSpecific;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-15.
 */
@Singleton
public class ConfigService {
    private final String platform;
    private final File directory;
    private final File gamedir;
    private final String architecture;
    private final Properties properties = new Properties();
    private final File propfile;

    @Inject
    public ConfigService(OsSpecific specific) {
        this.platform = specific.name();
        this.directory = specific.configdir(".neetoree");
        this.gamedir = new File(directory, "game");
        this.architecture = specific.architecure();

        if (!directory.exists()) {
            if (!directory.mkdirs()) {
                throw new IllegalStateException();
            }
        }

        try {
            propfile = new File(directory, "neetoree.properties");
            if (!propfile.exists()) {
                if (!propfile.createNewFile()) {
                    throw new IllegalStateException();
                }
                init();
            }
            properties.load(new FileReader(propfile));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void init() {
        properties.put("minmem", "512");
        properties.put("maxmem", "1300");
        save();
    }

    public File getGamedir() {
        return gamedir;
    }

    public File getDirectory() {
        return directory;
    }

    public String getArchitecture() {
        return architecture;
    }

    private void save() {
        try {
            properties.store(new FileWriter(propfile), "NEEToree properties");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void set(String key, String value) {
        properties.setProperty(key, value);
        save();
    }

    public void unset(String key) {
        properties.remove(key);
        save();
    }

    public String get(String key, String def) {
        String property = properties.getProperty(key);
        if (Strings.isNullOrEmpty(property)) {
            return def;
        }
        return property;
    }

    public String get(String key) {
        return properties.getProperty(key);
    }

    public String getPlatform() {
        return platform;
    }
}
