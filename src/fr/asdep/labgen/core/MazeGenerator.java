package fr.asdep.labgen.core;

import fr.asdep.labgen.mc.BlockRegistry;
import fr.asdep.labgen.mc.BlockState;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.utils.ProgressBar;
import maze.Direction;
import maze.Maze;

import java.lang.reflect.InvocationTargetException;

public class MazeGenerator {
    private final GenerationConfig config;
    private final short[] voxelData;
    private final java.util.List<BlockState> palette;
    private final BlockRegistry registry;
    private boolean generated = false;

    public boolean isGenerated() {
        return generated;
    }

    public MazeGenerator(GenerationConfig config) {
        this.config = config;
        this.voxelData = new short[config.getTotalWidth() * config.getTotalHeight() * config.getTotalDepth()];
        this.palette = new java.util.ArrayList<>();
        this.palette.add(null); // Index 0 is air/null
        this.registry = new BlockRegistry(config.getGameVersion());
    }

    public BlockRegistry getRegistry() {
        return registry;
    }

    private synchronized short getOrCreatePaletteIndex(BlockState block) {
        if (block == null) return 0;
        for (short i = 0; i < palette.size(); i++) {
            if (block.equals(palette.get(i))) return i;
        }
        palette.add(block);
        return (short) (palette.size() - 1);
    }

    private int getIndex(int x, int y, int z) {
        return (y * config.getTotalDepth() + z) * config.getTotalWidth() + x;
    }

