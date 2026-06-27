package wildfire;

public class Main {
    public static void main(String[] args) {
        SimConfig config = ConfigReader.readConfig("instructions.txt");
        System.out.println("Config loaded: " + config);

        WildfireSimulation sim = new WildfireSimulation(config);
        sim.generateForest();
        sim.igniteRandomTiles();

        long startTime = System.currentTimeMillis();
        sim.run(null);
        long endTime = System.currentTimeMillis();

        System.out.println("Simulation finished!");
        System.out.println("Total ticks: " + sim.getTick());
        System.out.println("Time: " + (endTime - startTime) + " ms");
    }
}