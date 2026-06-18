package fr.asdep.labgen.gui;

import fr.asdep.labgen.cli.Main;
import fr.asdep.labgen.core.*;
import fr.asdep.labgen.exporter.ImageExporter;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.mc.ThemeLoader;
import fr.asdep.labgen.utils.ProgressBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LabyrinthGUI extends JFrame {

    private JComboBox<String> versionCombo;
    private JSpinner widthSpinner;
    private JSpinner depthSpinner;
    private JSpinner heightSpinner;
    private JSpinner corridorWidthSpinner;
    private JSpinner wallWidthSpinner;
    private JSpinner erosionSpinner;
    private JCheckBox ceilingCheckBox;
    private JSpinner baseYSpinner;
    private JComboBox<String> themeCombo;
    private JComboBox<String> algorithmCombo;
    private JLabel sizeLabel;

    private DefaultListModel<Room> roomListModel;
    private JList<Room> roomList;
    private DefaultListModel<ErosionZone> erosionZoneListModel;
    private JList<ErosionZone> erosionZoneList;

    private JCheckBox exportSchematicCheck;
    private JTextField schematicNameField;
    private JCheckBox exportImageCheck;
    private JTextField imageNameField;
    private JCheckBox exportWorldCheck;
    private JTextField worldNameField;

    private JButton generateButton;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private ImagePanel previewPanel;
    private BufferedImage currentPreviewImage;
    private GenerationConfig currentConfig;

    public LabyrinthGUI() {
        setTitle("Labyrinth Generator for Minecraft");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 850);
        setLocationRelativeTo(null);

        initComponents();
        setupLayout();
        updateThemes();
        setupSizeListeners();
        updateSizeLabel();

        versionCombo.addActionListener(e -> updateThemes());

        // Listeners for preview
        previewPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                if (currentPreviewImage != null) {
                    previewPanel.resetView();
                    previewPanel.repaint();
                }
            }
        });
    }

    private void updatePreview() {
        previewPanel.setImage(currentPreviewImage, currentConfig);
    }

    private void initComponents() {

        versionCombo = new JComboBox<>(getAvailableVersions());
        widthSpinner = new JSpinner(new SpinnerNumberModel(250, 1, 2000, 1));
        depthSpinner = new JSpinner(new SpinnerNumberModel(250, 1, 2000, 1));
        heightSpinner = new JSpinner(new SpinnerNumberModel(25, 1, 255, 1));
        corridorWidthSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        wallWidthSpinner = new JSpinner(new SpinnerNumberModel(7, 1, 100, 1));
        erosionSpinner = new JSpinner(new SpinnerNumberModel(0.2, 0.0, 1.0, 0.05));
        ceilingCheckBox = new JCheckBox("Plafond activé", false);
        baseYSpinner = new JSpinner(new SpinnerNumberModel(64, -64, 320, 1));
        themeCombo = new JComboBox<>();

        List<String> algorithms = MazeAlgorithm.listAlgorithms();
        algorithmCombo = new JComboBox<>(algorithms.toArray(new String[0]));

        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Room) {
                    Room r = (Room) value;
                    setText(String.format("Salle: [%d, %d] %dx%d (%d entrées)", r.getX(), r.getZ(), r.getWidth(), r.getDepth(), r.getEntrances()));
                }
                return this;
            }
        });

        erosionZoneListModel = new DefaultListModel<>();
        erosionZoneList = new JList<>(erosionZoneListModel);
        erosionZoneList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ErosionZone) {
                    ErosionZone ez = (ErosionZone) value;
                    setText(String.format("Érosion: [%d, %d] %dx%d (Facteur: %.2f)", ez.getX(), ez.getZ(), ez.getWidth(), ez.getDepth(), ez.getFactor()));
                }
                return this;
            }
        });

        exportSchematicCheck = new JCheckBox("Exporter Schematic", true);
        schematicNameField = new JTextField("labyrinth");
        exportImageCheck = new JCheckBox("Exporter Image", false);
        imageNameField = new JTextField("labyrinth");
        exportWorldCheck = new JCheckBox("Exporter Monde", false);
        worldNameField = new JTextField("labyrinth");

        generateButton = new JButton("Générer le labyrinthe");
        generateButton.setFont(new Font("Arial", Font.BOLD, 14));
        generateButton.addActionListener(e -> startGeneration());

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressLabel = new JLabel("Prêt");
        sizeLabel = new JLabel("Taille: 0 x 0 x 0");
        sizeLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        sizeLabel.setHorizontalAlignment(SwingConstants.CENTER);

        previewPanel = new ImagePanel(this);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        SwingUtilities.invokeLater(() -> {
            new LabyrinthGUI().setVisible(true);
        });
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));

        JPanel configPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        configPanel.setBorder(BorderFactory.createTitledBorder("Configuration"));

        configPanel.add(new JLabel("Version du jeu:"));
        configPanel.add(versionCombo);
        configPanel.add(new JLabel("Largeur (cellules):"));
        configPanel.add(widthSpinner);
        configPanel.add(new JLabel("Profondeur (cellules):"));
        configPanel.add(depthSpinner);
        configPanel.add(new JLabel("Hauteur des murs (blocs):"));
        configPanel.add(heightSpinner);
        configPanel.add(new JLabel("Largeur des couloirs:"));
        configPanel.add(corridorWidthSpinner);
        configPanel.add(new JLabel("Epaisseur des murs:"));
        configPanel.add(wallWidthSpinner);
        configPanel.add(new JLabel("Érosion (0.0 - 1.0):"));
        configPanel.add(erosionSpinner);
        configPanel.add(new JLabel("Plafond:"));
        configPanel.add(ceilingCheckBox);
        configPanel.add(new JLabel("Base Y (export monde) :"));
        configPanel.add(baseYSpinner);
        configPanel.add(new JLabel("Algorithme:"));
        configPanel.add(algorithmCombo);
        configPanel.add(new JLabel("Thème:"));
        configPanel.add(themeCombo);
        configPanel.add(new JLabel("")); // maze size placeholder
        configPanel.add(sizeLabel);

        JPanel advancedPanel = new JPanel(new GridLayout(1, 2, 10, 5));
        advancedPanel.setBorder(BorderFactory.createTitledBorder("Salles et Érosion"));

        // Room Panel
        JPanel roomPanel = new JPanel(new BorderLayout());
        roomPanel.add(new JLabel("Salles:"), BorderLayout.NORTH);
        roomPanel.add(new JScrollPane(roomList), BorderLayout.CENTER);
        JPanel roomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addRoomBtn = new JButton("+");
        JButton removeRoomBtn = new JButton("-");
        addRoomBtn.addActionListener(e -> addRoomDialog());
        removeRoomBtn.addActionListener(e -> {
            int idx = roomList.getSelectedIndex();
            if (idx != -1) roomListModel.remove(idx);
        });
        roomList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = roomList.locationToIndex(e.getPoint());
                    if (idx != -1) editRoomDialog(idx);
                }
            }
        });
        roomButtons.add(addRoomBtn);
        roomButtons.add(removeRoomBtn);
        roomPanel.add(roomButtons, BorderLayout.SOUTH);

        // Erosion Zone Panel
        JPanel erosionPanel = new JPanel(new BorderLayout());
        erosionPanel.add(new JLabel("Zones d'érosion:"), BorderLayout.NORTH);
        erosionPanel.add(new JScrollPane(erosionZoneList), BorderLayout.CENTER);
        JPanel erosionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addErosionBtn = new JButton("+");
        JButton removeErosionBtn = new JButton("-");
        addErosionBtn.addActionListener(e -> addErosionDialog());
        removeErosionBtn.addActionListener(e -> {
            int idx = erosionZoneList.getSelectedIndex();
            if (idx != -1) erosionZoneListModel.remove(idx);
        });
        erosionZoneList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int idx = erosionZoneList.locationToIndex(e.getPoint());
                    if (idx != -1) editErosionDialog(idx);
                }
            }
        });
        erosionButtons.add(addErosionBtn);
        erosionButtons.add(removeErosionBtn);
        erosionPanel.add(erosionButtons, BorderLayout.SOUTH);

        advancedPanel.add(roomPanel);
        advancedPanel.add(erosionPanel);

        JPanel exportPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        exportPanel.setBorder(BorderFactory.createTitledBorder("Exportation"));

        exportPanel.add(exportSchematicCheck);
        exportPanel.add(schematicNameField);
        exportPanel.add(exportImageCheck);
        exportPanel.add(imageNameField);
        exportPanel.add(exportWorldCheck);
        exportPanel.add(worldNameField);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(configPanel, BorderLayout.NORTH);
        topPanel.add(advancedPanel, BorderLayout.CENTER);
        topPanel.add(exportPanel, BorderLayout.SOUTH);

        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.setPreferredSize(new Dimension(400, 0));

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("Aperçu"));
        rightPanel.add(previewPanel, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(BorderFactory.createTitledBorder("Progression"));
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(progressPanel, BorderLayout.NORTH);
        bottomPanel.add(generateButton, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void editRoomDialog(int index) {
        Room room = roomListModel.get(index);
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JSpinner xSp = new JSpinner(new SpinnerNumberModel(room.getX(), 0, 2000, 1));
        JSpinner zSp = new JSpinner(new SpinnerNumberModel(room.getZ(), 0, 2000, 1));
        JSpinner wSp = new JSpinner(new SpinnerNumberModel(room.getWidth(), 1, 100, 1));
        JSpinner dSp = new JSpinner(new SpinnerNumberModel(room.getDepth(), 1, 100, 1));
        JSpinner eSp = new JSpinner(new SpinnerNumberModel(room.getEntrances(), 1, 20, 1));

        panel.add(new JLabel("X (cellule):"));
        panel.add(xSp);
        panel.add(new JLabel("Z (cellule):"));
        panel.add(zSp);
        panel.add(new JLabel("Largeur:"));
        panel.add(wSp);
        panel.add(new JLabel("Profondeur:"));
        panel.add(dSp);
        panel.add(new JLabel("Entrées:"));
        panel.add(eSp);

        int result = JOptionPane.showConfirmDialog(this, panel, "Modifier la salle", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            room.setX((Integer) xSp.getValue());
            room.setZ((Integer) zSp.getValue());
            room.setWidth((Integer) wSp.getValue());
            room.setDepth((Integer) dSp.getValue());
            room.setEntrances((Integer) eSp.getValue());
            roomListModel.set(index, room);
            if (currentConfig != null) previewPanel.repaint();
        }
    }

    private void updateThemes() {
        String version = (String) versionCombo.getSelectedItem();
        themeCombo.removeAllItems();
        try {
            List<String> themes = ThemeLoader.listThemes(version);
            for (String theme : themes) {
                themeCombo.addItem(theme);
            }
            if (themes.contains("stone")) {
                themeCombo.setSelectedItem("stone");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des thèmes: " + e.getMessage());
        }
    }

    private void addRoomDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JSpinner xSp = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 1));
        JSpinner zSp = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 1));
        JSpinner wSp = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        JSpinner dSp = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        JSpinner eSp = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));

        panel.add(new JLabel("X (cellule):"));
        panel.add(xSp);
        panel.add(new JLabel("Z (cellule):"));
        panel.add(zSp);
        panel.add(new JLabel("Largeur:"));
        panel.add(wSp);
        panel.add(new JLabel("Profondeur:"));
        panel.add(dSp);
        panel.add(new JLabel("Entrées:"));
        panel.add(eSp);

        int result = JOptionPane.showConfirmDialog(this, panel, "Ajouter une salle", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            roomListModel.addElement(new Room((Integer) xSp.getValue(), (Integer) zSp.getValue(), (Integer) wSp.getValue(), (Integer) dSp.getValue(), (Integer) eSp.getValue()));
        }
    }

    private void editErosionDialog(int index) {
        ErosionZone zone = erosionZoneListModel.get(index);
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JSpinner xSp = new JSpinner(new SpinnerNumberModel(zone.getX(), 0, 2000, 1));
        JSpinner zSp = new JSpinner(new SpinnerNumberModel(zone.getZ(), 0, 2000, 1));
        JSpinner wSp = new JSpinner(new SpinnerNumberModel(zone.getWidth(), 1, 200, 1));
        JSpinner dSp = new JSpinner(new SpinnerNumberModel(zone.getDepth(), 1, 200, 1));
        JSpinner fSp = new JSpinner(new SpinnerNumberModel(zone.getFactor(), 0.0, 1.0, 0.05));

        panel.add(new JLabel("X (cellule):"));
        panel.add(xSp);
        panel.add(new JLabel("Z (cellule):"));
        panel.add(zSp);
        panel.add(new JLabel("Largeur:"));
        panel.add(wSp);
        panel.add(new JLabel("Profondeur:"));
        panel.add(dSp);
        panel.add(new JLabel("Facteur d'érosion:"));
        panel.add(fSp);

        int result = JOptionPane.showConfirmDialog(this, panel, "Modifier la zone d'érosion", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            zone.setX((Integer) xSp.getValue());
            zone.setZ((Integer) zSp.getValue());
            zone.setWidth((Integer) wSp.getValue());
            zone.setDepth((Integer) dSp.getValue());
            zone.setFactor(((Double) fSp.getValue()).floatValue());
            erosionZoneListModel.set(index, zone);
            if (currentConfig != null) previewPanel.repaint();
        }
    }

    private void addErosionDialog() {
        JPanel panel = new JPanel(new GridLayout(0, 2));
        JSpinner xSp = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 1));
        JSpinner zSp = new JSpinner(new SpinnerNumberModel(0, 0, 2000, 1));
        JSpinner wSp = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
        JSpinner dSp = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));
        JSpinner fSp = new JSpinner(new SpinnerNumberModel(0.5, 0.0, 1.0, 0.05));

        panel.add(new JLabel("X (cellule):"));
        panel.add(xSp);
        panel.add(new JLabel("Z (cellule):"));
        panel.add(zSp);
        panel.add(new JLabel("Largeur:"));
        panel.add(wSp);
        panel.add(new JLabel("Profondeur:"));
        panel.add(dSp);
        panel.add(new JLabel("Facteur d'érosion:"));
        panel.add(fSp);

        int result = JOptionPane.showConfirmDialog(this, panel, "Ajouter une zone d'érosion", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            erosionZoneListModel.addElement(new ErosionZone((Integer) xSp.getValue(), (Integer) zSp.getValue(), (Integer) wSp.getValue(), (Integer) dSp.getValue(), ((Double) fSp.getValue()).floatValue()));
        }
    }

    private void startGeneration() {
        currentPreviewImage = null;
        generateButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setForeground(new Color(51, 153, 255));
        progressLabel.setText("Démarrage...");

        GenerationConfig genConfig = new GenerationConfig();
        currentConfig = genConfig;
        ExportConfig expConfig = new ExportConfig();

        // Remplissage des configs
        genConfig.setGameVersion((String) versionCombo.getSelectedItem());
        genConfig.setWidth((Integer) widthSpinner.getValue());
        genConfig.setDepth((Integer) depthSpinner.getValue());
        genConfig.setHeight((Integer) heightSpinner.getValue());
        genConfig.setCorridorWidth((Integer) corridorWidthSpinner.getValue());
        genConfig.setWallWidth((Integer) wallWidthSpinner.getValue());
        genConfig.setErosion(((Double) erosionSpinner.getValue()).floatValue());
        genConfig.setCeilingEnabled(ceilingCheckBox.isSelected());
        genConfig.setBaseY((Integer) baseYSpinner.getValue());
        genConfig.setAlgorithm(MazeAlgorithm.fromName((String) algorithmCombo.getSelectedItem()));

        for (int i = 0; i < roomListModel.size(); i++) {
            genConfig.addRoom(roomListModel.get(i));
        }
        for (int i = 0; i < erosionZoneListModel.size(); i++) {
            genConfig.addErosionZone(erosionZoneListModel.get(i));
        }

        String themeName = (String) themeCombo.getSelectedItem();
        try {
            genConfig.setTheme(ThemeLoader.loadTheme(genConfig.getGameVersion(), themeName));
        } catch (IOException e) {
            System.err.println("Erreur thème: " + e.getMessage() + ". Utilisation du défaut.");
            genConfig.setTheme(Theme.getDefault());
        }

        expConfig.setExportSchematic(exportSchematicCheck.isSelected());
        expConfig.setSchematicName(schematicNameField.getText());
        expConfig.setExportImage(exportImageCheck.isSelected());
        expConfig.setImageName(imageNameField.getText());
        expConfig.setExportWorld(exportWorldCheck.isSelected());
        expConfig.setWorldName(worldNameField.getText());

        // Progress state
        final int[] currentStep = {0};
        final String[] lastTask = {""};

        int totalSteps = 4; // Structure Logique, Sol/Plafond, Voxelisation, Bordures
        if (genConfig.getErosion() > 0 || !genConfig.getErosionZones().isEmpty()) totalSteps++;
        if (!genConfig.getRooms().isEmpty()) totalSteps++;
        if (expConfig.isExportSchematic()) totalSteps += 2;
        totalSteps++;
        if (expConfig.isExportWorld()) totalSteps++;

        final int finalTotalSteps = totalSteps;

        ProgressBar.setListener((task, curr, tot) -> {
            if (!task.equals(lastTask[0])) {
                currentStep[0]++;
                lastTask[0] = task;
            }
            SwingUtilities.invokeLater(() -> {
                progressLabel.setText(String.format("[%d/%d] %s", currentStep[0], finalTotalSteps, task));
                progressBar.setValue((int) (((float) curr / tot) * 100));
            });
        });

        new Thread(() -> {
            try {
                MazeGenerator generator = Main.generateAndExport(genConfig, expConfig);

                currentPreviewImage = Main.getLastGeneratedImage();
                if (currentPreviewImage == null) {
                    ProgressBar pb = new ProgressBar("Rendu de l'image", 100);
                    currentPreviewImage = ImageExporter.generateImage(generator);
                    pb.step();
                }

                SwingUtilities.invokeLater(() -> {
                    progressLabel.setText("Génération terminée !");
                    progressBar.setValue(100);
                    progressBar.setForeground(new Color(51, 204, 51));
                    updatePreview();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    progressLabel.setText("ERREUR: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Erreur: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                });
                e.printStackTrace();
            } finally {
                SwingUtilities.invokeLater(() -> generateButton.setEnabled(true));
                ProgressBar.setListener(null);
            }
        }).start();
    }

    private void setupSizeListeners() {
        javax.swing.event.ChangeListener cl = e -> updateSizeLabel();
        widthSpinner.addChangeListener(cl);
        depthSpinner.addChangeListener(cl);
        heightSpinner.addChangeListener(cl);
        corridorWidthSpinner.addChangeListener(cl);
        wallWidthSpinner.addChangeListener(cl);
        ceilingCheckBox.addActionListener(e -> updateSizeLabel());
    }

    private void updateSizeLabel() {
        int w = (Integer) widthSpinner.getValue();
        int d = (Integer) depthSpinner.getValue();
        int h = (Integer) heightSpinner.getValue();
        int cw = (Integer) corridorWidthSpinner.getValue();
        int ww = (Integer) wallWidthSpinner.getValue();
        boolean ceiling = ceilingCheckBox.isSelected();

        int totalX = w * (cw + ww) + ww;
        int totalZ = d * (cw + ww) + ww;
        int totalY = h + (ceiling ? 2 : 1);

        sizeLabel.setText(String.format("Taille totale: %d x %d x %d (blocs)", totalX, totalY, totalZ));
    }

    private String[] getAvailableVersions() {
        File versionsDir = new File("versions");
        if (!versionsDir.exists() || !versionsDir.isDirectory()) {
            return new String[]{"1.12.2"};
        }

        File[] folders = versionsDir.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            return new String[]{"1.12.2"};
        }

        List<String> versionList = new ArrayList<>();
        for (File folder : folders) {
            versionList.add(folder.getName());
        }

        return versionList.toArray(new String[0]);
    }

    private static class ImagePanel extends JPanel {
        private BufferedImage image;
        private double zoom = 1.0;
        private double offsetX = 0;
        private double offsetY = 0;
        private Point lastMousePos;
        private Point currentMousePos;
        private GenerationConfig config;

        private Room selectedRoom;
        private ErosionZone selectedErosionZone;
        private boolean isDragging = false;
        private int startCellX, startCellZ;
        private int startObjX, startObjZ;

        private final LabyrinthGUI gui;

        public ImagePanel(LabyrinthGUI gui) {
            this.gui = gui;
            setBackground(Color.DARK_GRAY);
            setBorder(BorderFactory.createLineBorder(Color.GRAY));

            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    lastMousePos = e.getPoint();
                    isDragging = false;
                    selectedRoom = null;
                    selectedErosionZone = null;

                    if (config != null && image != null) {
                        double internalScale = getInternalScale();
                        int cw = config.getCorridorWidth();
                        int ww = config.getWallWidth();
                        int cellStep = cw + ww;

                        int imgX = (int) ((e.getX() - offsetX) / zoom);
                        int imgZ = (int) ((e.getY() - offsetY) / zoom);
                        int realX = (int) (imgX / internalScale);
                        int realZ = (int) (imgZ / internalScale);
                        int cellX = realX / cellStep;
                        int cellZ = realZ / cellStep;

                        for (int i = config.getRooms().size() - 1; i >= 0; i--) {
                            Room r = config.getRooms().get(i);
                            if (cellX >= r.getX() && cellX < r.getX() + r.getWidth() &&
                                cellZ >= r.getZ() && cellZ < r.getZ() + r.getDepth()) {
                                selectedRoom = r;
                                break;
                            }
                        }

                        if (selectedRoom == null) {
                            for (int i = config.getErosionZones().size() - 1; i >= 0; i--) {
                                ErosionZone ez = config.getErosionZones().get(i);
                                if (cellX >= ez.getX() && cellX < ez.getX() + ez.getWidth() &&
                                    cellZ >= ez.getZ() && cellZ < ez.getZ() + ez.getDepth()) {
                                    selectedErosionZone = ez;
                                    break;
                                }
                            }
                        }

                        if (selectedRoom != null || selectedErosionZone != null) {
                            isDragging = true;
                            startCellX = cellX;
                            startCellZ = cellZ;
                            if (selectedRoom != null) {
                                startObjX = selectedRoom.getX();
                                startObjZ = selectedRoom.getZ();
                                gui.roomList.setSelectedValue(selectedRoom, true);
                                gui.erosionZoneList.clearSelection();
                            } else {
                                startObjX = selectedErosionZone.getX();
                                startObjZ = selectedErosionZone.getZ();
                                gui.erosionZoneList.setSelectedValue(selectedErosionZone, true);
                                gui.roomList.clearSelection();
                            }
                        }
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isDragging && config != null) {
                        double internalScale = getInternalScale();
                        int cw = config.getCorridorWidth();
                        int ww = config.getWallWidth();
                        int cellStep = cw + ww;

                        int imgX = (int) ((e.getX() - offsetX) / zoom);
                        int imgZ = (int) ((e.getY() - offsetY) / zoom);
                        int realX = (int) (imgX / internalScale);
                        int realZ = (int) (imgZ / internalScale);
                        int cellX = realX / cellStep;
                        int cellZ = realZ / cellStep;

                        int dx = cellX - startCellX;
                        int dz = cellZ - startCellZ;

                        if (selectedRoom != null) {
                            selectedRoom.setX(startObjX + dx);
                            selectedRoom.setZ(startObjZ + dz);
                        } else if (selectedErosionZone != null) {
                            selectedErosionZone.setX(startObjX + dx);
                            selectedErosionZone.setZ(startObjZ + dz);
                        }
                        repaint();
                    } else if (lastMousePos != null) {
                        double deltaX = e.getX() - lastMousePos.x;
                        double deltaY = e.getY() - lastMousePos.y;
                        offsetX += deltaX;
                        offsetY += deltaY;
                        lastMousePos = e.getPoint();
                        currentMousePos = e.getPoint();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (isDragging) {
                        isDragging = false;
                        selectedRoom = null;
                        selectedErosionZone = null;
                        gui.roomList.repaint();
                        gui.erosionZoneList.repaint();
                    }
                }

                @Override
                public void mouseMoved(MouseEvent e) {
                    currentMousePos = e.getPoint();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    currentMousePos = null;
                    repaint();
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    double zoomFactor = (e.getWheelRotation() < 0) ? 1.1 : 0.9;

                    double mouseX = e.getX();
                    double mouseY = e.getY();

                    double newZoom = zoom * zoomFactor;

                    if (newZoom < 0.01) newZoom = 0.01;
                    if (newZoom > 100.0) newZoom = 100.0;

                    offsetX = mouseX - (mouseX - offsetX) * (newZoom / zoom);
                    offsetY = mouseY - (mouseY - offsetY) * (newZoom / zoom);

                    zoom = newZoom;
                    repaint();
                }
            };

            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
            addMouseWheelListener(mouseAdapter);
        }

        public void setImage(BufferedImage image, GenerationConfig config) {
            this.image = image;
            this.config = config;
            resetView();
            repaint();
        }

        public void resetView() {
            if (image == null) return;

            double scaleW = (double) getWidth() / image.getWidth();
            double scaleH = (double) getHeight() / image.getHeight();
            zoom = Math.min(scaleW, scaleH);
            if (zoom <= 0) zoom = 1.0;

            offsetX = (getWidth() - image.getWidth() * zoom) / 2;
            offsetY = (getHeight() - image.getHeight() * zoom) / 2;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) {
                g.setColor(Color.WHITE);
                String msg = "L'aperçu apparaîtra ici après la génération";
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(msg)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                g.drawString(msg, x, y);
                return;
            }

            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.translate(offsetX, offsetY);
            g2d.scale(zoom, zoom);

            double internalScale = getInternalScale();

            g2d.drawImage(image, 0, 0, null);

            if (config != null) {
                Graphics2D overlayG2d = (Graphics2D) g2d.create();
                overlayG2d.scale(internalScale, internalScale);

                int cw = config.getCorridorWidth();
                int ww = config.getWallWidth();
                int cellStep = cw + ww;

                for (ErosionZone zone : config.getErosionZones()) {
                    int vx = zone.getX() * cellStep;
                    int vz = zone.getZ() * cellStep;
                    int vw = zone.getWidth() * cellStep + ww;
                    int vd = zone.getDepth() * cellStep + ww;
                    overlayG2d.setColor(new Color(255, 0, 0, 100));
                    overlayG2d.fillRect(vx, vz, vw, vd);
                    overlayG2d.setColor(Color.RED);
                    overlayG2d.drawRect(vx, vz, vw, vd);
                }

                for (fr.asdep.labgen.core.Room room : config.getRooms()) {
                    int vx = room.getX() * cellStep;
                    int vz = room.getZ() * cellStep;
                    int vw = room.getWidth() * cellStep + ww;
                    int vd = room.getDepth() * cellStep + ww;
                    overlayG2d.setColor(new Color(0, 0, 255, 100));
                    overlayG2d.fillRect(vx, vz, vw, vd);
                    overlayG2d.setColor(Color.BLUE);
                    overlayG2d.drawRect(vx, vz, vw, vd);
                }
                overlayG2d.dispose();
            }

            g2d.dispose();

            if (currentMousePos != null && config != null) {
                int imgX = (int) ((currentMousePos.x - offsetX) / zoom);
                int imgZ = (int) ((currentMousePos.y - offsetY) / zoom);

                if (imgX >= 0 && imgX < image.getWidth() && imgZ >= 0 && imgZ < image.getHeight()) {
                    int realX = (int) (imgX / internalScale);
                    int realZ = (int) (imgZ / internalScale);

                    int cw = config.getCorridorWidth();
                    int ww = config.getWallWidth();
                    int cellStep = cw + ww;

                    int cellX = realX / cellStep;
                    int cellZ = realZ / cellStep;

                    // Minecraft coords (X, Z)
                    int mcX = realX;
                    int mcZ = realZ;
                    int mcY = config.getBaseY();

                    String coordText = String.format("Cellule: [%d, %d] | Minecraft: %d, %d, %d",
                            cellX, cellZ, mcX, mcY, mcZ);

                    g.setColor(new Color(0, 0, 0, 150));
                    g.fillRect(5, getHeight() - 25, g.getFontMetrics().stringWidth(coordText) + 10, 20);
                    g.setColor(Color.WHITE);
                    g.drawString(coordText, 10, getHeight() - 10);
                }
            }
        }

        private double getInternalScale() {
            if (config == null) return 1.0;
            int origWidth = config.getTotalWidth();
            int origDepth = config.getTotalDepth();
            if (origWidth > 4000 || origDepth > 4000) {
                return Math.min(4000.0 / origWidth, 4000.0 / origDepth);
            }
            return 1.0;
        }
    }
}