    public void generate() {
        Maze maze = null;
        ProgressBar pbStruct = new ProgressBar("Structure Logique", 1);
        try {
            maze = config.getAlgorithm().getAlgoClass().getConstructor(int.class, int.class).newInstance(config.getWidth(), config.getDepth());
            maze.generate();
        } catch (InstantiationException | SecurityException | NoSuchMethodException | InvocationTargetException |
                 IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        pbStruct.step();

        applyErosion(maze);
        fillWithAir();
        generateFloorAndCeiling();
        applyMazeToVoxels(maze);
        applyRooms();

        ProgressBar pbBorders = new ProgressBar("Bordures", 1);
        forceOuterBorders();
        pbBorders.step();
        System.out.println("Génération terminée.");
        generated = true;
    }

    private void generateFloorAndCeiling() {
        Theme theme = config.getTheme();
        ProgressBar pb = new ProgressBar("Sol et Plafond", config.getTotalWidth());
        java.util.stream.IntStream.range(0, config.getTotalWidth()).parallel().forEach(x -> {
            for (int z = 0; z < config.getTotalDepth(); z++) {
                setBlockAt(x, 0, z, theme.getFloor());
                if (config.isCeilingEnabled()) {
                    setBlockAt(x, config.getTotalHeight() - 1, z, theme.getCeiling());
                }
            }
            pb.step();
        });
    }

    private void applyMazeToVoxels(Maze maze) {
        int cw = config.getCorridorWidth();
        int ww = config.getWallWidth();

        ProgressBar pb = new ProgressBar("Voxelisation", maze.getHeight());
        java.util.stream.IntStream.range(0, maze.getHeight()).parallel().forEach(mz -> {
            for (int mx = 0; mx < maze.getWidth(); mx++) {
                int vx = ww + mx * (cw + ww);
                int vz = ww + mz * (cw + ww);

                fillVoxelArea(vx, 1, vz, cw, config.getHeight(), cw, (short) 0);
                applyCellWalls(maze, mx, mz, vx, vz);
            }
            pb.step();
        });
    }

    private void applyCellWalls(Maze maze, int mx, int mz, int vx, int vz) {
        Theme theme = config.getTheme();
        int cw = config.getCorridorWidth();
        int ww = config.getWallWidth();

        if (maze.isWall(mx, mz, Direction.NORTH)) {
            fillVoxelArea(vx, 1, vz - ww, cw, config.getHeight(), ww, theme.getWallsList());
        } else {
            fillVoxelArea(vx, 1, vz - ww, cw, config.getHeight(), ww, (short) 0);
        }

        if (maze.isWall(mx, mz, Direction.WEST)) {
            fillVoxelArea(vx - ww, 1, vz, ww, config.getHeight(), cw, theme.getWallsList());
        } else {
            fillVoxelArea(vx - ww, 1, vz, ww, config.getHeight(), cw, (short) 0);
        }

        if (shouldPlacePillar(maze, mx, mz)) {
            fillVoxelArea(vx - ww, 1, vz - ww, ww, config.getHeight(), ww, theme.getWallsList());
        } else {
            fillVoxelArea(vx - ww, 1, vz - ww, ww, config.getHeight(), ww, (short) 0);
        }
    }

    private boolean shouldPlacePillar(Maze maze, int mx, int mz) {
        boolean wallNorth = maze.isWall(mx, mz, Direction.NORTH);
        boolean wallWest = maze.isWall(mx, mz, Direction.WEST);
        boolean wallNorthOfWest = (mx > 0) && maze.isWall(mx - 1, mz, Direction.NORTH);
        boolean wallWestOfNorth = (mz > 0) && maze.isWall(mx, mz - 1, Direction.WEST);
        return wallNorth || wallWest || wallNorthOfWest || wallWestOfNorth;
    }

    private void forceOuterBorders() {
        Theme theme = config.getTheme();
        int ww = config.getWallWidth();
        int lastVX = config.getTotalWidth() - ww;
        int lastVZ = config.getTotalDepth() - ww;

        fillVoxelArea(0, 1, 0, config.getTotalWidth(), config.getHeight(), ww, theme.getWallsList());
        fillVoxelArea(0, 1, 0, ww, config.getHeight(), config.getTotalDepth(), theme.getWallsList());
        fillVoxelArea(lastVX, 1, 0, ww, config.getHeight(), config.getTotalDepth(), theme.getWallsList());
        fillVoxelArea(0, 1, lastVZ, config.getTotalWidth(), config.getHeight(), ww, theme.getWallsList());
    }

    private void applyErosion(Maze maze) {
        float baseErosion = config.getErosion();
        if (baseErosion <= 0 && config.getErosionZones().isEmpty()) return;

        ProgressBar pb = new ProgressBar("Érosion", maze.getHeight());
        java.util.stream.IntStream.range(0, maze.getHeight()).parallel().forEach(y -> {
            for (int x = 0; x < maze.getWidth(); x++) {
                if (x < maze.getWidth() - 1 && maze.isWall(x, y, Direction.EAST)) {
                    if (!isWallPartOfAnyRoom(x, y, Direction.EAST)) {
                        if (java.util.concurrent.ThreadLocalRandom.current().nextFloat() < calculateErosion(x, y, baseErosion)) {
                            maze.removeWall(x, y, Direction.EAST);
                        }
                    }
                }

                if (y < maze.getHeight() - 1 && maze.isWall(x, y, Direction.SOUTH)) {
                    if (!isWallPartOfAnyRoom(x, y, Direction.SOUTH)) {
                        if (java.util.concurrent.ThreadLocalRandom.current().nextFloat() < calculateErosion(x, y, baseErosion)) {
                            maze.removeWall(x, y, Direction.SOUTH);
                        }
                    }
                }
            }
            pb.step();
        });
    }

    private float calculateErosion(int x, int y, float baseErosion) {
        float currentErosion = baseErosion;
        for (ErosionZone zone : config.getErosionZones()) {
            if (zone.contains(x, y)) {
                currentErosion += zone.getFactor();
            }
        }
        return currentErosion;
    }

    private boolean isWallPartOfAnyRoom(int x, int z, Direction dir) {
        for (Room room : config.getRooms()) {
            if (room.isEdge(x, z, dir) || (room.isInside(x, z) && room.isInside(x + dir.dx, z + dir.dy))) {
                return true;
            }
        }
        return false;
    }

    private void applyRooms() {
        if (config.getRooms().isEmpty()) return;
        Theme theme = config.getTheme();
        ProgressBar pb = new ProgressBar("Salles", config.getRooms().size());
        for (Room room : config.getRooms()) {
            applyRoom(room, theme);
            pb.step();
        }
    }

    private void applyRoom(Room room, Theme theme) {
        int cw = config.getCorridorWidth();
        int ww = config.getWallWidth();

        int vxStart = ww + room.getX() * (cw + ww);
        int vzStart = ww + room.getZ() * (cw + ww);
        int vWidth = room.getWidth() * cw + (room.getWidth() - 1) * ww;
        int vDepth = room.getDepth() * cw + (room.getDepth() - 1) * ww;

        fillVoxelArea(vxStart, 1, vzStart, vWidth, config.getHeight(), vDepth, (short) 0);

        java.util.List<Point3D> potentialEntrances = collectPotentialEntrances(room, vxStart, vzStart, vWidth, vDepth, cw, ww);
        java.util.Collections.shuffle(potentialEntrances);

        int entrancesToOpen = Math.min(room.getEntrances(), potentialEntrances.size());
        for (int i = 0; i < potentialEntrances.size(); i++) {
            Point3D p = potentialEntrances.get(i);
            if (i < entrancesToOpen) {
                fillVoxelArea(p.x, 1, p.z, p.w, config.getHeight(), p.d, (short) 0);
            } else {
                fillVoxelArea(p.x, 1, p.z, p.w, config.getHeight(), p.d, theme.getWallsList());
            }
        }
    }

    private java.util.List<Point3D> collectPotentialEntrances(Room room, int vxStart, int vzStart, int vWidth, int vDepth, int cw, int ww) {
        java.util.List<Point3D> potentialEntrances = new java.util.ArrayList<>();
        for (int rx = 0; rx < room.getWidth(); rx++) {
            potentialEntrances.add(new Point3D(vxStart + rx * (cw + ww), vzStart - ww, cw, ww, true));
        }
        for (int rx = 0; rx < room.getWidth(); rx++) {
            potentialEntrances.add(new Point3D(vxStart + rx * (cw + ww), vzStart + vDepth, cw, ww, true));
        }
        for (int rz = 0; rz < room.getDepth(); rz++) {
            potentialEntrances.add(new Point3D(vxStart - ww, vzStart + rz * (cw + ww), ww, cw, false));
        }
        for (int rz = 0; rz < room.getDepth(); rz++) {
            potentialEntrances.add(new Point3D(vxStart + vWidth, vzStart + rz * (cw + ww), ww, cw, false));
        }
        return potentialEntrances;
    }

    private void fillWithAir() {
        java.util.Arrays.fill(voxelData, (short) 0);
    }

    public void setBlockAt(int x, int y, int z, BlockState block) {
        if (isInBounds(x, y, z)) {
            voxelData[getIndex(x, y, z)] = getOrCreatePaletteIndex(block);
        }
    }

    private void fillVoxelArea(int x, int y, int z, int w, int h, int d, java.util.List<BlockState> blocks) {
        java.util.Random rand = new java.util.Random();
        for (int dy = 0; dy < h; dy++) {
            for (int dz = 0; dz < d; dz++) {
                for (int dx = 0; dx < w; dx++) {
                    BlockState block = blocks.get(rand.nextInt(blocks.size()));
                    setBlockAt(x + dx, y + dy, z + dz, block);
                }
            }
        }
    }

    private void fillVoxelArea(int x, int y, int z, int w, int h, int d, short blockIndex) {
        for (int dy = 0; dy < h; dy++) {
            for (int dz = 0; dz < d; dz++) {
                for (int dx = 0; dx < w; dx++) {
                    int vx = x + dx;
                    int vy = y + dy;
                    int vz = z + dz;
                    if (isInBounds(vx, vy, vz)) {
                        voxelData[getIndex(vx, vy, vz)] = blockIndex;
                    }
                }
            }
        }
    }

    private boolean isInBounds(int x, int y, int z) {
        return x >= 0 && x < config.getTotalWidth() &&
                y >= 0 && y < config.getTotalHeight() &&
                z >= 0 && z < config.getTotalDepth();
    }

    public BlockState getBlockAt(int x, int y, int z) {
        if (!isInBounds(x, y, z)) return null;
        return palette.get(voxelData[getIndex(x, y, z)]);
    }

    public short[] getVoxelData() {
        return voxelData;
    }

    public java.util.List<BlockState> getPalette() {
        return palette;
    }

    public GenerationConfig getConfig() {
        return config;
    }

    private static class Point3D {
        int x, z, w, d;
        boolean horizontal;

        Point3D(int x, int z, int w, int d, boolean horizontal) {
            this.x = x;
            this.z = z;
            this.w = w;
            this.d = d;
            this.horizontal = horizontal;
        }
    }
}
