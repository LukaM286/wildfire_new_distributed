package wildfire;

import mpi.*;

public class Main {
    public static void main(String[] args) throws Exception {

        
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        SimConfig config = ConfigReader.readConfig("instructions.txt");

        if (rank == 0) {
            System.out.println("Config loaded: " + config);
            System.out.println("Processes: " + size);
        }

        WildfireSimulation sim = new WildfireSimulation(config, rank, size);
        sim.generateForest();
        sim.igniteRandomTiles();

        long startTime = System.currentTimeMillis();
        //sim.run(null);

        SimVisualizer visualizer = new SimVisualizer(sim);

        sim.run(visualizer);

/* proces 0 bi moral gather?
        SimVisualizer visualizer = null;
        if (rank == 0) {
            visualizer = new SimVisualizer(sim);
        }
        sim.run(visualizer);
*/


        long endTime = System.currentTimeMillis();

        if (rank == 0) {
            System.out.println("Simulation finished!");
            System.out.println("Total ticks: " + sim.getTick());
            System.out.println("Time: " + (endTime - startTime) + " ms");
        }

        MPI.Finalize();
    }
}