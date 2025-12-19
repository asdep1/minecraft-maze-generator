package fr.asdep.labgen.mc;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class BlockRegistry {
    private final Map<String, Integer> idMap = new HashMap<>();
    private final String version;

    public BlockRegistry(String version) {
        this.version = version;
        loadFromCsv(version);
    }

    private void loadFromCsv(String version) {
        Path csvPath = Paths.get("versions", version + ".csv");
        if (!csvPath.toFile().exists()) {
            System.err.println("Warning: Registry file not found: " + csvPath);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(csvPath.toFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String blockId = parts[0].trim();
                    try {
                        int numericId = Integer.parseInt(parts[1].trim());
                        idMap.put(blockId, numericId);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid numeric ID in " + version + ".csv: " + parts[1]);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading registry " + version + ": " + e.getMessage());
        }
    }

    public int getNumericId(String blockId) {
        return idMap.getOrDefault(blockId, 0); // Default to Air if not found (safer than Stone)
    }

    public String getVersion() {
        return version;
    }
}
