package fr.asdep.labgen.exporter;

import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.mc.BlockState;

import java.io.FileOutputStream;
import java.io.IOException;

public class SchematicExporter {

    public static void export(MazeGenerator generator, String filePath) throws IOException {
        short[] data = generator.getVoxelData();
        int width = generator.getConfig().getTotalWidth();
        int height = generator.getConfig().getTotalHeight();
        int length = generator.getConfig().getTotalDepth();

        byte[] blocks = new byte[width * height * length];
        byte[] add = null;
        byte[] meta = new byte[width * height * length];

        for (int i = 0; i < data.length; i++) {
            BlockState bs = generator.getPalette().get(data[i]);
            if (bs == null) {
                blocks[i] = 0;
                meta[i] = 0;
            } else {
                int blockId = generator.getRegistry().getNumericId(bs.getId());
                blocks[i] = (byte) (blockId & 0xFF);
                meta[i] = (byte) bs.getMeta();

                if (blockId > 255) {
                    if (add == null) add = new byte[(blocks.length + 1) / 2];
                    ExporterUtils.setNibble(add, i, blockId >> 8);
                }
            }
        }

        try (NBTOutputStream nos = new NBTOutputStream(new FileOutputStream(filePath))) {
            nos.writeTagCompound("Schematic");
            nos.writeTagShort("Width", (short) width);
            nos.writeTagShort("Height", (short) height);
            nos.writeTagShort("Length", (short) length);
            nos.writeTagString("Materials", "Alpha");
            nos.writeTagByteArray("Blocks", blocks);
            if (add != null) {
                nos.writeTagByteArray("AddBlocks", add);
                nos.writeTagByteArray("Add", add);
            }
            nos.writeTagByteArray("Data", meta);
            nos.writeTagEnd();
        }
    }
}
