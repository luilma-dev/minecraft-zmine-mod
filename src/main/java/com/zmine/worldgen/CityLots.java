package com.zmine.worldgen;

import com.zmine.block.ZMineBlocks;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** Public spaces keep the city varied while retaining a street frontage on every block. */
public final class CityLots {
    private CityLots() { }

    public static BuildingBlueprint create(DistrictLayout.Zone zone, RandomSource random) {
        var b = new BuildingBlueprint(random);
        b.lot();
        if (zone == DistrictLayout.Zone.PARKING) {
            b.fill(1, 0, 1, 13, 0, 13, Blocks.CONCRETE.gray());
            for (int x : new int[]{1, 7, 13}) b.fill(x, 0, 6, x, 0, 12, Blocks.CONCRETE.white());
            b.fill(1, 0, 13, 13, 0, 13, Blocks.CONCRETE.white());
            b.fill(5, 0, 0, 9, 0, 5, Blocks.CONCRETE.gray());
            b.air(0, 1, 0, 14, 1, 2);
            StreetBuildings.car(b, 3, 1, 7, Blocks.CONCRETE.blue());
            if (random.nextBoolean()) StreetBuildings.car(b, 9, 1, 7, Blocks.CONCRETE.white());
            b.fill(0, 1, 5, 0, 4, 5, Blocks.POLISHED_BLACKSTONE_WALL);
            b.fill(0, 4, 4, 2, 6, 4, Blocks.CONCRETE.blue());
            b.fill(1, 5, 3, 1, 6, 3, Blocks.CONCRETE.white());
            b.put(2, 6, 3, Blocks.CONCRETE.white());
            b.box(13, 1, 2, ZMineBlocks.BOX2, Direction.NORTH, "tools");
        } else {
            b.fill(1, 0, 3, 13, 0, 13, Blocks.COARSE_DIRT);
            b.fill(6, 0, 0, 8, 0, 14, Blocks.SMOOTH_STONE);
            b.fill(0, 0, 6, 14, 0, 8, Blocks.SMOOTH_STONE);
            // Dry fountain with an open rim and a crumbling central sculpture.
            b.fill(5, 0, 5, 9, 0, 9, Blocks.STONE_BRICKS);
            b.fill(5, 1, 5, 9, 1, 9, Blocks.STONE_BRICK_SLAB);
            b.air(6, 1, 6, 8, 1, 8);
            b.fill(7, 1, 7, 7, 3, 7, Blocks.CHISELED_STONE_BRICKS);
            for (int z : new int[]{4, 10}) {
                b.stairs(3, 1, z, Blocks.SPRUCE_STAIRS, Direction.WEST);
                b.stairs(3, 1, z + 1, Blocks.SPRUCE_STAIRS, Direction.WEST);
                b.stairs(11, 1, z, Blocks.SPRUCE_STAIRS, Direction.EAST);
                b.stairs(11, 1, z + 1, Blocks.SPRUCE_STAIRS, Direction.EAST);
            }
            b.fill(2, 1, 12, 2, 5, 12, Blocks.STRIPPED_OAK_LOG);
            b.fill(2, 4, 12, 4, 4, 12, Blocks.OAK_WOOD);
            b.fill(12, 1, 4, 12, 4, 4, Blocks.DARK_OAK_LOG);
            b.put(11, 3, 4, Blocks.DARK_OAK_WOOD);
            b.put(2, 1, 4, Blocks.DEAD_BUSH);
            b.put(12, 1, 12, Blocks.DEAD_BUSH);
            b.put(1, 1, 7, Blocks.CAULDRON);
            b.box(13, 1, 9, ZMineBlocks.BOX3_1, Direction.WEST, "household");
        }
        return b;
    }
}
