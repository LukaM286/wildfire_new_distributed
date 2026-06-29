package wildfire;

import mpi.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import mpi.Request;

/**
 * 
 *
 * 
 *   - Vsak proces ima svoj del grida
 *   - med seboj si pošiljajo robne vrstice (boundary rows)
 *     ,požar lahko preskoči med pasovi
 *   - Ni skupnega pomnilnika, preko sporočil (Send/Recv)
 *
 */
public class WildfireSimulation {

    private final SimConfig config;
    private final Random    rng;

    // rank 
    // size 
    private final int rank;
    private final int size;

    // Vsak proces hrani CEL grid
    private TileState[][] grid;
    private int[][]       burnTimer;
    private boolean[][]   shouldIgnite;
    private int           tick;

    // Kateri pas vrstic pripada temu procesu
    private final int rowStart;
    private final int rowEnd;

    public WildfireSimulation(SimConfig config, int rank, int size) {
        this.config = config;
        this.rank   = rank;
        this.size   = size;
        this.rng    = new Random(config.seed);

        this.grid         = new TileState[config.N][config.M];
        this.burnTimer    = new int[config.N][config.M];
        this.shouldIgnite = new boolean[config.N][config.M];
        this.tick         = 0;

        // Izračunaj pas vrstic za ta proces
        int rowsPerProcess = config.N / size;
        this.rowStart = rank * rowsPerProcess;
        this.rowEnd   = (rank == size - 1) ? config.N : rowStart + rowsPerProcess;

        // Inicializiraj cel grid kot GRASS
        for (int r = 0; r < config.N; r++)
            for (int c = 0; c < config.M; c++)
                grid[r][c] = TileState.GRASS;
    }

    // 
    // generateForest() - vsak proces generira ISTI gozd (isti seed)
    // ni potrebno pošiljati grida med procesi
    // 

    public void generateForest() {
        int totalTiles   = config.N * config.M;
        int targetForest = totalTiles / 2;

        int row = rng.nextInt(config.N);
        int col = rng.nextInt(config.M);
        int forestCount = 0;

        int[] dRow = {-1, 1, 0, 0};
        int[] dCol = { 0, 0,-1, 1};

        while (forestCount < targetForest) {
            if (grid[row][col] == TileState.GRASS) {
                grid[row][col] = TileState.FOREST;
                forestCount++;
            }
            int dir = rng.nextInt(4);
            int newRow = row + dRow[dir];
            int newCol = col + dCol[dir];
            if (newRow >= 0 && newRow < config.N && newCol >= 0 && newCol < config.M) {
                row = newRow;
                col = newCol;
            }
        }

        if (rank == 0) {
            System.out.printf("Forest generated: %d tiles (%.1f%%)%n",
                forestCount, 100.0 * forestCount / totalTiles);
        }
    }

    // 
    // igniteRandomTiles() - vsak proces zaigne iste tile (isti seed)
    // 

    public void igniteRandomTiles() {
        List<int[]> forestTiles = new ArrayList<>();
        for (int r = 0; r < config.N; r++)
            for (int c = 0; c < config.M; c++)
                if (grid[r][c] == TileState.FOREST)
                    forestTiles.add(new int[]{r, c});

        int ignitions = Math.min(config.K, forestTiles.size());
        Collections.shuffle(forestTiles, rng);
        for (int i = 0; i < ignitions; i++) {
            int r = forestTiles.get(i)[0];
            int c = forestTiles.get(i)[1];
            grid[r][c]      = TileState.BURNING;
            burnTimer[r][c] = 1;
        }

        if (rank == 0) {
            System.out.printf("Fire started at %d tiles%n", ignitions);
        }
    }

    // 
    // 
    // 

    public void run(SimVisualizer visualizer) {
        // Vsak proces preveri ali ima še goreče tile v SVOJEM pasu
        // Potem z Allreduce preveri če katerikoli proces še ima ogenj
        while (globallyBurning()) {
            tick++;
            doTick();

            
            if (visualizer != null) {
                visualizer.repaintAndWait();
            }
        }
    }

    // 
    // En tick simulacije
    // 

    private void doTick() {

        // KORAK 1: Izmenjaj robne vrstice s sosednjimi procesi
        // Vsak proces pošlji svojo prvo in zadnjo vrstico sosedom
        exchangeBoundaryRows();

        // KORAK 2: Izračunaj shouldIgnite za svoj pas
        for (int r = rowStart; r < rowEnd; r++) {
            for (int c = 0; c < config.M; c++) {
                if (grid[r][c] == TileState.BURNING) {
                    for (int dr = -1; dr <= 1; dr++) {
                        for (int dc = -1; dc <= 1; dc++) {
                            if (dr == 0 && dc == 0) continue;
                            int nr = r + dr;
                            int nc = c + dc;
                            if (nr < 0 || nr >= config.N || nc < 0 || nc >= config.M) continue;
                            if (grid[nr][nc] == TileState.FOREST) {
                                if (rng.nextDouble() < config.pSpread) {
                                    shouldIgnite[nr][nc] = true;
                                }
                            }
                        }
                    }
                }
            }
        }

        // KORAK 3: Apliciraj spremembe za svoj pas
        for (int r = rowStart; r < rowEnd; r++) {
            for (int c = 0; c < config.M; c++) {
                if (shouldIgnite[r][c]) {
                    grid[r][c]         = TileState.BURNING;
                    burnTimer[r][c]    = 1;
                    shouldIgnite[r][c] = false;
                } else if (grid[r][c] == TileState.BURNING) {
                    burnTimer[r][c]++;
                    if (burnTimer[r][c] > config.burnTicks) {
                        grid[r][c]      = TileState.BURNED;
                        burnTimer[r][c] = 0;
                    }
                }
            }
        }
    }

