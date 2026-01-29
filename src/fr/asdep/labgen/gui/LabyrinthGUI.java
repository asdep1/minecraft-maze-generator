package fr.asdep.labgen.gui;

import fr.asdep.labgen.cli.Main;
import fr.asdep.labgen.core.*;
import fr.asdep.labgen.mc.Theme;
import fr.asdep.labgen.mc.ThemeLoader;
import fr.asdep.labgen.utils.ProgressBar;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
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

    public LabyrinthGUI() {
        setTitle("Labyrinth Generator for Minecraft");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 850);
        setLocationRelativeTo(null);

        initComponents();
        setupLayout();
        updateThemes();

        versionCombo.addActionListener(e -> updateThemes());
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
    }

    private void setupLayout() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

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
        configPanel.add(new JLabel("Largeur couloirs:"));
        configPanel.add(corridorWidthSpinner);
        configPanel.add(new JLabel("Largeur murs:"));
        configPanel.add(wallWidthSpinner);
        configPanel.add(new JLabel("Érosion (0.0 - 1.0):"));
        configPanel.add(erosionSpinner);
        configPanel.add(new JLabel("Plafond:"));
        configPanel.add(ceilingCheckBox);
        configPanel.add(new JLabel("Base Y:"));
        configPanel.add(baseYSpinner);
        configPanel.add(new JLabel("Algorithme:"));
        configPanel.add(algorithmCombo);
        configPanel.add(new JLabel("Thème:"));
        configPanel.add(themeCombo);

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

        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setBorder(BorderFactory.createTitledBorder("Progression"));
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(progressPanel, BorderLayout.CENTER);
        mainPanel.add(generateButton, BorderLayout.SOUTH);

        add(mainPanel);
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

    private void startGeneration() {
        generateButton.setEnabled(false);
        progressBar.setValue(0);
        progressLabel.setText("Démarrage...");

        GenerationConfig genConfig = new GenerationConfig();
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
        if (expConfig.isExportImage()) totalSteps++;
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
                Main.generateAndExport(genConfig, expConfig);
                SwingUtilities.invokeLater(() -> {
                    progressLabel.setText("Génération terminée !");
                    progressBar.setForeground(new Color(51, 204, 51));
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
}
