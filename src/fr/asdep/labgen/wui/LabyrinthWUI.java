package fr.asdep.labgen.wui;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import fr.asdep.labgen.core.*;
import fr.asdep.labgen.exporter.ImageExporter;
import fr.asdep.labgen.exporter.SchematicExporter;
import fr.asdep.labgen.exporter.WorldExporter;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.mc.ThemeLoader;
import fr.asdep.labgen.utils.ProgressBar;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LabyrinthWUI {

    private static final int DEFAULT_PORT = 8080;
    private static final Map<String, ProgressInfo> progressMap = new ConcurrentHashMap<>();
    private static final Map<String, GeneratedFile> generatedFiles = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    static class GeneratedFile {
        byte[] content;
        String contentType;
        String filename;

        GeneratedFile(byte[] content, String contentType, String filename) {
            this.content = content;
            this.contentType = contentType;
            this.filename = filename;
        }
    }

    static class ProgressInfo {
        String task;
        int current;
        int total;
        long lastUpdate;

        ProgressInfo(String task, int current, int total) {
            this.task = task;
            this.current = current;
            this.total = total;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        ProgressBar.setListener((task, current, total) -> {
            progressMap.put("latest", new ProgressInfo(task, current, total));
        });

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new RootHandler());
        server.createContext("/style.css", new StaticFileHandler("style.css", "text/css"));
        server.createContext("/script.js", new StaticFileHandler("script.js", "application/javascript"));
        server.createContext("/generate", new GenerateHandler());
        server.createContext("/progress", new ProgressHandler());
        server.createContext("/download", new DownloadHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("Serveur Web démarré sur http://localhost:" + port);
        server.start();
    }

    static class ProgressHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            ProgressInfo info = progressMap.get("latest");
            boolean finished = info != null && info.task != null && info.task.toLowerCase().contains("terminé");
            String json = info == null ? "{}" : String.format(
                "{\"task\":\"%s\", \"current\":%d, \"total\":%d, \"finished\":%b}",
                info.task, info.current, info.total, generatedFiles.containsKey("latest")
            );
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            GeneratedFile file = generatedFiles.get("latest");
            if (file == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", file.contentType);
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + file.filename + "\"");
            exchange.sendResponseHeaders(200, file.content.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(file.content);
            }
            generatedFiles.remove("latest");
        }
    }


    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder formData = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            formData.append(line);
        }
        return formData.toString();
    }

    private static GenerationConfig parseConfig(Map<String, String> params) {
        GenerationConfig genConfig = new GenerationConfig();
        genConfig.setGameVersion(params.getOrDefault("version", "1.12.2"));
        genConfig.setWidth(Integer.parseInt(params.getOrDefault("width", "10")));
        genConfig.setDepth(Integer.parseInt(params.getOrDefault("depth", "10")));
        genConfig.setHeight(Integer.parseInt(params.getOrDefault("height", "3")));
        genConfig.setCorridorWidth(Integer.parseInt(params.getOrDefault("corridorWidth", "1")));
        genConfig.setWallWidth(Integer.parseInt(params.getOrDefault("wallWidth", "1")));
        genConfig.setErosion(Float.parseFloat(params.getOrDefault("erosion", "0.0")));
        genConfig.setCeilingEnabled("true".equals(params.get("ceiling")));
        genConfig.setBaseY(Integer.parseInt(params.getOrDefault("baseY", "64")));
        genConfig.setAlgorithm(MazeAlgorithm.fromName(params.getOrDefault("algorithm", "recursive-backtracker")));

        String themeName = params.getOrDefault("theme", "stone");
        try {
            genConfig.setTheme(ThemeLoader.loadTheme(genConfig.getGameVersion(), themeName));
        } catch (Exception e) {
            genConfig.setTheme(Theme.getDefault());
        }

        String[] roomXs = params.getOrDefault("room_x", "").split(",");
        String[] roomZs = params.getOrDefault("room_z", "").split(",");
        String[] roomWs = params.getOrDefault("room_w", "").split(",");
        String[] roomDs = params.getOrDefault("room_d", "").split(",");
        String[] roomEs = params.getOrDefault("room_e", "").split(",");
        for (int i = 0; i < roomXs.length; i++) {
            if (roomXs[i].isEmpty()) continue;
            genConfig.addRoom(new Room(
                Integer.parseInt(roomXs[i]),
                Integer.parseInt(roomZs[i]),
                Integer.parseInt(roomWs[i]),
                Integer.parseInt(roomDs[i]),
                Integer.parseInt(roomEs[i])
            ));
        }

        String[] ezXs = params.getOrDefault("ez_x", "").split(",");
        String[] ezZs = params.getOrDefault("ez_z", "").split(",");
        String[] ezWs = params.getOrDefault("ez_w", "").split(",");
        String[] ezDs = params.getOrDefault("ez_d", "").split(",");
        String[] ezFs = params.getOrDefault("ez_f", "").split(",");
        for (int i = 0; i < ezXs.length; i++) {
            if (ezXs[i].isEmpty()) continue;
            genConfig.addErosionZone(new ErosionZone(
                Integer.parseInt(ezXs[i]),
                Integer.parseInt(ezZs[i]),
                Integer.parseInt(ezWs[i]),
                Integer.parseInt(ezDs[i]),
                Float.parseFloat(ezFs[i])
            ));
        }
        return genConfig;
    }

    static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = loadResource("index.html");

            // Remplacer les placeholders dynamiques
            StringBuilder versionsHtml = new StringBuilder();
            for (String v : getAvailableVersions()) {
                versionsHtml.append("<option value='").append(v).append("'>").append(v).append("</option>");
            }
            html = html.replace("{{VERSIONS}}", versionsHtml.toString());

            StringBuilder algorithmsHtml = new StringBuilder();
            for (String alg : MazeAlgorithm.listAlgorithms()) {
                algorithmsHtml.append("<option value='").append(alg).append("'>").append(alg).append("</option>");
            }
            html = html.replace("{{ALGORITHMS}}", algorithmsHtml.toString());

            // Themes JSON
            StringBuilder themesJson = new StringBuilder("{");
            String[] versions = getAvailableVersions();
            for (int i = 0; i < versions.length; i++) {
                String v = versions[i];
                themesJson.append("'").append(v).append("': [");
                List<String> themeList = ThemeLoader.listThemes(v);
                for (int j = 0; j < themeList.size(); j++) {
                    themesJson.append("'").append(themeList.get(j)).append("'");
                    if (j < themeList.size() - 1) themesJson.append(",");
                }
                themesJson.append("]");
                if (i < versions.length - 1) themesJson.append(",");
            }
            themesJson.append("}");
            html = html.replace("{{THEMES_JSON}}", themesJson.toString());

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class StaticFileHandler implements HttpHandler {
        private final String fileName;
        private final String contentType;

        StaticFileHandler(String fileName, String contentType) {
            this.fileName = fileName;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String content = loadResource(fileName);
            if (content == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String loadResource(String fileName) {
        try (InputStream is = LabyrinthWUI.class.getResourceAsStream("/resources/wui/" + fileName)) {
            InputStream source = is;
            if (source == null) {
                File file = new File("src/resources/wui/" + fileName);
                if (file.exists()) {
                    source = new FileInputStream(file);
                } else {
                    return null;
                }
            }
            try (ByteArrayOutputStream result = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = source.read(buffer)) != -1) {
                    result.write(buffer, 0, length);
                }
                return result.toString(StandardCharsets.UTF_8.name());
            } finally {
                if (is == null && source != null) {
                    source.close();
                }
            }
        } catch (IOException e) {
            return null;
        }
    }

    static class GenerateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            Map<String, String> params = parseFormData(readRequestBody(exchange));
            progressMap.clear();
            generatedFiles.remove("latest");

            executor.submit(() -> {
                try {
                    GenerationConfig genConfig = parseConfig(params);
                    MazeGenerator generator = new MazeGenerator(genConfig);
                    generator.generate();

                    String exportType = params.getOrDefault("exportType", "schematic");
                    String baseName = params.getOrDefault("schematicName", "labyrinth");
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    String contentType;
                    String filename;

                    if ("world".equals(exportType)) {
                        filename = baseName + ".zip";
                        contentType = "application/zip";
                        WorldExporter.exportToZip(generator, baseName, baos);
                    } else if ("image".equals(exportType)) {
                        filename = baseName + ".png";
                        contentType = "image/png";
                        ImageExporter.export(generator, baos);
                    } else {
                        filename = baseName + ".schematic";
                        contentType = "application/octet-stream";
                        SchematicExporter.export(generator, baos);
                    }

                    generatedFiles.put("latest", new GeneratedFile(baos.toByteArray(), contentType, filename));
                    progressMap.put("latest", new ProgressInfo("Terminé", 100, 100));

                } catch (Exception e) {
                    e.printStackTrace();
                    progressMap.put("latest", new ProgressInfo("Erreur: " + e.getMessage(), 0, 100));
                }
            });

            String response = "{\"status\":\"started\"}";
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> params = new HashMap<>();
        String[] pairs = formData.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
                params.put(key, value);
            }
        }
        return params;
    }


    private static String[] getAvailableVersions() {
        File versionsDir = new File("versions");
        if (!versionsDir.exists() || !versionsDir.isDirectory()) {
            return new String[]{"1.12.2"};
        }

        File[] folders = versionsDir.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            return new String[]{"1.12.2"};
        }

        List<String> versionList = new ArrayList<>();
        for (File folder : folders) {
            versionList.add(folder.getName());
        }

        return versionList.toArray(new String[0]);
    }
}
