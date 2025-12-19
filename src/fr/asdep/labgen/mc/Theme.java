package fr.asdep.labgen.mc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Theme {
    private final List<WeightedBlock> floor;
    private final List<WeightedBlock> walls;
    private final List<WeightedBlock> ceiling;
    private final double totalWeightFloor;
    private final double totalWeightWalls;
    private final double totalWeightCeiling;
    private final Random random = new Random();

    public Theme(List<WeightedBlock> floor, List<WeightedBlock> walls, List<WeightedBlock> ceiling, boolean weighted) {
        this.floor = new ArrayList<>(floor);
        this.walls = new ArrayList<>(walls);
        this.ceiling = new ArrayList<>(ceiling);
        this.totalWeightFloor = calculateTotalWeight(this.floor);
        this.totalWeightWalls = calculateTotalWeight(this.walls);
        this.totalWeightCeiling = calculateTotalWeight(this.ceiling);
    }

    public Theme(List<BlockState> floor, List<BlockState> walls, List<BlockState> ceiling) {
        this(convertToWeighted(floor), convertToWeighted(walls), convertToWeighted(ceiling), true);
    }

    public Theme(BlockState floor, BlockState walls, BlockState ceiling) {
        this(Collections.singletonList(new WeightedBlock(floor, 1.0)),
                Collections.singletonList(new WeightedBlock(walls, 1.0)),
                Collections.singletonList(new WeightedBlock(ceiling, 1.0)),
                true);
    }

    private static List<WeightedBlock> convertToWeighted(List<BlockState> states) {
        List<WeightedBlock> weighted = new ArrayList<>();
        for (BlockState state : states) {
            weighted.add(new WeightedBlock(state, 1.0));
        }
        return weighted;
    }

    public static Theme getDefault() {
        BlockState stone = new BlockState("minecraft:stone", 0);
        return new Theme(stone, stone, stone);
    }

    private double calculateTotalWeight(List<WeightedBlock> list) {
        double total = 0;
        for (WeightedBlock wb : list) total += wb.getWeight();
        return total;
    }

    public List<BlockState> getFloorList() {
        return extractBlocks(floor);
    }

    public List<BlockState> getWallsList() {
        return extractBlocks(walls);
    }

    public List<BlockState> getCeilingList() {
        return extractBlocks(ceiling);
    }

    private List<BlockState> extractBlocks(List<WeightedBlock> weighted) {
        List<BlockState> blocks = new ArrayList<>();
        for (WeightedBlock wb : weighted) {
            blocks.add(wb.getBlock());
        }
        return blocks;
    }

    public BlockState getFloor() {
        return getRandomBlock(floor, totalWeightFloor);
    }

    public BlockState getWalls() {
        return getRandomBlock(walls, totalWeightWalls);
    }

    public BlockState getCeiling() {
        return getRandomBlock(ceiling, totalWeightCeiling);
    }

    private BlockState getRandomBlock(List<WeightedBlock> list, double totalWeight) {
        if (list == null || list.isEmpty()) return null;

        double r = random.nextDouble() * totalWeight;
        double countWeight = 0.0;
        for (WeightedBlock wb : list) {
            countWeight += wb.getWeight();
            if (countWeight >= r) {
                return wb.getBlock();
            }
        }
        return list.get(list.size() - 1).getBlock();
    }
}
