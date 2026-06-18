package fr.asdep.labgen.cli;

import fr.asdep.labgen.core.*;
import fr.asdep.labgen.exporter.ImageExporter;
import fr.asdep.labgen.exporter.SchematicExporter;
import fr.asdep.labgen.exporter.WorldExporter;
import fr.asdep.labgen.gui.LabyrinthGUI;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.mc.ThemeLoader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static final String VERSION = "1.0.2";

    /**
     * -v version (1.12.2, 1.20.1)
     * -w width (250)
     * -d depth (250)
     * -h height (25)
     * -c corridorWidth (10)
     * -ww wallWidth (7)
     * -f erosion (0.2)
     * -ce ceiling (false)
     * -baseY baseY (64)
     * -erosion-area erosion-area (x z width depth factor)
     * -room room (x z width depth entrances)
     * -ex-schem filenames (maze_output)
     * -ex-img filenames (maze_output)
     * -ex-world dirname (maze_output)
     * -theme theme (stone)
     * -alg alg (recursive-backtracker)
     * -v 1.12.2 -w 250 -d 250 -h 25 -c 10 -ww 7 -f 0.2 -ce false -baseY 64 -erosion-area 15 20 15 20 0.5 -room 100 100 50 50 19 -ex-schem test -ex-img test -ex-world test -theme stone -alg recursive-backtracker
     *
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("=== Labyrinth Generator for Minecraft (" + VERSION + ") ===");
        if (args.length > 0) {
            GenerationConfig generationConfig = new GenerationConfig();
            ExportConfig exportConfig = new ExportConfig();

            try {
                parseArguments(args, generationConfig, exportConfig);
                generateAndExport(generationConfig, exportConfig);
            } catch (Exception e) {
                System.err.println("Erreur lors du parsing des arguments : " + e.getMessage());
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            LabyrinthGUI.main(args);
        }
    }

    public static void startGenerationProcess() {
        System.out.println("=== Labyrinth Generator for Minecraft ===");

        Scanner scanner = new Scanner(System.in);

        GenerationConfig generationConfig = new GenerationConfig();
        ExportConfig exportConfig = new ExportConfig();

        // Game version
        System.out.print("Version du jeu (1.12.2 ou 1.20.1) [défaut: 1.12.2]: ");
        String version = scanner.nextLine().trim();
        if (version.isEmpty()) {
            version = "1.12.2";
        }
        generationConfig.setGameVersion(version);

        // Dimensions
        System.out.print("Largeur du labyrinthe en cellules [défaut: 250]: ");
        String widthStr = scanner.nextLine().trim();
        int width = widthStr.isEmpty() ? 250 : Integer.parseInt(widthStr);
        generationConfig.setWidth(width);

        System.out.print("Profondeur du labyrinthe en cellules [défaut: 250]: ");
        String depthStr = scanner.nextLine().trim();
        int depth = depthStr.isEmpty() ? 250 : Integer.parseInt(depthStr);
        generationConfig.setDepth(depth);

        System.out.print("Hauteur des murs en blocs [défaut: 25]: ");
        String heightStr = scanner.nextLine().trim();
        int height = heightStr.isEmpty() ? 25 : Integer.parseInt(heightStr);
        generationConfig.setHeight(height);

        // Corridor and wall widths
        System.out.print("Largeur des couloirs en blocs [défaut: 10]: ");
        String corridorStr = scanner.nextLine().trim();
        int corridorWidth = corridorStr.isEmpty() ? 10 : Integer.parseInt(corridorStr);
        generationConfig.setCorridorWidth(corridorWidth);

        System.out.print("Largeur des murs en blocs [défaut: 7]: ");
        String wallStr = scanner.nextLine().trim();
        int wallWidth = wallStr.isEmpty() ? 7 : Integer.parseInt(wallStr);
        generationConfig.setWallWidth(wallWidth);

        // Erosion
        System.out.print("Facteur d'érosion (0.0 à 1.0) [défaut: 0.2]: ");
        String erosionStr = scanner.nextLine().trim();
        float erosion = erosionStr.isEmpty() ? 0.2f : Float.parseFloat(erosionStr);
        generationConfig.setErosion(erosion);

        // Ceiling
        System.out.print("Activer le plafond ? (oui/non) [défaut: non]: ");
        String ceilingStr = scanner.nextLine().trim().toLowerCase();
        boolean ceiling = ceilingStr.equals("oui") || ceilingStr.equals("o") || ceilingStr.equals("yes") || ceilingStr.equals("y");
        generationConfig.setCeilingEnabled(ceiling);

        // Base Y
        System.out.print("Coordonnée Y de base [défaut: 64]: ");
        String baseYStr = scanner.nextLine().trim();
        int baseY = baseYStr.isEmpty() ? 64 : Integer.parseInt(baseYStr);
        generationConfig.setBaseY(baseY);

        // Erosion zones
        System.out.print("Ajouter des zones d'érosion ? (oui/non) [défaut: non]: ");
        String addErosionZones = scanner.nextLine().trim().toLowerCase();
        if (addErosionZones.equals("oui") || addErosionZones.equals("o") || addErosionZones.equals("yes") || addErosionZones.equals("y")) {
            System.out.print("Nombre de zones d'érosion: ");
            int numZones = Integer.parseInt(scanner.nextLine().trim());
            for (int i = 0; i < numZones; i++) {
                System.out.println("Zone d'érosion " + (i + 1) + ":");
                System.out.print("  X: ");
                int zoneX = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Z: ");
                int zoneZ = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Largeur: ");
                int zoneWidth = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Profondeur: ");
                int zoneDepth = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Facteur d'érosion: ");
                float zoneFactor = Float.parseFloat(scanner.nextLine().trim());
                generationConfig.addErosionZone(new ErosionZone(zoneX, zoneZ, zoneWidth, zoneDepth, zoneFactor));
            }
        }

        // Rooms
        System.out.print("Ajouter des salles ? (oui/non) [défaut: non]: ");
        String addRooms = scanner.nextLine().trim().toLowerCase();
        if (addRooms.equals("oui") || addRooms.equals("o") || addRooms.equals("yes") || addRooms.equals("y")) {
            System.out.print("Nombre de salles: ");
            int numRooms = Integer.parseInt(scanner.nextLine().trim());
            for (int i = 0; i < numRooms; i++) {
                System.out.println("Salle " + (i + 1) + ":");
                System.out.print("  X: ");
                int roomX = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Z: ");
                int roomZ = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Largeur: ");
                int roomWidth = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Profondeur: ");
                int roomDepth = Integer.parseInt(scanner.nextLine().trim());
                System.out.print("  Nombre d'entrées: ");
                int entrances = Integer.parseInt(scanner.nextLine().trim());
                generationConfig.addRoom(new Room(roomX, roomZ, roomWidth, roomDepth, entrances));
            }
        }

        System.out.print("Exporter en schematic ? (oui/non) ");
        if (scanner.nextLine().trim().equalsIgnoreCase("oui")) {
            exportConfig.setExportSchematic(true);
            System.out.print("Nom du fichier de sortie: ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) exportConfig.setSchematicName(input);
        }
        System.out.print("Exporter en image PNG ? (oui/non) ");
        if (scanner.nextLine().trim().equalsIgnoreCase("oui")) {
            exportConfig.setExportImage(true);
            System.out.print("Nom du fichier de sortie: ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) exportConfig.setImageName(input);
        }
        System.out.print("Exporter en monde ? (oui/non) ");
        if (scanner.nextLine().trim().equalsIgnoreCase("oui")) {
            exportConfig.setExportWorld(true);
            System.out.print("Nom du dossier de sortie: ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) exportConfig.setWorldName(input);
        }

        // Theme selection
        List<String> availableThemes = ThemeLoader.listThemes(generationConfig.getGameVersion());
        System.out.println("Thèmes disponibles pour " + generationConfig.getGameVersion() + " : " + availableThemes);
        System.out.print("Choisir un thème [défaut: stone]: ");
        String selectedTheme = scanner.nextLine().trim();
        if (selectedTheme.isEmpty()) {
            selectedTheme = "stone";
        }

        try {
            generationConfig.setTheme(ThemeLoader.loadTheme(generationConfig.getGameVersion(), selectedTheme));
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du thème, utilisation du thème par défaut.");
            generationConfig.setTheme(Theme.getDefault());
        }

        System.out.print("Choisissez l'algorythme " + MazeAlgorithm.listAlgorithms() + " : ");
        String algorithmStr = scanner.nextLine().trim();
        if (MazeAlgorithm.fromName(algorithmStr) == null) {
            algorithmStr = "recursive-backtracker";
        }
        generationConfig.setAlgorithm(MazeAlgorithm.fromName(algorithmStr));

        generateAndExport(generationConfig, exportConfig);
    }

    private static void parseArguments(String[] args, GenerationConfig generationConfig, ExportConfig exportConfig) throws IOException {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "-v":
                    if (i + 1 < args.length) {
                        generationConfig.setGameVersion(args[++i]);
                    }
                    break;
                case "-w":
                    if (i + 1 < args.length) {
                        generationConfig.setWidth(Integer.parseInt(args[++i]));
                    }
                    break;
                case "-d":
                    if (i + 1 < args.length) {
                        generationConfig.setDepth(Integer.parseInt(args[++i]));
                    }
                    break;
                case "-h":
                    if (i + 1 < args.length) {
                        generationConfig.setHeight(Integer.parseInt(args[++i]));
                    }
                    break;
                case "-c":
                    if (i + 1 < args.length) {
                        generationConfig.setCorridorWidth(Integer.parseInt(args[++i]));
                    }
                    break;
                case "-ww":
                    if (i + 1 < args.length) {
                        generationConfig.setWallWidth(Integer.parseInt(args[++i]));
                    }
                    break;
                case "-f":
                    if (i + 1 < args.length) {
                        generationConfig.setErosion(Float.parseFloat(args[++i]));
                    }
                    break;
                case "-ce":
                    if (i + 1 < args.length) {
                        generationConfig.setCeilingEnabled(Boolean.parseBoolean(args[++i]));
                    }
                    break;
                case "-baseY":
                    if (i + 1 < args.length) {
                        generationConfig.setBaseY(Integer.parseInt(args[++i]));
                    }
                    break;
                case "-erosion-area":
                    if (i + 5 < args.length) {
                        int x = Integer.parseInt(args[++i]);
                        int z = Integer.parseInt(args[++i]);
                        int width = Integer.parseInt(args[++i]);
                        int depth = Integer.parseInt(args[++i]);
                        float factor = Float.parseFloat(args[++i]);
                        generationConfig.addErosionZone(new ErosionZone(x, z, width, depth, factor));
                    }
                    break;
                case "-room":
                    if (i + 5 < args.length) {
                        int x = Integer.parseInt(args[++i]);
                        int z = Integer.parseInt(args[++i]);
                        int width = Integer.parseInt(args[++i]);
                        int depth = Integer.parseInt(args[++i]);
                        int entrances = Integer.parseInt(args[++i]);
                        generationConfig.addRoom(new Room(x, z, width, depth, entrances));
                    }
                    break;
                case "-ex-schem":
                    if (i + 1 < args.length) {
                        exportConfig.setExportSchematic(true);
                        exportConfig.setSchematicName(args[++i]);
                    }
                    break;
                case "-ex-img":
                    if (i + 1 < args.length) {
                        exportConfig.setExportImage(true);
                        exportConfig.setImageName(args[++i]);
                    }
                    break;
                case "-ex-world":
                    if (i + 1 < args.length) {
                        exportConfig.setExportWorld(true);
                        exportConfig.setWorldName(args[++i]);
                    }
                    break;
                case "-theme":
                    if (i + 1 < args.length) {
                        String themeName = args[++i];
                        generationConfig.setTheme(ThemeLoader.loadTheme(generationConfig.getGameVersion(), themeName));
                    }
                    break;
                case "-alg":
                    if (i + 1 < args.length) {
                        String algName = args[++i];
                        MazeAlgorithm algorithm = MazeAlgorithm.fromName(algName);
                        if (algorithm != null) {
                            generationConfig.setAlgorithm(algorithm);
                        }
                    }
                    break;
            }
        }

        // Set default theme if not specified
        if (generationConfig.getTheme() == null) {
            try {
                generationConfig.setTheme(ThemeLoader.loadTheme(generationConfig.getGameVersion(), "stone"));
            } catch (IOException e) {
                generationConfig.setTheme(Theme.getDefault());
            }
        }

        // Set default algorithm if not specified
        if (generationConfig.getAlgorithm() == null) {
            generationConfig.setAlgorithm(MazeAlgorithm.fromName("recursive-backtracker"));
        }
    }

    private static BufferedImage lastGeneratedImage;

    public static BufferedImage getLastGeneratedImage() {
        return lastGeneratedImage;
    }

    public static MazeGenerator generateAndExport(GenerationConfig generationConfig, ExportConfig exportConfig) {
        lastGeneratedImage = null;
        System.out.println("Configuration :");
        System.out.println(generationConfig);

        System.out.println("Génération du labyrinthe...");

        MazeGenerator generator = new MazeGenerator(generationConfig);
        generator.generate();

        System.out.println("Exportation...");
        try {
            if (exportConfig.isExportSchematic()) {
                SchematicExporter.export(generator, exportConfig.getSchematicName() + ".schematic");
                System.out.println("  - Exporté vers " + exportConfig.getSchematicName() + ".schematic (Schematic)");
            }

            if (exportConfig.isExportImage()) {
                lastGeneratedImage = ImageExporter.export(generator, exportConfig.getImageName() + ".png");
                System.out.println("  - Exporté vers " + exportConfig.getImageName() + ".png (Image PNG)");
            }

            if (exportConfig.isExportWorld()) {
                WorldExporter.export(generator, ".", exportConfig.getWorldName());
                System.out.println("  - Exporté vers le dossier monde: " + exportConfig.getWorldName());
            }

            System.out.println("Succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'exportation : " + e.getMessage());
            e.printStackTrace();
        }
        return generator;
    }
}
