import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;

public class Grid extends JFrame {

    private PathfindingLogic logic;
    
    // UI Colors
    private final Color COLOR_BG = new Color(30, 30, 30);
    private final Color COLOR_START = new Color(46, 204, 113);
    private final Color COLOR_END = new Color(231, 76, 60);
    private final Color COLOR_PATH = new Color(52, 152, 219);
    private final Color COLOR_EXPLORED = new Color(155, 89, 182, 100);
    private final Color COLOR_FRONTIER = new Color(241, 196, 15, 100);

    private char currentBrush = 'S'; 
    
    private GridPanel gridPanel;
    
    // Controls
    private JComboBox<String> algoCombo;
    private JSlider speedSlider;
    private JSlider spinDisaster;
    private JComboBox<String> pointCombo;
    private JLabel lblTotalCost;
    
    // Animation state
    private Timer timer;
    private boolean isAnimating = false;
    private boolean isSimulationMode = false;

    public Grid() {
        super("Dynamic City Pathfinding Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COLOR_BG);
        
        logic = new PathfindingLogic(21, 31);
        
        gridPanel = new GridPanel();
        
        JPanel container = new JPanel(new GridBagLayout());
        container.setBackground(COLOR_BG);
        container.add(gridPanel);
        
        JScrollPane scrollPane = new JScrollPane(container);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(COLOR_BG);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.EAST);
        
        add(createLegendPanel(), BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        if(getWidth() > Toolkit.getDefaultToolkit().getScreenSize().width) {
            setSize(Toolkit.getDefaultToolkit().getScreenSize().width - 100, getHeight());
        }
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(40, 40, 40));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        addLabel(panel, "Algorithm:");
        algoCombo = new JComboBox<>(new String[]{"A* Search", "Dijkstra's", "Greedy Best-First", "Breadth-First (BFS)"});
        algoCombo.setMaximumSize(new Dimension(200, 30));
        panel.add(algoCombo);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        addLabel(panel, "Animation Delay (ms):");
        speedSlider = new JSlider(1, 100, 50);
        speedSlider.setBackground(new Color(40, 40, 40));
        speedSlider.setForeground(Color.WHITE);
        speedSlider.setMaximumSize(new Dimension(200, 40));
        speedSlider.addChangeListener(e -> {
            if (timer != null) timer.setDelay(speedSlider.getValue());
        });
        panel.add(speedSlider);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        addLabel(panel, "Set Position:");
        pointCombo = new JComboBox<>(new String[]{"Start (S)", "End (E)"});
        pointCombo.setMaximumSize(new Dimension(200, 30));
        pointCombo.addActionListener(e -> currentBrush = pointCombo.getSelectedIndex() == 0 ? 'S' : 'E');
        panel.add(pointCombo);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        addLabel(panel, "Disaster Rate:");
        spinDisaster = new JSlider(0, 50, 5);
        spinDisaster.setBackground(new Color(40, 40, 40));
        spinDisaster.setForeground(Color.WHITE);
        spinDisaster.setMaximumSize(new Dimension(200, 40));
        panel.add(spinDisaster);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));

        lblTotalCost = new JLabel("Total Cost: N/A");
        lblTotalCost.setForeground(new Color(241, 196, 15));
        lblTotalCost.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTotalCost.setFont(lblTotalCost.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(lblTotalCost);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        JButton btnFindPath = new JButton("Find Path");
        btnFindPath.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnFindPath.addActionListener(e -> startAlgorithm());
        panel.add(btnFindPath);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JButton btnSimulate = new JButton("Init Simulation");
        btnSimulate.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnSimulate.addActionListener(e -> startSimulation());
        panel.add(btnSimulate);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JButton btnNext = new JButton("Next Step");
        btnNext.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnNext.addActionListener(e -> {
            if (isSimulationMode) handleSimulationTick();
            else JOptionPane.showMessageDialog(this, "Click 'Init Simulation' first!");
        });
        panel.add(btnNext);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JButton btnClearPath = new JButton("Clear Path");
        btnClearPath.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClearPath.addActionListener(e -> clearPath());
        panel.add(btnClearPath);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JButton btnCity = new JButton("Generate City Map");
        btnCity.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnCity.addActionListener(e -> generateCityMap());
        panel.add(btnCity);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        
        JButton btnClearWalls = new JButton("Reset City");
        btnClearWalls.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnClearWalls.addActionListener(e -> {
            stopAnimation();
            logic.clearDisasters();
            clearPath();
            gridPanel.repaint();
        });
        panel.add(btnClearWalls);
        
        return panel;
    }
    
    private JPanel createLegendPanel() {
        JPanel legend = new JPanel();
        legend.setBackground(COLOR_BG);
        legend.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));

        legend.add(createLegendItem("Street", new Color(45, 45, 45)));
        legend.add(createLegendItem("Building", new Color(60, 60, 70)));
        legend.add(createLegendItem("Park", new Color(34, 139, 34)));
        legend.add(createLegendItem("Fire", new Color(230, 80, 0)));
        legend.add(createLegendItem("Flood", new Color(40, 120, 200)));
        legend.add(createLegendItem("Fallen Bldg", new Color(90, 60, 40)));
        legend.add(createLegendItem("Traffic", new Color(100, 100, 100)));
        legend.add(createLegendItem("Path", COLOR_PATH));
        legend.add(createLegendItem("Agent", Color.CYAN));

        return legend;
    }

    private JPanel createLegendItem(String labelText, Color color) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setBackground(COLOR_BG);
        
        JPanel colorBox = new JPanel();
        colorBox.setPreferredSize(new Dimension(15, 15));
        colorBox.setBackground(color);
        colorBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        JLabel label = new JLabel(labelText);
        label.setForeground(Color.WHITE);
        label.setFont(label.getFont().deriveFont(12f));
        
        panel.add(colorBox);
        panel.add(label);
        
        return panel;
    }
    
    private void addLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(label);
    }
    
    private void generateCityMap() {
        stopAnimation();
        logic.generateCityMap();
        lblTotalCost.setText("Total Cost: N/A");
        gridPanel.repaint();
    }
    
    private void clearPath() {
        stopAnimation();
        logic.clearPath();
        lblTotalCost.setText("Total Cost: N/A");
        gridPanel.repaint();
    }
    
    private void stopAnimation() {
        if (timer != null) timer.stop();
        isAnimating = false;
        isSimulationMode = false;
    }

    private void startAlgorithm() {
        clearPath();
        if (!logic.hasStartAndGoal()) {
            JOptionPane.showMessageDialog(this, "Need Start and End positions!");
            return;
        }

        int algo = algoCombo.getSelectedIndex();
        logic.startAlgorithm(algo);
        
        isAnimating = true;
        isSimulationMode = false;
        timer = new Timer(speedSlider.getValue(), e -> handleAlgorithmTick());
        timer.start();
    }

    private void startSimulation() {
        clearPath();
        if (!logic.hasStartAndGoal()) {
            JOptionPane.showMessageDialog(this, "Need Start and End positions!");
            return;
        }

        int algo = algoCombo.getSelectedIndex();
        int dRate = spinDisaster.getValue();
        
        logic.startSimulation(algo, dRate);
        
        isSimulationMode = true;
        isAnimating = false;
        if (timer != null) timer.stop();
        gridPanel.repaint();
    }

    private void handleAlgorithmTick() {
        PathfindingLogic.Status status = logic.step();
        gridPanel.repaint();
        
        if (status == PathfindingLogic.Status.FOUND) {
            timer.stop();
            isAnimating = false;
            int cost = logic.getCalculatedCost();
            int steps = logic.getFinalPath() != null ? logic.getFinalPath().size() - 1 : 0;
            lblTotalCost.setText("Total Cost: " + cost);
            JOptionPane.showMessageDialog(this, "Goal Reached!\nTotal Cost: " + cost + "\nPath Length (steps): " + steps);
        } else if (status == PathfindingLogic.Status.NO_PATH) {
            timer.stop();
            isAnimating = false;
            lblTotalCost.setText("Total Cost: No Path");
            JOptionPane.showMessageDialog(this, "No path found!");
        }
    }

    private void handleSimulationTick() {
        PathfindingLogic.Status status = logic.simulateStep();
        gridPanel.paintImmediately(0, 0, gridPanel.getWidth(), gridPanel.getHeight());
        
        if (status == PathfindingLogic.Status.SIMULATION_REACHED) {
            isSimulationMode = false;
            int cost = logic.getCalculatedCost();
            lblTotalCost.setText("Total Cost: " + cost);
            JOptionPane.showMessageDialog(this, "Agent reached the destination!\nTotal Cost: " + cost);
        } else if (status == PathfindingLogic.Status.SIMULATION_NO_PATH) {
            isSimulationMode = false;
            lblTotalCost.setText("Total Cost: No Path");
            JOptionPane.showMessageDialog(this, "Agent is completely blocked by disasters!");
        } else if (status == PathfindingLogic.Status.SIMULATION_REROUTING) {
            JOptionPane.showMessageDialog(this, "Disaster! Road blocked, rerouting...", "Warning", JOptionPane.WARNING_MESSAGE);
            logic.recalculatePathFromAgent();
            gridPanel.repaint();
        }
    }

    private class GridPanel extends JPanel {
        private final int CELL_SIZE = 30;

        public GridPanel() {
            setPreferredSize(new Dimension(logic.getCols() * CELL_SIZE, logic.getRows() * CELL_SIZE));
            setBackground(COLOR_BG);
            
            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) { paintCell(e); }
                @Override
                public void mouseDragged(MouseEvent e) { paintCell(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void paintCell(MouseEvent e) {
            if (isAnimating) return; 
            
            int c = e.getX() / CELL_SIZE;
            int r = e.getY() / CELL_SIZE;
            
            if (r >= 0 && r < logic.getRows() && c >= 0 && c < logic.getCols()) {
                logic.setCell(r, c, currentBrush);
                clearPath(); 
                repaint();
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int rows = logic.getRows();
            int cols = logic.getCols();

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = c * CELL_SIZE;
                    int y = r * CELL_SIZE;
                    char cell = logic.getCell(r, c);
                    
                    if (cell == 'X') {
                        // Building
                        g2.setColor(new Color(60, 60, 70));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        g2.setColor(new Color(40, 40, 50));
                        g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                        // Tiny windows to make it look like a building from above
                        g2.setColor(new Color(220, 220, 150, 150));
                        g2.fillRect(x + 5, y + 5, 8, 8);
                        g2.fillRect(x + 17, y + 5, 8, 8);
                        g2.fillRect(x + 5, y + 17, 8, 8);
                        g2.fillRect(x + 17, y + 17, 8, 8);
                    } else if (cell == 'R') {
                        // Park / Plaza
                        g2.setColor(new Color(34, 139, 34));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        g2.setColor(new Color(0, 100, 0));
                        g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                    } else if (cell == 'C' || cell == 'S' || cell == 'E') {
                        // Street
                        g2.setColor(new Color(45, 45, 45));
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                        g2.setColor(new Color(30, 30, 30));
                        g2.drawRect(x, y, CELL_SIZE, CELL_SIZE);
                        
                        // Dashed road lines
                        g2.setColor(new Color(255, 200, 0, 100)); // Yellow
                        boolean up = r > 0 && isStreet(logic.getCell(r-1, c));
                        boolean down = r < rows-1 && isStreet(logic.getCell(r+1, c));
                        boolean left = c > 0 && isStreet(logic.getCell(r, c-1));
                        boolean right = c < cols-1 && isStreet(logic.getCell(r, c+1));
                        
                        if (left && right && !up && !down) {
                            g2.fillRect(x + CELL_SIZE/4, y + CELL_SIZE/2 - 1, CELL_SIZE/2, 2);
                        } else if (up && down && !left && !right) {
                            g2.fillRect(x + CELL_SIZE/2 - 1, y + CELL_SIZE/4, 2, CELL_SIZE/2);
                        }
                    } else if (cell == 'F') {
                        g2.setColor(new Color(230, 80, 0)); // Fire
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    } else if (cell == 'B') {
                        g2.setColor(new Color(90, 60, 40)); // Building rubble
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    } else if (cell == 'W') {
                        g2.setColor(new Color(40, 120, 200)); // Flood
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    } else if (cell == 'T') {
                        g2.setColor(new Color(100, 100, 100)); // Traffic/Rubble
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    }
                    
                    String key = r + "," + c;
                    if (logic.getExploredSet().contains(key)) {
                        g2.setColor(COLOR_EXPLORED);
                        g2.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    } else if (logic.getFrontierSet().contains(key)) {
                        g2.setColor(COLOR_FRONTIER);
                        g2.fillRect(x + CELL_SIZE/4, y + CELL_SIZE/4, CELL_SIZE/2, CELL_SIZE/2);
                    }

                    if (cell == 'S') {
                        g2.setColor(COLOR_START);
                        g2.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                    } else if (cell == 'E') {
                        g2.setColor(COLOR_END);
                        g2.fillOval(x + 5, y + 5, CELL_SIZE - 10, CELL_SIZE - 10);
                    }
                }
            }
            
            java.util.List<int[]> path = logic.getFinalPath();
            if (path != null && path.size() > 1) {
                g2.setColor(COLOR_PATH);
                g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                for (int i = 0; i < path.size() - 1; i++) {
                    int[] p1 = path.get(i);
                    int[] p2 = path.get(i+1);
                    int x1 = p1[1] * CELL_SIZE + CELL_SIZE/2;
                    int y1 = p1[0] * CELL_SIZE + CELL_SIZE/2;
                    int x2 = p2[1] * CELL_SIZE + CELL_SIZE/2;
                    int y2 = p2[0] * CELL_SIZE + CELL_SIZE/2;
                    g2.drawLine(x1, y1, x2, y2);
                }
            }

            // Draw Agent
            int[] agent = logic.getAgentPos();
            if (agent != null) {
                g2.setColor(Color.CYAN);
                int ax = agent[1] * CELL_SIZE;
                int ay = agent[0] * CELL_SIZE;
                g2.fillOval(ax + 6, ay + 6, CELL_SIZE - 12, CELL_SIZE - 12);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(ax + 6, ay + 6, CELL_SIZE - 12, CELL_SIZE - 12);
            }
        }
        
        private boolean isStreet(char c) {
            return c == 'C' || c == 'S' || c == 'E' || c == 'F' || c == 'B' || c == 'W' || c == 'T';
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> {
            new Grid().setVisible(true);
        });
    }
}
