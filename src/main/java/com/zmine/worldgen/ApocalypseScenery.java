package com.zmine.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;

@SuppressWarnings("null")
public final class ApocalypseScenery {
    private ApocalypseScenery() { }

    public static boolean countryside(WorldGenLevel level, int x0, int z0, RandomSource random) {
        boolean placed = random.nextInt(5) == 0 && wilderness(level, x0, z0, random);
        // Broad grassy fields, with sparse dead groves instead of an evenly forested wasteland.
        for (int i = 0; i < 12; i++) {
            var ground = RuinsTerrain.ground(level, x0 + random.nextInt(16), z0 + random.nextInt(16));
            if (RuinsTerrain.dryNaturalGround(level, ground) && level.getBlockState(ground.above()).isAir()) {
                level.setBlock(ground.above(), (random.nextInt(4) == 0 ? Blocks.DEAD_BUSH : Blocks.SHORT_GRASS).defaultBlockState(), 2);
                placed = true;
            }
        }
        if (random.nextInt(55) == 0) {
            var b = new BuildingBlueprint(random);
            StreetBuildings.tank(b, 4, 1, 2, true);
            var ground = RuinsTerrain.ground(level, x0 + 7, z0 + 6);
            boolean flat = RuinsTerrain.dryNaturalGround(level, ground);
            for (int x = 4; x <= 10 && flat; x++) for (int z = 3; z <= 10; z++) {
                if (RuinsTerrain.ground(level, x0 + x, z0 + z).getY() != ground.getY()) flat = false;
            }
            if (flat) placed |= overlayIfClear(b, level, new BlockPos(x0, ground.getY(), z0), random);
        }
        return placed;
    }

    public static boolean wilderness(WorldGenLevel level, int x0, int z0, RandomSource random) {
        boolean placed = false;
        // Multiple independent roots make dead groves instead of evenly spaced single trees.
        int count = 1 + random.nextInt(4);
        for (int i = 0; i < count; i++) {
            int x = 3 + random.nextInt(9), z = 3 + random.nextInt(9);
            BlockPos ground = RuinsTerrain.ground(level, x0 + x, z0 + z);
            if (!RuinsTerrain.dryNaturalGround(level, ground)) continue;
            var b = new BuildingBlueprint(random);
            int height = 4 + random.nextInt(4);
            var wood = random.nextBoolean() ? Blocks.DARK_OAK_LOG : Blocks.STRIPPED_OAK_LOG;
            b.fill(x, 1, z, x, height, z, wood);
            b.fill(x - 2, height - 2, z, x + 1, height - 2, z,
                wood.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
            b.put(x - 2, height - 1, z, wood);
            b.fill(x, height - 1, z, x, height - 1, z + 2,
                wood.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Z));
            b.put(x + 1, 1, z, Blocks.OAK_WOOD);
            placed |= overlayIfClear(b, level, new BlockPos(x0, ground.getY(), z0), random);
        }
        if (random.nextInt(3) == 0) {
            var b = new BuildingBlueprint(random);
            if (random.nextBoolean()) {
                // Toppled trunk, exposed stump and dry undergrowth.
                b.fill(3, 1, 8, 8, 1, 8, Blocks.SPRUCE_LOG.defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
                b.put(9, 1, 8, Blocks.STRIPPED_SPRUCE_LOG);
                b.put(4, 1, 7, Blocks.DEAD_BUSH);
            } else {
                b.weathered(4, 1, 7, 8, 2, 7, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
                b.put(4, 3, 7, Blocks.COBBLESTONE_WALL);
                b.slab(7, 1, 8, Blocks.STONE_BRICK_SLAB, false);
                b.put(9, 1, 7, Blocks.DEAD_BUSH);
            }
            BlockPos ground = RuinsTerrain.ground(level, x0 + 6, z0 + 8);
            if (RuinsTerrain.dryNaturalGround(level, ground)) placed |= overlayIfClear(b, level, new BlockPos(x0, ground.getY(), z0), random);
        }
        return placed;
    }

    /** Standalone preview on local terrain; natural city generation uses CityTerrain.surfaceY. */
    public static boolean road(WorldGenLevel level, int x0, int z0, DistrictLayout.Zone zone, RandomSource random) {
        var origin = new BlockPos(x0, RuinsTerrain.ground(level, x0 + 7, z0 + 7).getY(), z0);
        var lot = new DistrictLayout.Lot(zone, null, Rotation.NONE, false, false);
        if (!lot.isRoad()) return false;
        int[][] heights = new int[16][16];
        for (var row : heights) java.util.Arrays.fill(row, origin.getY());
        var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
        if (bed == null) return false;
        CityRoads.paint(level, origin, lot, heights, random, bed);
        return true;
    }

    private static boolean overlayIfClear(BuildingBlueprint b, WorldGenLevel level, BlockPos origin, RandomSource random) {
        for (var entry : b.blocks().entrySet()) {
            BlockPos target = origin.offset(entry.getKey());
            var existing = level.getBlockState(target);
            if (!level.ensureCanWrite(target) || !existing.getFluidState().isEmpty()
                || existing.hasBlockEntity() || !(existing.isAir() || existing.canBeReplaced())) return false;
        }
        b.placeOverlay(level, origin, Rotation.NONE, random);
        return true;
    }
}
