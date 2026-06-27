package wildfire;

import javax.swing.*;
import java.awt.*;

/**
 * Draws the simulation grid in a window.
 *
 * 
 * Each grid cell is drawn as a colored rectangle.
 *
 * Color legend:
 *   Tan/Yellow  = GRASS (open land)
 *   Dark Green  = FOREST (trees)
 *   Orange/Red  = BURNING (fire!)
 *   Dark Gray   = BURNED (ash)
 */
public class SimVisualizer extends JFrame {

    // How many pixels wide/tall each grid cell is
    private static final int CELL_SIZE = 6;

    // Delay between frames in milliseconds (lower = faster simulation)
    private static final int FRAME_DELAY_MS = 30;

    // Colors for each tile state
    private static final Color COLOR_GRASS   = new Color(210, 190, 140); // sandy tan
    private static final Color COLOR_FOREST  = new Color( 34, 100,  34); // dark green
    private static final Color COLOR_BURNING = new Color(230,  70,  10); // fire orange
    private static final Color COLOR_BURNED  = new Color( 60,  50,  40); // dark ash

    private final WildfireSimulation sim;
    private final GridPanel gridPanel;

    public SimVisualizer(WildfireSimulation sim) {
        this.sim = sim;

        setTitle("Wildfire Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gridPanel = new GridPanel();
        add(gridPanel);

        pack();
        setLocationRelativeTo(null); // center on screen
        setVisible(true);
    }

    /**
     * Redraws the grid and waits a short time so we can see the animation.
     */
    public void repaintAndWait() {
        // Ask Swing to repaint on the EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(gridPanel::repaint);

        try {
            Thread.sleep(FRAME_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 
    // Inner panel that does the actual drawing
    // 

    private class GridPanel extends JPanel {

        public GridPanel() {
            int width  = sim.getConfig().M * CELL_SIZE;
            int height = sim.getConfig().N * CELL_SIZE + 30; // +30 for tick label
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            TileState[][] grid = sim.getGrid();
            int N = sim.getConfig().N;
            int M = sim.getConfig().M;

            // Draw each tile
            for (int r = 0; r < N; r++) {
                for (int c = 0; c < M; c++) {
                    g.setColor(colorFor(grid[r][c]));
                    g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }

            // Draw tick counter at the bottom
            g.setColor(Color.WHITE);
            g.setFont(new Font("Monospaced", Font.BOLD, 13));
            g.drawString("Tick: " + sim.getTick(), 8, N * CELL_SIZE + 20);
        }

        private Color colorFor(TileState state) {
            switch (state) {
                case GRASS:   return COLOR_GRASS;
                case FOREST:  return COLOR_FOREST;
                case BURNING: return COLOR_BURNING;
                case BURNED:  return COLOR_BURNED;
                default:      return Color.MAGENTA; // should never happen
            }
        }
    }
}
