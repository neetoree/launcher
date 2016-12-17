package org.neetoree.launcher.services;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Provider;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.neetoree.launcher.session.SessionConfig;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Alexander <iamtakingiteasy> Tumin on 2016-12-16.
 */
public class LaunchDownloader implements Runnable {
    private boolean stop;
    private LaunchListener launchListener;

    @Inject
    private TextService textService;

    @Inject
    private FetcherService fetcherService;

    @Inject
    private ConfigService configService;

    @Inject
    private NEEToreeRepository repository;

    @Inject
    private Provider<SessionConfig> sessionConfig;

    public LaunchDownloader(LaunchListener listener) {
        this.launchListener = listener;
    }

    public boolean broadcastFileProgress(double amount) {
        launchListener.fileProgress(amount);
        return stop;
    }

    public void broadcastFileName(String urlstr) {
        launchListener.fileName(urlstr);
    }

    public void taskProgress(double amount) {
        launchListener.taskProgress(amount);
    }

    public void taskName(String name) {
        launchListener.taskName(name);
    }

    @Override
    public void run() {
        taskName(textService.text("get.manifest"));
        taskProgress(-1);

        try {
            JsonObject root = Json.createReader(fetch(repository.getManifestUrl())).readObject();
            JsonObject launcher = root.getJsonObject("launcher");
            JsonObject versions = root.getJsonObject("versions");

            processLauncher(launcher);

            if (!"true".equals(configService.get("inited"))) {
                String vanillaManifest = versions.getString("vanilla");
                String forgeManifest = versions.getString("forge");

                File versionsDir = new File(configService.getGamedir(), "versions");

                File vanillaJson = new File(versionsDir, "vanilla.json");
                download(vanillaManifest, vanillaJson, -1);

                File forgeJson = new File(versionsDir, "forge.json");
                download(forgeManifest, forgeJson, -1);

                List<JsonObject> libraries = new ArrayList<>();
                if (!stop) {
                    processVanilla(libraries, vanillaJson, versionsDir);
                }
                if (!stop) {
                    processForge(libraries, forgeJson);
                }
                if (!stop) {
                    processLibraries(libraries);
                }
            }
            if (!stop) {
                broadcastDone();
                configService.set("inited", "true");
                configService.set("serial", launcher.getString("serial"));
                launch();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void launch() throws IOException {
        Map<String, String> props = new HashMap<>();
        props.put("auth_player_name", sessionConfig.get().getClientId());
        props.put("version_name", configService.get("version"));
        props.put("game_directory", configService.getGamedir().getAbsolutePath());
        props.put("assets_root", new File(configService.getGamedir(), "assets").getAbsolutePath());
        props.put("assets_index_name", configService.get("version"));
        props.put("auth_uuid", sessionConfig.get().getClientId());
        props.put("auth_access_token", sessionConfig.get().getAccessToken());
        props.put("user_properties", "{}");
        props.put("user_type", "mojang");

        StringBuilder libs = new StringBuilder();
        Files.find(
                new File(configService.getGamedir(), "libraries").toPath(),
                Integer.MAX_VALUE,
                (path, attr) -> path.toString().toLowerCase().endsWith(".jar"))
                .forEach((path) -> libs.append(path.toAbsolutePath()).append(File.pathSeparatorChar));

        File versionsDir = new File(configService.getGamedir(), "versions");
        File dest = new File(versionsDir, "vanilla.jar");
        libs.append(dest.getAbsolutePath());

        List<String> commands = new ArrayList<>();
        commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        commands.add("-Xms" + configService.get("minmem", "512") + "M");
        commands.add("-Xmx" + configService.get("maxmem", "1300") + "M");
        commands.add("-Djava.library.path=" + new File(configService.getGamedir(), "natives"));
        commands.add("-cp");
        commands.add(libs.toString());
        commands.add(configService.get("mainclass"));

        String[] split = configService.get("arguments").split("\\s+");
        for (String s : split) {
            if (s.startsWith("${") && s.endsWith("}")) {
                commands.add(props.get(s.substring(2, s.length()-1)));
            } else {
                commands.add(s);
            }
        }

        System.out.println(Joiner.on(' ').join(commands));
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
        while (!stop && process.isAlive()) {
            String line = br.readLine();
            if (line == null) {
                launchListener.terminate();
                return;
            }
            launchListener.updateLog(line);
        }
    }

    private void processLibraries(List<JsonObject> libraries) throws ZipException {
        taskName(textService.text("get.mc.libraries"));
        taskProgress(-1);

        File librariesDir = new File(configService.getGamedir(), "libraries");
        int done = 0;
        for (JsonObject lobj : libraries) {
            if (stop) {
                break;
            }
            String prefix = null;
            String name = lobj.getString("name");
            String classifier = null;
            int size = -1;
            if (lobj.containsKey("downloads")) {
                JsonObject aobj = null;
                if (lobj.containsKey("natives")) {
                    classifier = lobj.getJsonObject("natives").getString(configService.getPlatform());
                    aobj = lobj.getJsonObject("downloads").getJsonObject("classifiers").getJsonObject(classifier);
                } else {
                    aobj = lobj.getJsonObject("downloads").getJsonObject("artifact");
                }
                prefix = repository.getMCLibraries();
                size = aobj.getInt("size");
            } else if (lobj.containsKey("url")) {
                if (lobj.containsKey("classifier")) {
                    classifier = lobj.getString("classifier");
                }
                prefix = lobj.getString("url");
            } else {
                prefix = repository.getMCLibraries();
            }

            String[] split = name.split(":");
            String filename = split[split.length-2] + "-" + split[split.length-1] + (classifier == null ? "" : "-" + classifier) + ".jar";
            String processed = split[0].replace('.', '/') + "/" + name.replaceFirst("^[^:]+:", "").replace(':', '/');
            String url = prefix + "/" + processed + "/" + filename;
            String dest = librariesDir.getAbsolutePath() + File.separator + processed.replace('/', File.separatorChar) + File.separator + filename;
            download(url, new File(dest), size);


            if (lobj.containsKey("natives")) {
                new ZipFile(dest).extractAll(new File(configService.getGamedir(), "natives").getAbsolutePath());
            }
            taskProgress((double)++done / (double)libraries.size());
        }
    }

    private void processForge(List<JsonObject> libraries, File forgeJson) throws FileNotFoundException {
        JsonObject root = Json.createReader(new FileReader(forgeJson)).readObject();
        configService.set("arguments", root.getString("minecraftArguments"));
        configService.set("mainclass", root.getString("mainClass"));

        List<JsonObject> collect = root.getJsonArray("libraries").stream()
                .map((value) -> (JsonObject) value)
                .filter((value) -> !value.containsKey("rules"))
                .collect(Collectors.toList());


        JsonObject remove = collect.remove(0);
        collect.add(Json.createObjectBuilder()
                .add("name", remove.getString("name"))
                .add("url", remove.getString("url"))
                .add("classifier", "universal").build());
        libraries.addAll(collect);
    }

    private void processVanilla(List<JsonObject> libraries, File vanillaJson, File versionsDir) throws FileNotFoundException {
        taskName(textService.text("get.mc.assets"));
        taskProgress(-1);

        JsonObject root = Json.createReader(new FileReader(vanillaJson)).readObject();
        JsonObject client = root.getJsonObject("downloads").getJsonObject("client");
        download(client.getString("url"), new File(versionsDir, "vanilla.jar"), client.getInt("size"));

        configService.set("version",root.getString("assets"));

        String assetsUrl = root.getJsonObject("assetIndex").getString("url");
        File assetsDir = new File(configService.getGamedir(), "assets");
        File assetsFile = new File(new File(assetsDir, "indexes"), assetsUrl.replaceAll(".*/", ""));
        download(assetsUrl, assetsFile, -1);

        int done = 0;
        File objectsDir = new File(assetsDir, "objects");
        JsonObject objects = Json.createReader(new FileReader(assetsFile)).readObject().getJsonObject("objects");
        for (String key : objects.keySet()) {
            if (stop) {
                break;
            }
            JsonObject aobj = objects.getJsonObject(key);
            String hash = aobj.getString("hash");
            File dest = new File(new File(objectsDir, hash.substring(0, 2)), hash);
            download(repository.getMCAsset(hash), dest, aobj.getInt("size"));
            taskProgress((double)++done / (double)objects.size());
        }

        libraries.addAll(root.getJsonArray("libraries").stream()
                .map((value) -> (JsonObject) value)
                .filter((value) -> !value.containsKey("rules"))
                .filter((value) -> !value.getString("name").contains("guava"))
                .collect(Collectors.toList()));
    }

    private void processLauncher(JsonObject launcher) throws IOException {
        if (!Objects.equals(launcher.getString("serial"), configService.get("serial"))) {
            configService.unset("inited");
        }

        if (repository.getVersionName().equals(launcher.getString("version"))) {
            return;
        }

        configService.unset("inited");

        if (launchListener.confirmDownload()) {
            File target = new File(LaunchDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            File tmp = new File(target.getParentFile(), "launcher.tmp");
            download(launcher.getString("url"), tmp, launcher.getInt("size"));
            if (!tmp.renameTo(target)) {
                throw new IllegalStateException();
            }
            String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
            new ProcessBuilder(javaBin, "-jar", target.getAbsolutePath()).start();
            System.exit(0);
        }
    }

    public InputStream fetch(String url) {
        broadcastFileName(url.replaceAll(".*/", ""));
        return fetcherService.fetch(url, this::broadcastFileProgress);
    }

    public void download(String url, File dest, int size) {
        broadcastFileName(url.replaceAll(".*/", ""));
        fetcherService.download(url, dest, size, this::broadcastFileProgress);
    }

    public void stop() {
        this.stop = true;
    }

    public void broadcastDone() {
        launchListener.done(!stop);
    }


    /*
    private final Set<LaunchListener> listeners;

    private boolean stop;
    private String gameVersion;
    private String gameVersionUrl;
    private JsonObject assetsIndex;
    private String assetsId;
    private JsonObject clientJar;
    private JsonArray libraries;
    private String mainClass;
    private String argumetsTemplate;
    private JsonObject forge;

    @Inject
    private Provider<SessionConfig> sessionConfig;

    @Inject
    private FetcherService fetcherService;

    @Inject
    private TextService textService;

    @Inject
    private ConfigService configService;

    public LaunchDownloader(Set<LaunchListener> listeners) {
        this.listeners = listeners;
    }

    @Override
    public void run() {
        stop = false;
        try {
            do {
                getManifest();
                if (stop) {
                    break;
                }
                getVersions();
                if (stop) {
                    break;
                }
                loadVersion();
                if (stop) {
                    break;
                }
                loadAssets();
                if (stop) {
                    break;
                }
                loadClient();
                if (stop) {
                    break;
                }
                loadLibraries();
                if (stop) {
                    break;
                }
                loadForge();
                if (stop) {
                    break;
                }
                //launch();
            } while (false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        broadcastDone();
    }

    private void launch() throws IOException {
        Map<String, String> props = new HashMap<>();
        props.put("auth_player_name", sessionConfig.get().getClientId());
        props.put("version_name", gameVersion);
        props.put("game_directory", configService.getGamedir().getAbsolutePath());
        props.put("assets_root", new File(configService.getGamedir(), "assets").getAbsolutePath());
        props.put("assets_index_name", assetsId);
        props.put("auth_uuid", sessionConfig.get().getClientId());
        props.put("auth_access_token", sessionConfig.get().getAccessToken());
        props.put("user_properties", "{}");
        props.put("user_type", "mojang");

        StringBuilder libs = new StringBuilder();
        Files.find(
                new File(configService.getGamedir(), "libraries").toPath(),
                Integer.MAX_VALUE,
                (path, attr) -> path.toString().toLowerCase().endsWith(".jar"))
                .forEach((path) -> libs.append(path.toAbsolutePath()).append(File.pathSeparatorChar));

        File versionsDir = new File(configService.getGamedir(), "versions");
        File dest = new File(new File(versionsDir, assetsId), assetsId + ".jar");
        libs.append(dest.getAbsolutePath());

        List<String> commands = new ArrayList<>();
        commands.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        commands.add("-Xms512M");
        commands.add("-Xmx1300M");
        commands.add("-Djava.library.path=" + new File(configService.getGamedir(), "natives"));
        commands.add("-cp");
        commands.add(libs.toString());
        commands.add(mainClass);

        String[] split = argumetsTemplate.split("\\s+");
        for (String s : split) {
            if (s.startsWith("${") && s.endsWith("}")) {
                commands.add(props.get(s.substring(2, s.length()-1)));
            } else {
                commands.add(s);
            }
        }

        new ProcessBuilder(commands).start();
        System.exit(0);
    }

    private void loadForge() throws ZipException {
        File versionsDir = new File(configService.getGamedir(), "versions");
        File dest = new File(new File(versionsDir, gameVersion), gameVersion + "-forge.jar");
        download(forge.getString("url"), "forge.jar", dest, forge.getInt("size"));
        ZipFile zip = new ZipFile(dest);
        FileHeader fileHeader = zip.getFileHeader("version.json");
        JsonReader reader = Json.createReader(zip.getInputStream(fileHeader));
        JsonObject root = reader.readObject();
        argumetsTemplate = root.getString("minecraftArguments");
        mainClass = root.getString("mainClass");
        JsonArray forgelibs = root.getJsonArray("libraries");
        for (JsonValue value : forgelibs) {
            JsonObject obj = (JsonObject) value;
            String name = obj.getString("name");

        }
    }

    private void loadLibraries() throws IOException, ZipException {
        taskName(textService.text("get.mc.libraries"));
        taskProgress(-1);
        int done = 0;
        File librariesDir = new File(configService.getGamedir(), "libraries");
        for (JsonValue value : libraries) {
            if (stop) {
                break;
            }
            JsonObject obj = (JsonObject)value;
            if (obj.containsKey("rules")) {
                taskProgress((double)++done / (double)libraries.size());
                continue;
            }
            JsonObject downloads = obj.getJsonObject("downloads");
            JsonObject artifact;
            if (obj.containsKey("natives")) {
                String key = obj.getJsonObject("natives").getString(configService.getPlatform());
                artifact = downloads.getJsonObject("classifiers").getJsonObject(key);
            } else {
                artifact = downloads.getJsonObject("artifact");
            }
            String path = artifact.getString("path");
            File dest = new File(librariesDir.getPath() + File.separator + path.replace('/', File.separatorChar));
            download(artifact.getString("url"), path, dest, artifact.getInt("size"));
            if (obj.containsKey("natives")) {
                new ZipFile(dest).extractAll(new File(configService.getGamedir(), "natives").getAbsolutePath());
            }
            taskProgress((double)++done / (double)libraries.size());
        }
    }

    private void loadClient() {
        taskName(textService.text("get.mc.client"));
        taskProgress(-1);
        File versionsDir = new File(configService.getGamedir(), "versions");
        File dest = new File(new File(versionsDir, assetsId), assetsId + ".jar");
        download(clientJar.getString("url"), "client.jar", dest, clientJar.getInt("size"));
    }

    private void loadAssets() throws FileNotFoundException {
        taskName(textService.text("get.mc.assets"));
        taskProgress(-1);
        File assetsDir = new File(configService.getGamedir(), "assets");
        File assetsIndexFile = new File(new File(assetsDir, "indexes"), assetsId + ".json");
        File objectsDir = new File(assetsDir, "objects");
        download(assetsIndex.getString("url"), "assetsIndex.json", assetsIndexFile, assetsIndex.getInt("size"));
        JsonReader reader = Json.createReader(new FileReader(assetsIndexFile));
        JsonObject objects = reader.readObject().getJsonObject("objects");
        int done = 0;
        for (String key : objects.keySet()) {
            if (stop) {
                break;
            }
            String hash = objects.getJsonObject(key).getString("hash");
            File dest = new File(new File(objectsDir, hash.substring(0, 2)), hash);
            download(NEEToreeRepository.instance().getMCAsset(hash), key, dest, objects.getJsonObject(key).getInt("size"));
            taskProgress((double)++done / (double)objects.size());
        }
    }

    private void loadVersion() throws FileNotFoundException {
        taskName(textService.text("get.mc.index"));
        taskProgress(-1);

        File versionsDir = new File(configService.getGamedir(), "versions");
        File dest = new File(new File(versionsDir, gameVersion), gameVersion + ".json");
        download(gameVersionUrl, "index.json", dest, -1);
        JsonReader reader = Json.createReader(new FileReader(dest));
        JsonObject root = reader.readObject();
        assetsIndex = root.getJsonObject("assetIndex");
        assetsId = root.getString("assets");
        clientJar = root.getJsonObject("downloads").getJsonObject("client");
        libraries = root.getJsonArray("libraries");
        mainClass = root.getString("mainClass");
        argumetsTemplate = root.getString("minecraftArguments");
    }

    private void getManifest() {
        taskName(textService.text("get.manifest"));
        taskProgress(-1);

        JsonReader reader = Json.createReader(fetch(NEEToreeRepository.instance().getManifestUrl()));
        JsonObject root = reader.readObject();
        JsonObject launcher = root.getJsonObject("launcher");
        String launcherVersion = launcher.getString("version");
        String launcherUrl = launcher.getString("url");

        if (!NEEToreeRepository.instance().getVersionName().equals(launcherVersion)) {
            downloadNewLauncher(launcherUrl);
            stop = true;
        }

        JsonObject game = root.getJsonObject("game");
        gameVersion = game.getString("version");
        forge = game.getJsonObject("forge");
    }

    private void downloadNewLauncher(String versionUrl) {
        boolean result = true;
        for (LaunchListener listener : listeners) {
            result = result && listener.confirmDownload();
        }

        if (result) {
            InputStream inputStream = fetch(versionUrl);
            File target = new File(LaunchDownloader.class.getProtectionDomain().getCodeSource().getLocation().getPath());
            File tmp = new File(target.getParentFile(), "launcher.tmp");
            try {
                FileOutputStream fos = new FileOutputStream(tmp);
                ByteStreams.copy(inputStream, fos);
                fos.close();
                if (!tmp.renameTo(target)) {
                    throw new IllegalStateException();
                }
                String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
                final ArrayList<String> command = new ArrayList<>();
                command.add(javaBin);
                command.add("-jar");
                command.add(target.getPath());
                new ProcessBuilder(command).start();
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void getVersions() {
        taskName(textService.text("get.mc.versions"));
        taskProgress(-1);

        JsonReader reader = Json.createReader(fetch(NEEToreeRepository.instance().getMCVersionsManifest()));
        JsonObject root = reader.readObject();
        JsonArray versions = root.getJsonArray("versions");
        JsonObject versionInfo = null;
        for (JsonValue object : versions) {
            String id = ((JsonObject) object).getString("id");
            if (gameVersion.equals(id)) {
                versionInfo = (JsonObject) object;
                break;
            }
        }
        if (versionInfo == null) {
            stop = true;
            return;
        }
        gameVersionUrl = versionInfo.getString("url");
    }

    private InputStream fetch(String url) {
        return fetch(url, url);
    }

    private InputStream fetch(String url, String text) {
        broadcastFileName(text);
        return fetcherService.fetch(url, this::broadcastFileProgress);
    }

    private void download(String url, String name, File dest, int size) {
        if (dest.exists() && dest.length() == size) {
            return;
        }
        try {
            dest.getParentFile().mkdirs();
            FileOutputStream fos = new FileOutputStream(dest);
            InputStream uis = fetch(url, name);
            ByteStreams.copy(uis, fos);
            uis.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void broadcastFileProgress(double amount) {
        for (LaunchListener listener : listeners) {
            listener.fileProgress(amount);
        }
    }

    private void broadcastFileName(String urlstr) {
        for (LaunchListener listener : listeners) {
            listener.fileName(urlstr);
        }
    }

    private void taskProgress(double amount) {
        for (LaunchListener listener : listeners) {
            listener.taskProgress(amount);
        }
    }

    private void taskName(String name) {
        for (LaunchListener listener : listeners) {
            listener.taskName(name);
        }
    }


    private void broadcastDone() {
        for (LaunchListener listener : listeners) {
            listener.done(!stop);
        }
    }

    public void stop() {
        stop = true;
    }
    */
}