package wildfire;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Reads simulation configuration from a text file.
 * If anything is missing, we fall back to safe default values.
 */
public class ConfigReader {

    // Default values (used when a line is missing from instructions.txt)
    private static final int    DEFAULT_N           = 100;
    private static final int    DEFAULT_M           = 100;
    private static final int    DEFAULT_K           = 10;
    private static final double DEFAULT_P_SPREAD    = 0.30;
    private static final int    DEFAULT_BURN_TICKS  = 5;

    /**
     * Reads instructions.txt and returns a SimConfig.
     * The file format is one value per line: N, M, K, pSpread, burnTicks, seed
     */
    public static SimConfig readConfig(String filename) {
        int    N          = DEFAULT_N;
        int    M          = DEFAULT_M;
        int    K          = DEFAULT_K;
        double pSpread    = DEFAULT_P_SPREAD;
        int    burnTicks  = DEFAULT_BURN_TICKS;
        long   seed       = System.currentTimeMillis(); // random seed by default

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            String[] parts;

            // Try to read each line; if the line doesn't exist, keep the default
            line = reader.readLine();
            if (line != null) {
                parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    N = Integer.parseInt(parts[0]);
                    M = Integer.parseInt(parts[1]);
                }
                if (parts.length >= 3) K         = Integer.parseInt(parts[2]);
                if (parts.length >= 4) pSpread   = Double.parseDouble(parts[3]);
                if (parts.length >= 5) burnTicks = Integer.parseInt(parts[4]);
                if (parts.length >= 6) seed      = Long.parseLong(parts[5]);
            }

        } catch (IOException e) {
            System.out.println("instructions.txt not found or unreadable. Using defaults.");
        } catch (NumberFormatException e) {
            System.out.println("Bad number in instructions.txt. Using defaults where needed.");
        }

        return new SimConfig(N, M, K, pSpread, burnTicks, seed);
    }
}
