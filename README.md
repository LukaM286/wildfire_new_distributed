# Wildfire Simulation — Sequential Version

Simulates the spread of a wildfire across a randomly generated forest grid.

## How to compile & run

```bash
# Compile all source files
javac -d out src/wildfire/*.java

# Run (instructions.txt must be in the current directory)
java -cp out wildfire.Main
```

## Configuration (`instructions.txt`)

One line with space-separated values:

```
N M K pSpread burnTicks seed
```

| Parameter   | Meaning                              | Default |
|-------------|--------------------------------------|---------|
| N           | Grid rows                            | 100     |
| M           | Grid columns                         | 100     |
| K           | Number of ignition points            | 10      |
| pSpread     | Spread probability per neighbor/tick | 0.30    |
| burnTicks   | Ticks a tile burns before turning to ash | 5   |
| seed        | Random seed (omit for random)        | random  |

## Color legend

| Color       | State   |
|-------------|---------|
| Tan         | Grass   |
| Dark green  | Forest  |
| Orange      | Burning |
| Dark gray   | Burned  |
