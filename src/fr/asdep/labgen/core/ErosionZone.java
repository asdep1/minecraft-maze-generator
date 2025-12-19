package fr.asdep.labgen.core;

public class ErosionZone {
    private final int x, z;
    private final int width, depth;
    private final float factor;

    public ErosionZone(int x, int z, int width, int depth, float factor) {
        this.x = x;
        this.z = z;
        this.width = width;
        this.depth = depth;
        this.factor = factor;
    }

    public boolean contains(int cellX, int cellZ) {
        return cellX >= x && cellX < x + width && cellZ >= z && cellZ < z + depth;
    }

    public float getFactor() {
        return factor;
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
}
