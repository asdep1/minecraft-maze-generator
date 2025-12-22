package fr.asdep.labgen.exporter;

import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.mc.BlockState;
import fr.asdep.labgen.utils.ProgressBar;

import java.io.FileOutputStream;
import java.io.IOException;

public class SchematicExporter {

    public static void export(MazeGenerator generator, String filePath) throws IOException {
        short[] data = generator.getVoxelData();
        int width = generator.getConfig().getTotalWidth();
        int height = generator.getConfig().getTotalHeight();
        int length = generator.getConfig().getTotalDepth();

        byte[] blocks = new byte[width * height * length];
        boolean hasExtendedIds = generator.getPalette().stream()
                .filter(java.util.Objects::nonNull)
                .anyMatch(bs -> generator.getRegistry().getNumericId(bs.getId()) > 255);
        byte[] add = hasExtendedIds ? new byte[(blocks.length + 1) / 2] : null;
        byte[] meta = new byte[width * height * length];

        ProgressBar pb = new ProgressBar("Schematic Conv", height);
        java.util.stream.IntStream.range(0, height).parallel().forEach(y -> {
            for (int z = 0; z < length; z++) {
                for (int x = 0; x < width; x++) {
                    int i = (y * length + z) * width + x;
                    BlockState bs = generator.getPalette().get(data[i]);
                    if (bs != null) {
                        int blockId = generator.getRegistry().getNumericId(bs.getId());
                        blocks[i] = (byte) (blockId & 0xFF);
                        meta[i] = (byte) bs.getMeta();

                        if (blockId > 255) {
                            ExporterUtils.setNibble(add, i, blockId >> 8);
                        }
                    }
                }
            }
            pb.step();
        });

        long totalBytes = (long) blocks.length + meta.length + (add != null ? add.length * 2L : 0);
        ProgressBar pbSave = new ProgressBar("Schematic Save", (int) (totalBytes / 1024));
        final long[] written = {0};

        try (NBTOutputStream nos = new NBTOutputStream(new FileOutputStream(filePath))) {
            nos.writeTagCompound("Schematic");
            nos.writeTagShort("Width", (short) width);
            nos.writeTagShort("Height", (short) height);
            nos.writeTagShort("Length", (short) length);
            nos.writeTagString("Materials", "Alpha");
            
            nos.writeTagByteArray("Blocks", blocks, p -> {
                written[0] = p;
                pbSave.update((int) (written[0] / 1024));
            });
            final long afterBlocks = written[0];
            
            if (add != null) {
                nos.writeTagByteArray("AddBlocks", add, p -> {
                    pbSave.update((int) ((afterBlocks + p) / 1024));
                });
                final long afterAdd1 = afterBlocks + add.length;
                nos.writeTagByteArray("Add", add, p -> {
                    pbSave.update((int) ((afterAdd1 + p) / 1024));
                });
                final long afterAdd2 = afterAdd1 + add.length;
                nos.writeTagByteArray("Data", meta, p -> {
                    pbSave.update((int) ((afterAdd2 + p) / 1024));
                });
            } else {
                nos.writeTagByteArray("Data", meta, p -> {
                    pbSave.update((int) ((afterBlocks + p) / 1024));
                });
            }
            
            nos.writeTagEnd();
        }
    }
}
