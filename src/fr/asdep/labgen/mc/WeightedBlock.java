package fr.asdep.labgen.mc;

public class WeightedBlock {
    private final BlockState block;
    private final double weight;

    public WeightedBlock(BlockState block, double weight) {
        this.block = block;
        this.weight = weight;
    }

    public BlockState getBlock() {
        return block;
    }

    public double getWeight() {
        return weight;
    }
}
