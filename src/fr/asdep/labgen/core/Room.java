package fr.asdep.labgen.core;

import maze.Direction;

public class Room {
    private final int x, z;
    private final int width, depth;
    private final int entrances;

    public Room(int x, int z, int width, int depth, int entrances) {
        this.x = x;
        this.z = z;
        this.width = width;
        this.depth = depth;
        this.entrances = entrances;
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
    }

    public int getEntrances() {
        return entrances;
    }

    public boolean isInside(int cx, int cz) {
        return cx >= x && cx < x + width && cz >= z && cz < z + depth;
    }

    public boolean isEdge(int cx, int cz, Direction dir) {
        boolean currentInside = isInside(cx, cz);
        boolean neighborInside = isInside(cx + dir.dx, cz + dir.dy);

        return currentInside != neighborInside;
    }
}
