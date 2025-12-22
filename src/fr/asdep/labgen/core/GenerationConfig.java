package fr.asdep.labgen.core;

import fr.asdep.labgen.mc.Theme;

public class GenerationConfig {
    private final java.util.List<Room> rooms;
    private final java.util.List<ErosionZone> erosionZones;
    private int width; // X
    private int depth; // Z
    private int height; // Y
    private int baseY; // Sol du monde
    private int corridorWidth; // Largeur des couloirs
    private int wallWidth; // Largeur des murs
    private float erosion; // Facteur d'erosion
    private boolean ceilingEnabled; // avec/sans plafond
    private Theme theme;
    private MazeAlgorithm algorithm;
    private String gameVersion;

    public GenerationConfig() {
        this.width = 10;
        this.depth = 10;
        this.height = 3;
        this.baseY = 64;
        this.corridorWidth = 1;
        this.wallWidth = 1;
        this.erosion = 0.0f;
        this.ceilingEnabled = true;
        this.theme = Theme.getDefault();
        this.algorithm = MazeAlgorithm.RECURSIVE_BACKTRACKER;
        this.rooms = new java.util.ArrayList<>();
        this.erosionZones = new java.util.ArrayList<>();
        this.gameVersion = "1.12.2";
    }

    // Getters and Setters
    public String getGameVersion() {
        return gameVersion;
    }

    public void setGameVersion(String gameVersion) {
        this.gameVersion = gameVersion;
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

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getBaseY() {
        return baseY;
    }

    public void setBaseY(int baseY) {
        this.baseY = baseY;
    }

    public int getCorridorWidth() {
        return corridorWidth;
    }

    public void setCorridorWidth(int corridorWidth) {
        this.corridorWidth = corridorWidth;
    }

    public int getWallWidth() {
        return wallWidth;
    }

    public void setWallWidth(int wallWidth) {
        this.wallWidth = wallWidth;
    }

    public float getErosion() {
        return erosion;
    }

    public void setErosion(float erosion) {
        this.erosion = erosion;
    }

    public boolean isCeilingEnabled() {
        return ceilingEnabled;
    }

    public void setCeilingEnabled(boolean ceilingEnabled) {
        this.ceilingEnabled = ceilingEnabled;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public java.util.List<Room> getRooms() {
        return rooms;
    }

    public void addRoom(Room room) {
        this.rooms.add(room);
    }

    public java.util.List<ErosionZone> getErosionZones() {
        return erosionZones;
    }

    public void addErosionZone(ErosionZone zone) {
        this.erosionZones.add(zone);
    }

    public int getTotalWidth() {
        return width * (corridorWidth + wallWidth) + wallWidth;
    }

    public int getTotalDepth() {
        return depth * (corridorWidth + wallWidth) + wallWidth;
    }

    public int getTotalHeight() {
        return height + (ceilingEnabled ? 2 : 1); // + sol (+ plafond si activé)
    }

    public MazeAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(MazeAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    @Override
    public String toString() {
        return "GenerationConfig{" +
                "width=" + width +
                ", depth=" + depth +
                ", height=" + height +
                ", baseY=" + baseY +
                ", corridorWidth=" + corridorWidth +
                ", wallWidth=" + wallWidth +
                ", erosion=" + erosion +
                ", ceilingEnabled=" + ceilingEnabled +
                ", theme=" + theme +
                ", algorithm=" + algorithm +
                ", rooms=" + rooms +
                ", erosionZones=" + erosionZones +
                ", gameVersion='" + gameVersion + '\'' +
                '}';
    }
}
