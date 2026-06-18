package fr.asdep.labgen.exporter;

import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.mc.BlockState;
import fr.asdep.labgen.utils.ProgressBar;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImageExporter {

    public static BufferedImage export(MazeGenerator generator, String filename) throws IOException {
        BufferedImage image = generateImage(generator);
        File outputFile = new File(filename);
        ImageIO.write(image, "png", outputFile);
        return image;
    }

    public static BufferedImage generateImage(MazeGenerator generator) {
        return generateImage(generator.getConfig(), generator);
    }

    private static BufferedImage generateImage(fr.asdep.labgen.core.GenerationConfig config, MazeGenerator generator) {
        int width = config.getTotalWidth();
        int depth = config.getTotalDepth();

        // Limiter la taille de l'image pour éviter l
        // es OutOfMemoryError
        int maxWidth = 4000;
        int maxHeight = 4000;
        double scale = 1.0;
        if (width > maxWidth || depth > maxHeight) {
            scale = Math.min((double) maxWidth / width, (double) maxHeight / depth);
            width = (int) (width * scale);
            depth = (int) (depth * scale);
        }

        BufferedImage image = new BufferedImage(width, depth, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        if (scale != 1.0) {
            g2d.scale(scale, scale);
        }

        if (generator != null) {
            ProgressBar pb = new ProgressBar("Export Image", width);
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {

                    int genX = (int) (x / scale);
                    int genZ = (int) (z / scale);
                    BlockState state = generator.getBlockAt(genX, 1, genZ);
                    int color;
                    if (state == null) {
                        color = Color.WHITE.getRGB(); // Air / Couloir
                    } else {
                        color = Color.BLACK.getRGB(); // Mur
                    }
                    image.setRGB(x, z, color);
                }
                pb.step();
            }
        } else {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, config.getTotalWidth(), config.getTotalDepth());

            int cw = config.getCorridorWidth();
            int ww = config.getWallWidth();
            int cellStep = cw + ww;

            g2d.setColor(Color.GRAY);
            for (int i = 0; i <= config.getWidth(); i++) {
                g2d.fillRect(i * cellStep, 0, ww, config.getTotalDepth());
            }
            for (int i = 0; i <= config.getDepth(); i++) {
                g2d.fillRect(0, i * cellStep, config.getTotalWidth(), ww);
            }
        }

        g2d.dispose();
        return image;
    }
}
