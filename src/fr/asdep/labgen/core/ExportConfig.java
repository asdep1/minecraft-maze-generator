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
    public String getSchematicName() {
        return schematicName;
    }

    public boolean isExportImage() {
        return exportImage;
    }
    public String getImageName() {
        return imageName;
    }

    public boolean isExportWorld() {
        return exportWorld;
    }
    public String getWorldName() {
        return worldName;
    }

    public void setExportSchematic(boolean exportSchematic) {
        this.exportSchematic = exportSchematic;
    }
    public void setSchematicName(String schematicName) {
        this.schematicName = schematicName;
    }
    public void setExportImage(boolean exportImage) {
        this.exportImage = exportImage;
    }
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    public void setExportWorld(boolean exportWorld) {
        this.exportWorld = exportWorld;
    }
    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

}
