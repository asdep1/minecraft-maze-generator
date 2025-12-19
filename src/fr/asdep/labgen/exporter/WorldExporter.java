package fr.asdep.labgen.exporter;

import fr.asdep.labgen.core.MazeGenerator;
import fr.asdep.labgen.mc.BlockState;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class WorldExporter {

    public static void export(MazeGenerator generator, String worldName) throws IOException {
        Path worldPath = Paths.get(worldName);
        Files.createDirectories(worldPath);
        Path regionDir = worldPath.resolve("region");
        Files.createDirectories(regionDir);
        Files.createDirectories(worldPath.resolve("data"));

        writeLevelDat(worldPath, worldName, generator);

        int totalWidth = generator.getConfig().getTotalWidth();
        int totalDepth = generator.getConfig().getTotalDepth();
        int chunksX = (totalWidth + 15) / 16;
        int chunksZ = (totalDepth + 15) / 16;

        int regionsX = (chunksX + 31) / 32;
        int regionsZ = (chunksZ + 31) / 32;

        for (int rz = 0; rz < regionsZ; rz++) {
            for (int rx = 0; rx < regionsX; rx++) {
                String mcaName = "r." + rx + "." + rz + ".mca";
                writeMCA(regionDir.resolve(mcaName), generator, rx, rz, chunksX, chunksZ);
            }
        }
    }

    private static void writeLevelDat(Path worldPath, String worldName, MazeGenerator generator) throws IOException {
        int baseY = generator.getConfig().getBaseY();
        BlockState floorBlock = generator.getConfig().getTheme().getFloor();
        String floorId = floorBlock != null ? floorBlock.getId() : "minecraft:grass";

        StringBuilder options = new StringBuilder("3;minecraft:bedrock");
        if (baseY > 2) {
            options.append(",").append(baseY - 2).append("*minecraft:stone");
        }
        if (baseY > 1) {
            options.append(",minecraft:dirt");
        }

        options.append(",").append(floorId);
        options.append(";1");

        try (NBTOutputStream nos = new NBTOutputStream(new FileOutputStream(worldPath.resolve("level.dat").toFile()), true)) {
            nos.writeTagCompound(""); // Root
            nos.writeTagCompound("Data");
            nos.writeTagInt("DataVersion", 1343); // 1.12.2
            nos.writeTagString("LevelName", worldName);
            nos.writeTagString("generatorName", "flat");
            nos.writeTagString("generatorOptions", options.toString());
            nos.writeTagInt("version", 19133);
            nos.writeTagLong("RandomSeed", 12345L);
            nos.writeTagInt("MapFeatures", 0);
            nos.writeTagInt("SpawnX", 0);
            nos.writeTagInt("SpawnY", baseY + 1);
            nos.writeTagInt("SpawnZ", 0);
            nos.writeTagLong("Time", 0L);
            nos.writeTagLong("LastPlayed", System.currentTimeMillis());
            nos.writeTagLong("SizeOnDisk", 0L);
            nos.writeTagInt("thundering", 0);
            nos.writeTagInt("thunderTime", 100000);
            nos.writeTagInt("raining", 0);
            nos.writeTagInt("rainTime", 100000);
            nos.writeTagInt("hardcore", 0);
            nos.writeTagInt("GameType", 1); // Creative
            nos.writeTagInt("Difficulty", 0);
            nos.writeTagInt("DifficultyLocked", 0);
            nos.writeTagInt("initialized", 1);
            nos.writeTagInt("allowCommands", 1);
            nos.writeTagCompound("GameRules");
            nos.writeTagEnd();
            nos.writeTagEnd();
            nos.writeTagEnd();
        }
    }


    private static void writeMCA(Path mcaPath, MazeGenerator generator, int rx, int rz, int totalChunksX, int totalChunksZ) throws IOException {
        byte[][] compressedChunks = new byte[1024][];
        for (int cz = 0; cz < 32; cz++) {
            for (int cx = 0; cx < 32; cx++) {
                int worldChunkX = rx * 32 + cx;
                int worldChunkZ = rz * 32 + cz;

                if (worldChunkX < totalChunksX && worldChunkZ < totalChunksZ) {
                    byte[] uncompressed = createChunkNBT(generator, worldChunkX, worldChunkZ);
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    try (java.util.zip.DeflaterOutputStream dos = new java.util.zip.DeflaterOutputStream(baos)) {
                        dos.write(uncompressed);
                    }
                    compressedChunks[cx + cz * 32] = baos.toByteArray();
                }
            }
        }

        int[] offsets = new int[1024];
        int currentSector = 2;
        for (int i = 0; i < 1024; i++) {
            if (compressedChunks[i] != null) {
                int length = compressedChunks[i].length + 5;
                int sectors = (length + 4095) / 4096;
                offsets[i] = (currentSector << 8) | (sectors & 0xFF);
                currentSector += sectors;
            }
        }

        try (FileOutputStream fos = new FileOutputStream(mcaPath.toFile())) {
            for (int i = 0; i < 1024; i++) {
                fos.write((offsets[i] >> 24) & 0xFF);
                fos.write((offsets[i] >> 16) & 0xFF);
                fos.write((offsets[i] >> 8) & 0xFF);
                fos.write(offsets[i] & 0xFF);
            }
            for (int i = 0; i < 1024; i++) {
                int ts = (int) (System.currentTimeMillis() / 1000L);
                fos.write((ts >> 24) & 0xFF);
                fos.write((ts >> 16) & 0xFF);
                fos.write((ts >> 8) & 0xFF);
                fos.write(ts & 0xFF);
            }
            for (int i = 0; i < 1024; i++) {
                if (compressedChunks[i] != null) {
                    int len = compressedChunks[i].length + 1;
                    fos.write((len >> 24) & 0xFF);
                    fos.write((len >> 16) & 0xFF);
                    fos.write((len >> 8) & 0xFF);
                    fos.write(len & 0xFF);
                    fos.write(2); // Zlib
                    fos.write(compressedChunks[i]);
                    int padding = 4096 - ((len + 4) % 4096);
                    if (padding < 4096) {
                        fos.write(new byte[padding]);
                    }
                }
            }
        }
    }

    private static byte[] createChunkNBT(MazeGenerator generator, int cx, int cz) throws IOException {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (NBTOutputStream nos = new NBTOutputStream(baos, false)) {
            nos.writeTagCompound(""); // Root
            nos.writeTagCompound("Level");
            nos.writeTagInt("DataVersion", 1343);
            nos.writeTagInt("xPos", cx);
            nos.writeTagInt("zPos", cz);
            nos.writeTagLong("LastUpdate", 0L);
            nos.writeTagByte("LightPopulated", (byte) 1);
            nos.writeTagByte("TerrainPopulated", (byte) 1);
            nos.writeTagByte("V", (byte) 1);
            nos.writeTagIntArray("HeightMap", new int[256]);
            nos.writeTagByteArray("Biomes", new byte[256]);

            int baseY = generator.getConfig().getBaseY();
            int totalHeight = generator.getConfig().getTotalHeight();
            int maxAbsY = baseY + totalHeight;
            int numSections = (maxAbsY + 15) / 16;
            nos.writeTagListHeader("Sections", (byte) 10, numSections);

            for (int sy = 0; sy < numSections; sy++) {
                nos.writeTagByte("Y", (byte) sy);

                byte[] blocks = new byte[4096];
                byte[] add = null;
                byte[] data = new byte[2048];
                byte[] blockLight = new byte[2048];
                byte[] skyLight = new byte[2048];
                java.util.Arrays.fill(skyLight, (byte) 0xFF);

                for (int y = 0; y < 16; y++) {
                    int absY = sy * 16 + y;
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            int worldX = cx * 16 + x;
                            int worldZ = cz * 16 + z;

                            int blockId = 0;
                            int meta = 0;

                            if (absY < baseY) {
                                if (absY == 0) {
                                    blockId = 7;
                                } else if (absY == baseY - 1) {
                                    blockId = 3;
                                } else {
                                    blockId = 1;
                                }
                            } else {
                                int mazeY = absY - baseY;
                                BlockState bs = generator.getBlockAt(worldX, mazeY, worldZ);
                                if (bs != null) {
                                    blockId = generator.getRegistry().getNumericId(bs.getId());
                                    meta = bs.getMeta();
                                }
                            }

                            if (blockId != 0) {
                                int blockIndex = y * 256 + z * 16 + x;
                                blocks[blockIndex] = (byte) (blockId & 0xFF);

                                if (blockId > 255) {
                                    if (add == null) add = new byte[2048];
                                    ExporterUtils.setNibble(add, blockIndex, blockId >> 8);
                                }

                                if (meta != 0) {
                                    ExporterUtils.setNibble(data, blockIndex, meta);
                                }
                            }
                        }
                    }
                }

                nos.writeTagByteArray("Blocks", blocks);
                if (add != null) {
                    nos.writeTagByteArray("Add", add);
                }
                nos.writeTagByteArray("Data", data);
                nos.writeTagByteArray("BlockLight", blockLight);
                nos.writeTagByteArray("SkyLight", skyLight);
                nos.writeTagEnd();
            }

            nos.writeTagListHeader("Entities", (byte) 10, 0);
            nos.writeTagListHeader("TileEntities", (byte) 10, 0);
            nos.writeTagEnd(); // Level
            nos.writeTagEnd(); // Root
        }
        return baos.toByteArray();
    }

}
