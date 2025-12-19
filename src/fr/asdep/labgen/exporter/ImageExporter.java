package fr.asdep.labgen.exporter;

import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.mc.BlockState;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageExporter {

    public static void export(MazeGenerator generator, String filename) throws IOException {
        int width = generator.getConfig().getTotalWidth();
        int depth = generator.getConfig().getTotalDepth();

        BufferedImage image = new BufferedImage(width, depth, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                BlockState state = generator.getBlockAt(x, 1, z);
                int color;
                if (state == null) {
                    color = Color.WHITE.getRGB(); // Air / Couloir
                } else {
                    color = Color.BLACK.getRGB(); // Mur
                }
                image.setRGB(x, z, color);
            }
        }

        File outputFile = new File(filename);
        ImageIO.write(image, "png", outputFile);
    }
}
