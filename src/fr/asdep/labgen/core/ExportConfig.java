package fr.asdep.labgen.core;

public class ExportConfig {
    private boolean exportSchematic;
    private String schematicName;
    private boolean exportImage;
    private String imageName;
    private boolean exportWorld;
    private String worldName;

    public ExportConfig() {
        this.exportSchematic = false;
        this.exportImage = false;
        this.exportWorld = false;
        this.schematicName = "labyrinth";
        this.imageName = "labyrinth";
        this.worldName = "labyrinth";
    }

    public boolean isExportSchematic() {
        return exportSchematic;
    }

    public void setExportSchematic(boolean exportSchematic) {
        this.exportSchematic = exportSchematic;
    }

    public String getSchematicName() {
        return schematicName;
    }

    public void setSchematicName(String schematicName) {
        this.schematicName = schematicName;
    }

    public boolean isExportImage() {
        return exportImage;
    }

    public void setExportImage(boolean exportImage) {
        this.exportImage = exportImage;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public boolean isExportWorld() {
        return exportWorld;
    }

    public void setExportWorld(boolean exportWorld) {
        this.exportWorld = exportWorld;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

}
