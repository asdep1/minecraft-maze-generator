package fr.asdep.labgen.core;

public class ErosionZone {
    private int x, z;
    private int width, depth;
    private float factor;

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

    public void setFactor(float factor) {
        this.factor = factor;
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
}
