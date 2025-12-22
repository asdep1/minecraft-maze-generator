package fr.asdep.labgen.core;

import maze.*;

import java.util.ArrayList;
import java.util.List;

public enum MazeAlgorithm {
    RECURSIVE_BACKTRACKER("recursive-backtracker", RecursiveBacktracker.class),
    RANDOMIZED_KRUSKALS("randomized-kruskals", RandomizedKruskals.class),
    RANDOMIZED_PRIMS("randomized-prims", RandomizedPrims.class),
    RECURSIVE_DIVIDER("recursive-divider", RecursiveDivider.class),
    BINARY_TREE("binary-tree", BinaryTreeMaze.class),
    SIDEWINDER("sidewinder", Sidewinder.class),
    ELLERS("ellers", Ellers.class),
    WILSONS("wilsons", Wilsons.class);

    private final String name;
    private final Class<? extends Maze> algoClass;

    MazeAlgorithm(String name, Class<? extends Maze> algoClass) {
        this.name = name;
        this.algoClass = algoClass;
    }

    public String getName() {
        return name;
    }
    public Class<? extends Maze> getAlgoClass() {return algoClass;}

    public static MazeAlgorithm fromName(String name) {
        for (MazeAlgorithm algo : values()) {
            if (algo.name.equalsIgnoreCase(name) || algo.name().equalsIgnoreCase(name)) {
                return algo;
            }
        }
        return RECURSIVE_BACKTRACKER;
    }

    public static List<String> listAlgorithms() {
        ArrayList<String> list = new ArrayList<>();
        for (MazeAlgorithm algo : values()) {
            list.add(algo.getName());
        }
        return list;
    }
}
