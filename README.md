# Fruit Fly Brain — Minecraft Fabric Mod

Minecraft Java 1.21.11 Fabric mod that simulates a Drosophila connectome. `/spawnfly` creates the simulated fly; hitting it triggers a nociception-like neural stimulus and `/flybrain` reports active nodes. This is a computer simulation, not real pain or suffering.

The current graph is a small prototype. The next stage is importing published Drosophila connectome neuron IDs and synapse edges into the simulation.

## Build
Use Java 21 and run `./gradlew build`. Fabric's 1.21.11 example targets Java 21, Loader 0.19.5, and Fabric API 0.141.6+1.21.11.
