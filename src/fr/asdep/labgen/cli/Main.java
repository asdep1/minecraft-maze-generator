package fr.asdep.labgen.cli;

import fr.asdep.labgen.core.*;
import fr.asdep.labgen.exporter.ImageExporter;
import fr.asdep.labgen.exporter.SchematicExporter;
import fr.asdep.labgen.exporter.WorldExporter;
import fr.asdep.labgen.mc.BlockState;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.mc.ThemeLoader;
import fr.asdep.labgen.mc.WeightedBlock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Labyrinth Generator for Minecraft ===");
        
        Scanner scanner = new Scanner(System.in);

        GenerationConfig config = new GenerationConfig();

        // Game version
        System.out.print("Version du jeu (1.12.2 ou 1.20.1) [défaut: 1.12.2]: ");
        String version = scanner.nextLine().trim();
        if (version.isEmpty()) {
            version = "1.12.2";
        }
        config.setGameVersion(version);

        // Dimensions
        System.out.print("Largeur du labyrinthe en cellules [défaut: 250]: ");
        String widthStr = scanner.nextLine().trim();
        int width = widthStr.isEmpty() ? 250 : Integer.parseInt(widthStr);
        config.setWidth(width);

        System.out.print("Profondeur du labyrinthe en cellules [défaut: 250]: ");
        String depthStr = scanner.nextLine().trim();
        int depth = depthStr.isEmpty() ? 250 : Integer.parseInt(depthStr);
        config.setDepth(depth);

        System.out.print("Hauteur des murs en blocs [défaut: 25]: ");
        String heightStr = scanner.nextLine().trim();
        int height = heightStr.isEmpty() ? 25 : Integer.parseInt(heightStr);
        config.setHeight(height);

        // Corridor and wall widths
        System.out.print("Largeur des couloirs en blocs [défaut: 10]: ");
        String corridorStr = scanner.nextLine().trim();
        int corridorWidth = corridorStr.isEmpty() ? 10 : Integer.parseInt(corridorStr);
        config.setCorridorWidth(corridorWidth);

        System.out.print("Largeur des murs en blocs [défaut: 7]: ");
        String wallStr = scanner.nextLine().trim();
        int wallWidth = wallStr.isEmpty() ? 7 : Integer.parseInt(wallStr);
        config.setWallWidth(wallWidth);

        // Erosion
        System.out.print("Facteur d'érosion (0.0 à 1.0) [défaut: 0.2]: ");
        String erosionStr = scanner.nextLine().trim();
        float erosion = erosionStr.isEmpty() ? 0.2f : Float.parseFloat(erosionStr);
        config.setErosion(erosion);

        // Ceiling
        System.out.print("Activer le plafond ? (oui/non) [défaut: non]: ");
        String ceilingStr = scanner.nextLine().trim().toLowerCase();
        boolean ceiling = ceilingStr.equals("oui") || ceilingStr.equals("o") || ceilingStr.equals("yes") || ceilingStr.equals("y");
        config.setCeilingEnabled(ceiling);

        // Base Y
        System.out.print("Coordonnée Y de base [défaut: 64]: ");
        String baseYStr = scanner.nextLine().trim();
        int baseY = baseYStr.isEmpty() ? 64 : Integer.parseInt(baseYStr);
        config.setBaseY(baseY);

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
                config.addErosionZone(new ErosionZone(zoneX, zoneZ, zoneWidth, zoneDepth, zoneFactor));
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
                config.addRoom(new Room(roomX, roomZ, roomWidth, roomDepth, entrances));
            }
        }

        String filename = "maze_output";
        String imageFilename = "maze_output";
        String worldName = "MazeWorld";

        System.out.print("Exporter en schematic ? (oui/non) ");
        boolean exportSchematic = scanner.nextLine().trim().toLowerCase().equals("oui");
        if (exportSchematic) {
            System.out.print("Nom du fichier de sortie: ");
            filename = scanner.nextLine().trim().isEmpty() ? filename : scanner.nextLine().trim();
        }
        System.out.print("Exporter en image PNG ? (oui/non) ");
        boolean exportImage = scanner.nextLine().trim().toLowerCase().equals("oui");
        if (exportImage) {
            System.out.print("Nom du fichier de sortie: ");
            imageFilename = scanner.nextLine().trim().isEmpty() ? imageFilename : scanner.nextLine().trim();
        }
        System.out.print("Exporter en monde ? (oui/non) ");
        boolean exportWorld = scanner.nextLine().trim().toLowerCase().equals("oui");
        if (exportWorld) {
            System.out.print("Nom du dossier de sortie: ");
            worldName = scanner.nextLine().trim().isEmpty() ? worldName : scanner.nextLine().trim();
        }

        // Theme selection
        List<String> availableThemes = ThemeLoader.listThemes(config.getGameVersion());
        System.out.println("Thèmes disponibles pour " + config.getGameVersion() + " : " + availableThemes);
        System.out.print("Choisir un thème [défaut: stone]: ");
        String selectedTheme = scanner.nextLine().trim();
        if (selectedTheme.isEmpty()) {
            selectedTheme = "stone";
        }

        try {
            config.setTheme(ThemeLoader.loadTheme(config.getGameVersion(), selectedTheme));
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement du thème, utilisation du thème par défaut.");
            config.setTheme(Theme.getDefault());
        }

        System.out.print("Choisissez l'algorythme "+ MazeAlgorithm.listAlgorithms() +" : ");
        String algorithmStr = scanner.nextLine().trim();
        if (MazeAlgorithm.fromName(algorithmStr) == null ) {
            algorithmStr = "recursive-backtracker";
        }
        config.setAlgorithm(MazeAlgorithm.fromName(algorithmStr));

        System.out.println("Configuration :");
        System.out.println(config);

        System.out.println("Génération du labyrinthe...");

        MazeGenerator generator = new MazeGenerator(config);
        generator.generate();

        System.out.println("Exportation...");
        try {
            if (exportSchematic) {
                SchematicExporter.export(generator, filename+".schematic");
                System.out.println("  - Exporté vers " + filename + ".schematic (Schematic)");
            }

            if (exportImage) {
                ImageExporter.export(generator, imageFilename+".png");
                System.out.println("  - Exporté vers " + imageFilename + ".png (Image PNG)");
            }

            if (exportWorld) {
                WorldExporter.export(generator, worldName);
                System.out.println("  - Exporté vers le dossier monde: " + worldName);
            }

            System.out.println("Succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'exportation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
