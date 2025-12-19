package fr.asdep.labgen.cli;

import fr.asdep.labgen.core.ErosionZone;
import fr.asdep.labgen.core.GenerationConfig;
import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.core.Room;
import fr.asdep.labgen.exporter.ImageExporter;
import fr.asdep.labgen.exporter.SchematicExporter;
import fr.asdep.labgen.exporter.WorldExporter;
import fr.asdep.labgen.mc.BlockState;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.mc.WeightedBlock;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Labyrinth Generator for Minecraft ===");

        GenerationConfig config = new GenerationConfig();
        config.setGameVersion("1.12.2");
        config.setWidth(50);
        config.setDepth(50);
        config.setHeight(25);
        config.setCorridorWidth(5);
        config.setWallWidth(3);
        config.setErosion(0.05f);
        config.setCeilingEnabled(false);
        config.setBaseY(64);

        config.addErosionZone(new ErosionZone(0, 0, 50, 50, 0.3f));

        config.addRoom(new Room(5, 5, 20, 20, 2));


        Theme stonetheme1122 = new Theme(
                Collections.singletonList(new WeightedBlock(new BlockState("minecraft:grass", 0), 1.0)),
                Arrays.asList(
                        new WeightedBlock(new BlockState("minecraft:stone", 0), 1.0),
                        new WeightedBlock(new BlockState("minecraft:stonebrick", 0), 70.0),
                        new WeightedBlock(new BlockState("minecraft:stonebrick", 1), 30.0)
                ),
                Collections.singletonList(new WeightedBlock(new BlockState("minecraft:glass", 0), 1.0)),
                true
        );

        Theme stonetheme1201 = new Theme(
                Collections.singletonList(new WeightedBlock(new BlockState("minecraft:grass", 0), 1.0)),
                Arrays.asList(
                        new WeightedBlock(new BlockState("minecraft:stone", 0), 1.0),
                        new WeightedBlock(new BlockState("minecraft:stone_bricks", 0), 20.0),
                        new WeightedBlock(new BlockState("minecraft:mossy_stone_bricks", 0), 30.0)
                ),
                Collections.singletonList(new WeightedBlock(new BlockState("minecraft:glass", 0), 1.0)),
                true
        );

        if (config.getGameVersion().equals("1.12.2")) {
            config.setTheme(stonetheme1122);
        } else if (config.getGameVersion().equals("1.20.1")) {
            config.setTheme(stonetheme1201);
        }

        System.out.println("Génération du labyrinthe...");
        MazeGenerator generator = new MazeGenerator(config);
        generator.generate();

        String filename = "maze_output.schematic";
        String imageFilename = "maze_output.png";
        String worldName = "MazeWorld";

        System.out.println("Exportation...");
        try {
            SchematicExporter.export(generator, filename);
            System.out.println("  - Exporté vers " + filename + " (Schematic)");

            ImageExporter.export(generator, imageFilename);
            System.out.println("  - Exporté vers " + imageFilename + " (Image PNG)");

            WorldExporter.export(generator, worldName);
            System.out.println("  - Exporté vers le dossier monde: " + worldName);

            System.out.println("Succès !");
        } catch (IOException e) {
            System.err.println("Erreur lors de l'exportation : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
