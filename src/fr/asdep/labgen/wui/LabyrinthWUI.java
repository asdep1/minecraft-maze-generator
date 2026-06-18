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
            String response = getHtmlInterface();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
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

    private static String getHtmlInterface() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html lang='fr'><head><title>Minecraft Labyrinth Generator - WUI</title>");
        html.append("<style>");
        html.append("body{font-family:'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;margin:20px;background:#f4f4f9;color:#333;}");
        html.append(".container{max-width:900px;margin:auto;background:white;padding:30px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}");
        html.append("h1{color:#2c3e50;text-align:center;margin-top:0;}");
        html.append(".section{margin-bottom:25px;padding-bottom:15px;border-bottom:1px solid #eee;}");
        html.append(".section h2{font-size:1.2em;color:#34495e;margin-bottom:15px;}");
        html.append(".form-group{margin-bottom:12px;display:flex;align-items:center;}");
        html.append("label{display:inline-block;width:180px;font-weight:600;}");
        html.append("input[type='number'], input[type='text'], select{padding:8px;border:1px solid #ddd;border-radius:4px;width:200px;}");
        html.append("button{padding:10px 20px;background:#3498db;color:white;border:none;border-radius:4px;cursor:pointer;font-weight:600;}");
        html.append("button:hover{background:#2980b9;}");
        html.append(".list-container{margin-top:10px;background:#f9f9f9;padding:10px;border-radius:4px;border:1px solid #ddd;}");
        html.append(".list-item{display:flex;justify-content:space-between;align-items:center;padding:5px;border-bottom:1px solid #eee;}");
        html.append(".remove-btn{background:#e74c3c;padding:5px 10px;font-size:0.8em;}");
        html.append(".remove-btn:hover{background:#c0392b;}");
        html.append("#size-display{background:#ecf0f1;padding:10px;text-align:center;font-weight:bold;margin-bottom:20px;border-radius:4px;}");
        html.append(".progress-container{display:none;margin-top:20px;padding:15px;background:#fff;border:1px solid #ddd;border-radius:4px;}");
        html.append(".progress-bar{height:20px;background:#eee;border-radius:10px;overflow:hidden;margin-top:10px;}");
        html.append(".progress-fill{height:100%;background:#2ecc71;width:0%;transition:width 0.3s;}");
        html.append("</style>");
        html.append("</head><body>");
        html.append("<div class='container'>");
        html.append("<h1>Générateur de Labyrinthe Minecraft</h1>");
        
        html.append("<div id='size-display'>Taille totale: 0 x 0 x 0 (blocs)</div>");

        html.append("<form action='/generate' method='POST' id='genForm' onsubmit='event.preventDefault(); startGeneration();'>");
        
        html.append("<div class='section'>");
        html.append("<h2>Configuration de base</h2>");
        html.append("<div class='form-group'><label>Version:</label><select name='version' id='version' onchange='updateThemes()'>");
        for (String v : getAvailableVersions()) {
            html.append("<option value='").append(v).append("'>").append(v).append("</option>");
        }
        html.append("</select></div>");
        
        html.append("<div class='form-group'><label>Largeur (X cellules):</label><input type='number' name='width' id='width' value='20' oninput='updateSize()'></div>");
        html.append("<div class='form-group'><label>Profondeur (Z cellules):</label><input type='number' name='depth' id='depth' value='20' oninput='updateSize()'></div>");
        html.append("<div class='form-group'><label>Hauteur (Y blocs):</label><input type='number' name='height' id='height' value='3' oninput='updateSize()'></div>");
        html.append("<div class='form-group'><label>Largeur Couloirs:</label><input type='number' name='corridorWidth' id='corridorWidth' value='1' oninput='updateSize()'></div>");
        html.append("<div class='form-group'><label>Épaisseur Murs:</label><input type='number' name='wallWidth' id='wallWidth' value='1' oninput='updateSize()'></div>");
        html.append("<div class='form-group'><label>Érosion (0.0-1.0):</label><input type='text' name='erosion' value='0.0'></div>");
        html.append("<div class='form-group'><label>Base Y:</label><input type='number' name='baseY' value='64'></div>");
        html.append("<div class='form-group'><label>Plafond:</label><input type='checkbox' name='ceiling' id='ceiling' value='true' checked onchange='updateSize()'></div>");
        html.append("<div class='form-group'><label>Algorithme:</label><select name='algorithm'>");
        for (String alg : MazeAlgorithm.listAlgorithms()) {
            html.append("<option value='").append(alg).append("'>").append(alg).append("</option>");
        }
        html.append("</select></div>");
        html.append("<div class='form-group'><label>Thème:</label><select name='theme' id='theme'></select></div>");
        html.append("<div class='form-group'><label>Type d'export:</label><select name='exportType'>");
        html.append("<option value='schematic'>Schematic (.schematic)</option>");
        html.append("<option value='world'>Monde Minecraft (ZIP)</option>");
        html.append("<option value='image'>Image PNG (.png)</option>");
        html.append("</select></div>");
        html.append("<div class='form-group'><label>Nom du fichier/monde:</label><input type='text' name='schematicName' value='labyrinth'></div>");
        html.append("</div>");

        html.append("<div class='section'>");
        html.append("<h2>Salles</h2>");
        html.append("<div class='form-group'>");
        html.append("X: <input type='number' id='room_x' value='0' style='width:50px'> ");
        html.append("Z: <input type='number' id='room_z' value='0' style='width:50px'> ");
        html.append("W: <input type='number' id='room_w' value='5' style='width:50px'> ");
        html.append("D: <input type='number' id='room_d' value='5' style='width:50px'> ");
        html.append("E: <input type='number' id='room_e' value='1' style='width:40px'> ");
        html.append("<button type='button' onclick='addRoom()'>Ajouter</button>");
        html.append("</div>");
        html.append("<div id='room_list' class='list-container'></div>");
        html.append("<div id='room_inputs'></div>");
        html.append("</div>");

        html.append("<div class='section'>");
        html.append("<h2>Zones d'érosion</h2>");
        html.append("<div class='form-group'>");
        html.append("X: <input type='number' id='ez_x' value='0' style='width:50px'> ");
        html.append("Z: <input type='number' id='ez_z' value='0' style='width:50px'> ");
        html.append("W: <input type='number' id='ez_w' value='10' style='width:50px'> ");
        html.append("D: <input type='number' id='ez_d' value='10' style='width:50px'> ");
        html.append("F: <input type='text' id='ez_f' value='0.5' style='width:40px'> ");
        html.append("<button type='button' onclick='addEZ()'>Ajouter</button>");
        html.append("</div>");
        html.append("<div id='ez_list' class='list-container'></div>");
        html.append("<div id='ez_inputs'></div>");
        html.append("</div>");

        html.append("<div style='text-align:center;margin-top:20px;'>");
        html.append("<button type='submit' style='font-size:1.2em;background:#2ecc71;'>Générer et Télécharger</button>");
        html.append("</div>");
        
        html.append("</form>");

        html.append("<div class='progress-container' id='progress-container'>");
        html.append("<div id='progress-task'>Préparation...</div>");
        html.append("<div class='progress-bar'><div class='progress-fill' id='progress-fill'></div></div>");
        html.append("<div id='progress-text' style='text-align:right;margin-top:5px;'>0%</div>");
        html.append("</div>");

        html.append("</div>");

        html.append("<script>");
        html.append("const themes = {");
        for (String v : getAvailableVersions()) {
            html.append("'").append(v).append("': [");
            List<String> themeList = ThemeLoader.listThemes(v);
            for (int i = 0; i < themeList.size(); i++) {
                html.append("'").append(themeList.get(i)).append("'");
                if (i < themeList.size() - 1) html.append(",");
            }
            html.append("],");
        }
        html.append("};");

        html.append("function updateThemes() {");
        html.append("  const v = document.getElementById('version').value;");
        html.append("  const sel = document.getElementById('theme');");
        html.append("  sel.innerHTML = '';");
        html.append("  (themes[v] || []).forEach(t => {");
        html.append("    const opt = document.createElement('option');");
        html.append("    opt.value = t; opt.textContent = t;");
        html.append("    if (t === 'stone') opt.selected = true;");
        html.append("    sel.appendChild(opt);");
        html.append("  });");
        html.append("}");

        html.append("function updateSize() {");
        html.append("  const w = parseInt(document.getElementById('width').value) || 0;");
        html.append("  const d = parseInt(document.getElementById('depth').value) || 0;");
        html.append("  const h = parseInt(document.getElementById('height').value) || 0;");
        html.append("  const cw = parseInt(document.getElementById('corridorWidth').value) || 0;");
        html.append("  const ww = parseInt(document.getElementById('wallWidth').value) || 0;");
        html.append("  const ceil = document.getElementById('ceiling').checked;");
        html.append("  const tx = w * (cw + ww) + ww;");
        html.append("  const tz = d * (cw + ww) + ww;");
        html.append("  const ty = h + (ceil ? 2 : 1);");
        html.append("  document.getElementById('size-display').textContent = `Taille totale: ${tx} x ${ty} x ${tz} (blocs)`;");
        html.append("}");

        html.append("let rooms = [];");
        html.append("function addRoom() {");
        html.append("  const r = {x:document.getElementById('room_x').value, z:document.getElementById('room_z').value, w:document.getElementById('room_w').value, d:document.getElementById('room_d').value, e:document.getElementById('room_e').value};");
        html.append("  rooms.push(r); updateRooms();");
        html.append("}");
        html.append("function updateRooms() {");
        html.append("  const list = document.getElementById('room_list'); list.innerHTML = '';");
        html.append("  const inputs = document.getElementById('room_inputs'); inputs.innerHTML = '';");
        html.append("  rooms.forEach((r, i) => {");
        html.append("    const div = document.createElement('div'); div.className='list-item';");
        html.append("    div.innerHTML = `<span>Salle: [${r.x}, ${r.z}] ${r.w}x${r.d} (${r.e} entrées)</span> <button class='remove-btn' type='button' onclick='rooms.splice(${i},1);updateRooms()'>X</button>`;");
        html.append("    list.appendChild(div);");
        html.append("    ['x','z','w','d','e'].forEach(k => {");
        html.append("      const inp = document.createElement('input'); inp.type='hidden'; inp.name='room_'+k; inp.value=r[k];");
        html.append("      inputs.appendChild(inp);");
        html.append("    });");
        html.append("  });");
        html.append("}");

        html.append("let ezs = [];");
        html.append("function addEZ() {");
        html.append("  const ez = {x:document.getElementById('ez_x').value, z:document.getElementById('ez_z').value, w:document.getElementById('ez_w').value, d:document.getElementById('ez_d').value, f:document.getElementById('ez_f').value};");
        html.append("  ezs.push(ez); updateEZs();");
        html.append("}");
        html.append("function updateEZs() {");
        html.append("  const list = document.getElementById('ez_list'); list.innerHTML = '';");
        html.append("  const inputs = document.getElementById('ez_inputs'); inputs.innerHTML = '';");
        html.append("  ezs.forEach((ez, i) => {");
        html.append("    const div = document.createElement('div'); div.className='list-item';");
        html.append("    div.innerHTML = `<span>Erosion: [${ez.x}, ${ez.z}] ${ez.w}x${ez.d} (F: ${ez.f})</span> <button class='remove-btn' type='button' onclick='ezs.splice(${i},1);updateEZs()'>X</button>`;");
        html.append("    list.appendChild(div);");
        html.append("    ['x','z','w','d','f'].forEach(k => {");
        html.append("      const inp = document.createElement('input'); inp.type='hidden'; inp.name='ez_'+k; inp.value=ez[k];");
        html.append("      inputs.appendChild(inp);");
        html.append("    });");
        html.append("  });");
        html.append("}");

        html.append("function startGeneration() {");
        html.append("  const form = document.getElementById('genForm');");
        html.append("  const formData = new FormData(form);");
        html.append("  const params = new URLSearchParams();");
        html.append("  for (const pair of formData) params.append(pair[0], pair[1]);");
        
        html.append("  document.getElementById('progress-container').style.display = 'block';");
        html.append("  document.getElementById('progress-fill').style.width = '0%';");
        html.append("  document.getElementById('progress-task').textContent = 'Initialisation...';");

        html.append("  fetch('/generate', { method: 'POST', body: params })");
        html.append("    .then(r => r.json())");
        html.append("    .then(data => {");
        html.append("      if(data.status === 'started') {");
        html.append("        startPolling();");
        html.append("      }");
        html.append("    });");
        html.append("}");

        html.append("let progInterval;");
        html.append("function startPolling() {");
        html.append("  if(progInterval) clearInterval(progInterval);");
        html.append("  progInterval = setInterval(() => {");
        html.append("    fetch('/progress').then(r => r.json()).then(data => {");
        html.append("      if(data.task) {");
        html.append("        document.getElementById('progress-task').textContent = data.task;");
        html.append("        const p = Math.round((data.current / data.total) * 100);");
        html.append("        document.getElementById('progress-fill').style.width = p + '%';");
        html.append("        document.getElementById('progress-text').textContent = p + '% (' + data.current + '/' + data.total + ')';");
        
        html.append("        if(data.finished) {");
        html.append("           clearInterval(progInterval);");
        html.append("           window.location.href = '/download';");
        html.append("           setTimeout(() => { document.getElementById('progress-container').style.display = 'none'; }, 3000);");
        html.append("        } else if(data.task.startsWith('Erreur:')) {");
        html.append("           clearInterval(progInterval);");
        html.append("           alert(data.task);");
        html.append("        }");
        html.append("      }");
        html.append("    });");
        html.append("  }, 500);");
        html.append("}");

        html.append("updateThemes(); updateSize();");
        html.append("</script>");
        html.append("</body></html>");
        return html.toString();
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
