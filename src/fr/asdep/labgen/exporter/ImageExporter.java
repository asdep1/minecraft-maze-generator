package fr.asdep.labgen.exporter;

import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.mc.BlockState;
import fr.asdep.labgen.utils.ProgressBar;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class ImageExporter {

    public static void export(MazeGenerator generator, OutputStream os) throws IOException {
        BufferedImage image = generateImage(generator);
        ImageIO.write(image, "png", os);
    }

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
        int totalWidth = config.getTotalWidth();
        int totalDepth = config.getTotalDepth();

        // limit to avoid OutOfMemoryError
        int maxWidth = 4000;
        int maxHeight = 4000;
        double scale = 1.0;
        if (totalWidth > maxWidth || totalDepth > maxHeight) {
            scale = Math.min((double) maxWidth / totalWidth, (double) maxHeight / totalDepth);
        }

        int imgWidth = (int) (totalWidth * scale);
        int imgDepth = (int) (totalDepth * scale);

        BufferedImage image = new BufferedImage(imgWidth, imgDepth, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        g2d.scale(scale, scale);

        if (generator != null && generator.isGenerated()) {
            ProgressBar pb = new ProgressBar("Export Image", totalWidth);
            for (int x = 0; x < totalWidth; x++) {
                for (int z = 0; z < totalDepth; z++) {
                    BlockState state = generator.getBlockAt(x, 1, z);
                    int color;
                    if (state == null) {
                        color = Color.WHITE.getRGB(); // Air / Couloir
                    } else {
                        color = Color.BLACK.getRGB(); // Mur
                    }
                    g2d.setColor(new Color(color));
                    g2d.fillRect(x, z, 1, 1);
                }
                pb.step();
            }
        } else {
            g2d.setColor(Color.LIGHT_GRAY);
            g2d.fillRect(0, 0, totalWidth, totalDepth);

            int cw = config.getCorridorWidth();
            int ww = config.getWallWidth();
            int cellStep = cw + ww;

            g2d.setColor(Color.GRAY);
            for (int i = 0; i <= config.getWidth(); i++) {
                g2d.fillRect(i * cellStep, 0, ww, totalDepth);
            }
            for (int i = 0; i <= config.getDepth(); i++) {
                g2d.fillRect(0, i * cellStep, totalWidth, ww);
            }

            g2d.setColor(new Color(255, 200, 200, 150));
            for (fr.asdep.labgen.core.Room room : config.getRooms()) {
                int rx = room.getX() * cellStep + ww;
                int rz = room.getZ() * cellStep + ww;
                int rw = room.getWidth() * cellStep - ww;
                int rd = room.getDepth() * cellStep - ww;
                g2d.fillRect(rx, rz, rw, rd);
                g2d.setColor(Color.RED);
                g2d.drawRect(rx, rz, rw, rd);
                g2d.setColor(new Color(255, 200, 200, 150));
            }

            g2d.setColor(new Color(200, 255, 200, 150));
            for (fr.asdep.labgen.core.ErosionZone ez : config.getErosionZones()) {
                int ex = ez.getX() * cellStep + ww;
                int ez_z = ez.getZ() * cellStep + ww;
                int ew = ez.getWidth() * cellStep - ww;
                int ed = ez.getDepth() * cellStep - ww;
                g2d.fillRect(ex, ez_z, ew, ed);
                g2d.setColor(Color.GREEN);
                g2d.drawRect(ex, ez_z, ew, ed);
                g2d.setColor(new Color(200, 255, 200, 150));
            }
        }

        g2d.dispose();
        return image;
    }
}
