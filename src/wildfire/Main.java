package wildfire;

import mpi.*;

public class Main {
    public static void main(String[] args) throws Exception {

        // Inicializiraj MPJ 
        
        MPI.Init(args);

        // rank = kateri po vrsti
        // size = koliko jih je
        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        // Vsak proces prebere config samostojno
        SimConfig config = ConfigReader.readConfig("instructions.txt");

        // Samo proces 0 (master) izpisuje na začetku
        if (rank == 0) {
            System.out.println("Config loaded: " + config);
            System.out.println("Processes: " + size);
        }

        // vsak proces ve kdo je (rank) in koliko jih je (size)
        WildfireSimulation sim = new WildfireSimulation(config, rank, size);
        sim.generateForest();
        sim.igniteRandomTiles();

        long startTime = System.currentTimeMillis();
        sim.run(null);




/* proces 0 bi moral gather?
        SimVisualizer visualizer = null;
        if (rank == 0) {
            visualizer = new SimVisualizer(sim);
        }
        sim.run(visualizer);
*/


        long endTime = System.currentTimeMillis();

        // Samo master izpiše končne rezultate
        if (rank == 0) {
            System.out.println("Simulation finished!");
            System.out.println("Total ticks: " + sim.getTick());
            System.out.println("Time: " + (endTime - startTime) + " ms");
        }

        // Zaključi MPJ
        MPI.Finalize();
    }
}