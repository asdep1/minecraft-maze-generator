package fr.asdep.labgen.mc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ThemeLoader {
    public static Theme loadTheme(String version, String themeName) throws IOException {
        File themeFile = new File("versions" + File.separator + version + File.separator + "themes" + File.separator + themeName + ".txt");
        if (!themeFile.exists()) {
            throw new IOException("Le fichier de thème n'existe pas : " + themeFile.getAbsolutePath());
        }

        List<WeightedBlock> floor = new ArrayList<>();
        List<WeightedBlock> walls = new ArrayList<>();
        List<WeightedBlock> ceiling = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(themeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] parts = line.split(";");
                if (parts.length < 3) continue;

                String type = parts[0].toLowerCase();
                String id = parts[1];
                int meta = Integer.parseInt(parts[2]);
                double weight = (parts.length >= 4) ? Double.parseDouble(parts[3]) : 1.0;

                WeightedBlock wb = new WeightedBlock(new BlockState(id, meta), weight);

                switch (type) {
                    case "floor":
                        floor.add(wb);
                        break;
                    case "walls":
                        walls.add(wb);
                        break;
                    case "ceiling":
                        ceiling.add(wb);
                        break;
                }
            }
        }

        if (floor.isEmpty() || walls.isEmpty() || ceiling.isEmpty()) {
            BlockState stone = new BlockState("minecraft:stone", 0);
            if (floor.isEmpty()) floor.add(new WeightedBlock(stone, 1.0));
            if (walls.isEmpty()) walls.add(new WeightedBlock(stone, 1.0));
            if (ceiling.isEmpty()) ceiling.add(new WeightedBlock(stone, 1.0));
        }

        return new Theme(floor, walls, ceiling, true);
    }

    public static List<String> listThemes(String version) {
        List<String> themes = new ArrayList<>();
        File themesDir = new File("versions" + File.separator + version + File.separator + "themes");
        if (themesDir.exists() && themesDir.isDirectory()) {
            File[] files = themesDir.listFiles((dir, name) -> name.endsWith(".txt"));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    themes.add(name.substring(0, name.lastIndexOf('.')));
                }
            }
        }
        return themes;
    }
}
