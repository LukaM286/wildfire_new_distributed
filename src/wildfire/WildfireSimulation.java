package wildfire;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 
 *
 * Holds the grid and runs the tick-by-tick fire spreading logic.
 * Everything here is sequential (single-threaded)
 */
public class WildfireSimulation {

    private final SimConfig config;
    private final Random    rng;

    // The grid: grid[row][col] = current state of that tile
    private TileState[][] grid;

    // burnTimer[row][col] = how many ticks this tile has been burning
    // (0 if not burning)
    private int[][] burnTimer;

    // Current simulation tick
    private int tick;

    public WildfireSimulation(SimConfig config) {
        this.config    = config;
        this.rng       = new Random(config.seed);
        this.grid      = new TileState[config.N][config.M];
        this.burnTimer = new int[config.N][config.M];
        this.tick      = 0;

        // Initialize everything as GRASS
        for (int r = 0; r < config.N; r++) {
            for (int c = 0; c < config.M; c++) {
                grid[r][c] = TileState.GRASS;
            }
        }
    }

    // 
    // STEP 1: Generate the forest using a random walk
    // 

    /**
     * Walks randomly across the grid, marking tiles as FOREST.
     * Keeps walking until 50% of all tiles are forest.
     */
    public void generateForest() {
        int totalTiles  = config.N * config.M;
        int targetForest = totalTiles / 2; // 50%

        // Start from a random tile
        int row = rng.nextInt(config.N);
        int col = rng.nextInt(config.M);

        int forestCount = 0;

        // Direction arrays: up, down, left, right
        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = { 0, 0,-1, 1};

        while (forestCount < targetForest) {
            // Mark this tile as forest if it isn't already
            if (grid[row][col] == TileState.GRASS) {
                grid[row][col] = TileState.FOREST;
                forestCount++;
            }

            // Pick a random direction to step
            int dir = rng.nextInt(4);
            int newRow = row + dRow[dir];
            int newCol = col + dCol[dir];

            // Stay inside the grid (bounce off walls)
            if (newRow >= 0 && newRow < config.N && newCol >= 0 && newCol < config.M) {
                row = newRow;
                col = newCol;
            }
            // If we'd go out of bounds, we just stay put and try again next iteration
        }

        System.out.printf("Forest generated: %d tiles (%.1f%%)%n",
            forestCount, 100.0 * forestCount / totalTiles);
    }

    // 
    // STEP 2: Start the fire at K random forest tiles
    // 

    /**
     * Picks K random FOREST tiles and sets them on FIRE.
     */
    public void igniteRandomTiles() {
        // Collect all forest tile positions
        List<int[]> forestTiles = new ArrayList<>();
        for (int r = 0; r < config.N; r++) {
            for (int c = 0; c < config.M; c++) {
                if (grid[r][c] == TileState.FOREST) {
                    forestTiles.add(new int[]{r, c});
                }
            }
        }

        // Clamp K: can't ignite more tiles than we have forest
        int ignitions = Math.min(config.K, forestTiles.size());

        // Shuffle and pick the first `ignitions` tiles
        Collections.shuffle(forestTiles, rng);
        for (int i = 0; i < ignitions; i++) {
            int r = forestTiles.get(i)[0];
            int c = forestTiles.get(i)[1];
            grid[r][c]      = TileState.BURNING;
            burnTimer[r][c] = 1;
        }

        System.out.printf("Fire started at %d tiles%n", ignitions);
    }

    // 
    // STEP 3: Run the simulation
    // 

    /**
     * Runs tick by tick until no tile is burning anymore.
     * After each tick, calls the visualizer to redraw the screen.
     */
    public void run(SimVisualizer visualizer) {
        while (hasBurningTile()) {
            tick++;
            doTick();

            if (visualizer != null) {
                visualizer.repaintAndWait();
            }
        }
    }

    /**
     * One tick of the simulation:
     *   1. Check which FOREST neighbors of BURNING tiles should ignite.
     *   2. Advance burn timers; tiles that finish burning become BURNED.
     */
    private void doTick() {
        // We need a separate "next state" grid so that changes in this tick
        // don't immediately affect the same tick's spread calculation.
        // (We update burnTimer in-place since it's only read by the "did it burn out?" check.)

        boolean[][] shouldIgnite = new boolean[config.N][config.M];

        // Phase 1: Decide which forest tiles catch fire this tick
        for (int r = 0; r < config.N; r++) {
            for (int c = 0; c < config.M; c++) {
                if (grid[r][c] == TileState.BURNING) {
                    // Look at all 8 neighbors (diagonals included)
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue; // skip self

                            int nr = r + dr;
                            int nc = c + dc;

                            // Check bounds
                            if (nr < 0 || nr >= config.N || nc < 0 || nc >= config.M) continue;

                            // Only FOREST tiles can ignite
                            if (grid[nr][nc] == TileState.FOREST) {
                                // 30% chance (or whatever pSpread is)
                                if (rng.nextDouble() < config.pSpread) {
                                    shouldIgnite[nr][nc] = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Phase 2: Apply ignitions and advance burn timers
        for (int r = 0; r < config.N; r++) {
            for (int c = 0; c < config.M; c++) {

                if (shouldIgnite[r][c]) {
                    // New fire!
                    grid[r][c]      = TileState.BURNING;
                    burnTimer[r][c] = 1;

                } else if (grid[r][c] == TileState.BURNING) {
                    // Already burning â€” advance timer
                    burnTimer[r][c]++;

                    // If burned long enough, it turns to ash
                    if (burnTimer[r][c] > config.burnTicks) {
                        grid[r][c]      = TileState.BURNED;
                        burnTimer[r][c] = 0;
                    }
                }
            }
        }
    }

    /**
     * Returns true if at least one tile is still burning.
     */
    private boolean hasBurningTile() {
        for (int r = 0; r < config.N; r++) {
            for (int c = 0; c < config.M; c++) {
                if (grid[r][c] == TileState.BURNING) return true;
            }
        }
        return false;
    }

    // 
    // Getters (used by the visualizer)
    // 

    public TileState[][] getGrid()    { return grid; }
    public SimConfig     getConfig()  { return config; }
    public int           getTick()    { return tick; }
}
