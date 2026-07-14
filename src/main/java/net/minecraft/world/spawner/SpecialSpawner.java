package net.minecraft.world.spawner;

import net.minecraft.server.world.ServerWorld;

public interface SpecialSpawner {
   int spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals);
}
