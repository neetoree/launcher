package org.neetoree.launcher.services;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-10.
 */
@Singleton
public class NEEToreeRepository extends DefaultApi20 {
    private String version;

    public NEEToreeRepository() {
        Properties properties = new Properties();
        try {
            properties.load(NEEToreeRepository.class.getResourceAsStream("/system/version.properties"));
            version = properties.getProperty("version");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getAccessTokenEndpoint() {
        return "https://neetoree.org/oauth/token";
    }

    @Override
    protected String getAuthorizationBaseUrl() {
        return "https://neetoree.org/oauth/authorize";
    }

    public String getNewstUrl() {
        return "https://neetoree.org/news";
    }

    public String getManifestUrl() {
        return "https://neetoree.org/manifest";
    }

    public String getVersionName() {
        return version;
    }

    public String getMCVersionsManifest() {
        return "https://launchermeta.mojang.com/mc/game/version_manifest.json";
    }

    public String getMCAsset(String hash) {
        return "http://resources.download.minecraft.net/" + hash.substring(0, 2) + "/" + hash;
    }

    public String getMCLibraries() {
        return "https://libraries.minecraft.net";
    }
}