    // 
    // Izmenjava robnih vrstic med sosednjimi procesi
    //
    // 
    // Proces 0 ima vrstice 0-24. Proces 1 ima vrstice 25-49.
    // Če gori tile v vrstici 24 (proces 0), lahko vname tile v vrstici 25
    // (ki pripada procesu 1). proces 1 ne ve da vrstica 24 gori
    // 
    // 

    private void exchangeBoundaryRows() {
        int M = config.M;

        // buffer za pošiljanje in prejemanje
        int[] sendDown = new int[M]; // pošlji procesu rank+1
        int[] sendUp   = new int[M]; // pošlji procesu rank-1
        int[] recvDown = new int[M]; // prejmi od procesa rank+1
        int[] recvUp   = new int[M]; // prejmi od procesa rank-1

        Request[] requests = new Request[4];
        int reqCount = 0;

        // non-blocking prejemanje PREDEN pošiljamo
        if (rank < size - 1) { //če nisi zadnji proces(ta nima soseda spodaj)
            try { //prejmi od procesa spodaj, pripravi se
                requests[reqCount++] = MPI.COMM_WORLD.Irecv(recvDown, 0, M, MPI.INT, rank + 1, 0);
            } catch (Exception e) { e.printStackTrace(); }
        }
        if (rank > 0) { //samo če nmisi prvi proces(ta nima soseda zograj)
            try {
                requests[reqCount++] = MPI.COMM_WORLD.Irecv(recvUp, 0, M, MPI.INT, rank - 1, 1);
            } catch (Exception e) { e.printStackTrace(); }
        }

        // robne vrstice
        if (rank < size - 1) { //če nisi zadnji proces(ta nima soseda spodaj)
            sendDown = tileRowToIntArray(grid[rowEnd - 1]); //poslji zadnjo vrstico pasa, spremeni v int[]
            try {
                requests[reqCount++] = MPI.COMM_WORLD.Isend(sendDown, 0, M, MPI.INT, rank + 1, 1);
            } catch (Exception e) { e.printStackTrace(); }
        }
        if (rank > 0) { //samo če nmisi prvi proces(ta nima soseda zograj)
            sendUp = tileRowToIntArray(grid[rowStart]);
            try {
                requests[reqCount++] = MPI.COMM_WORLD.Isend(sendUp, 0, M, MPI.INT, rank - 1, 0);
            } catch (Exception e) { e.printStackTrace(); }
        }

        // Počakaj da se vse pošiljanje/prejemanje konča
        try {
            Request.Waitall(java.util.Arrays.copyOf(requests, reqCount));
        } catch (Exception e) { e.printStackTrace(); }

        // Shrani prejete vrstice
        if (rank < size - 1 && rowEnd < config.N) {
            intArrayToTileRow(recvDown, grid[rowEnd]);
        }
        if (rank > 0 && rowStart > 0) {
            intArrayToTileRow(recvUp, grid[rowStart - 1]);
        }
    }

    // 
    // Preveri ali katerikoli proces še ima goreče tile
    // MPI.Allreduce zbere vrednosti od vseh procesov
    // 

    private boolean globallyBurning() {
        int localBurning = 0;
        for (int r = rowStart; r < rowEnd; r++)
            for (int c = 0; c < config.M; c++)
                if (grid[r][c] == TileState.BURNING) {
                    localBurning = 1;
                    break;
                }

        int[] local  = {localBurning};
        int[] global = {0};

        // Zberi vsote na procesu 0 (če se kaj gori)
        try {
            MPI.COMM_WORLD.Reduce(local, 0, global, 0, 1, MPI.INT, MPI.SUM, 0);
        } catch (Exception e) { e.printStackTrace(); }

        // Proces 0 pošlje rezultat vsem
        try {
            MPI.COMM_WORLD.Bcast(global, 0, 1, MPI.INT, 0);
        } catch (Exception e) { e.printStackTrace(); }

        return global[0] > 0;
    }

    // 
    // Pomožne metode za pretvorbo TileState ↔ int
    // (MPJ zna pošiljati int[], ne TileState[])
    // 

    private int[] tileRowToIntArray(TileState[] row) {
        int[] arr = new int[row.length];
        for (int i = 0; i < row.length; i++) {
            arr[i] = row[i].ordinal(); // GRASS=0, FOREST=1, BURNING=2, BURNED=3
        }
        return arr;
    }

    private void intArrayToTileRow(int[] arr, TileState[] row) {
        TileState[] values = TileState.values();
        for (int i = 0; i < arr.length; i++) {
            row[i] = values[arr[i]];
        }
    }

    // 
    // Getterji
    // 

    public TileState[][] getGrid()   { return grid; }
    public SimConfig     getConfig() { return config; }
    public int           getTick()   { return tick; }


    //get za animacijo 
    public int getRowStart() { return rowStart; }
    public int getRowEnd()   { return rowEnd; }
    public int getRank()     { return rank; }
    public int getSize()     { return size; }

}