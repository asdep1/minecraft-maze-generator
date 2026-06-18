package fr.asdep.labgen.core;

import maze.Direction;

public class Room {
    private int x, z;
    private int width, depth;
    private int entrances;

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

    public void setX(int x) {
        this.x = x;
    }

    public int getZ() {
        return z;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public int getEntrances() {
        return entrances;
    }

    public void setEntrances(int entrances) {
        this.entrances = entrances;
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
