package wildfire;


public class SimConfig {
    public final int N;           // number of rows
    public final int M;           // number of columns
    public final int K;           // number of fire ignition points
    public final double pSpread;  // probability a neighbor catches fire (0.0 - 1.0)
    public final int burnTicks;   // how many ticks a tile burns before it turns to ash
    public final long seed;       // random seed (for reproducibility)

    public SimConfig(int N, int M, int K, double pSpread, int burnTicks, long seed) {
        this.N = N;
        this.M = M;
        this.K = K;
        this.pSpread = pSpread;
        this.burnTicks = burnTicks;
        this.seed = seed;
    }

    @Override
    public String toString() {
        return String.format(
            "SimConfig{N=%d, M=%d, K=%d, pSpread=%.2f, burnTicks=%d, seed=%d}",
            N, M, K, pSpread, burnTicks, seed
        );
    }
}
