package wildfire;

import javax.swing.*;
import java.awt.*;


public class SimVisualizer extends JFrame {

    private static final int CELL_SIZE = 6;

    private static final int FRAME_DELAY_MS = 30;

    private static final Color COLOR_GRASS   = new Color(210, 190, 140); 
    private static final Color COLOR_FOREST  = new Color( 34, 100,  34); 
    private static final Color COLOR_BURNING = new Color(230,  70,  10); 
    private static final Color COLOR_BURNED  = new Color( 60,  50,  40); 
    private final WildfireSimulation sim;
    private final GridPanel gridPanel;

    public SimVisualizer(WildfireSimulation sim) {
        this.sim = sim;

        int rank = sim.getRank();
        setTitle("Wildfire - proces " + rank + " (vrstice " + sim.getRowStart() + "-" + (sim.getRowEnd() - 1) + ")");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        gridPanel = new GridPanel();
        add(gridPanel);

        pack();
        //okna po diagonali, da se ne prekrivajo popolnoma
        setLocation(60 + rank * 70, 60 + rank * 50);
        setVisible(true);
    }


    public void repaintAndWait() {
        // Ask Swing to repaint on the EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(gridPanel::repaint);

        try {
            Thread.sleep(FRAME_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    private class GridPanel extends JPanel {

        public GridPanel() {
            int width  = sim.getConfig().M * CELL_SIZE;
            int height = sim.getConfig().N * CELL_SIZE + 30; 
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            TileState[][] grid = sim.getGrid();
            int N = sim.getConfig().N;
            int M = sim.getConfig().M;

            for (int r = 0; r < N; r++) {
                for (int c = 0; c < M; c++) {
                    g.setColor(colorFor(grid[r][c]));
                    g.fillRect(c * CELL_SIZE, r * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }

            // ta proces jih ne posodablja 
            int rowStart = sim.getRowStart();
            int rowEnd   = sim.getRowEnd();
            Graphics2D g2 = (Graphics2D) g;
            g2.setColor(new Color(0, 0, 0, 140));
            if (rowStart > 0) {
                g2.fillRect(0, 0, M * CELL_SIZE, rowStart * CELL_SIZE);
            }
            if (rowEnd < N) {
                g2.fillRect(0, rowEnd * CELL_SIZE, M * CELL_SIZE, (N - rowEnd) * CELL_SIZE);
            }

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
                default:      return Color.MAGENTA; 
            }
        }
    }
}
