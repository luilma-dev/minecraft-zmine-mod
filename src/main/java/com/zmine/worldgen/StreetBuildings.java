package com.zmine.worldgen;

import com.zmine.block.ZMineBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LadderBlock;

/** Roadside businesses and survivor remnants, each with its own silhouette. */
final class StreetBuildings {
    private StreetBuildings() { }

    static void diner(BuildingBlueprint b) {
        b.shell(0, 4, Blocks.SMOOTH_QUARTZ, Blocks.DYED_TERRACOTTA.white(), Blocks.SMOOTH_STONE);
        for (int x = 2; x <= 12; x++) for (int z = 4; z <= 12; z++) {
            b.put(x, 0, z, (x + z) % 2 == 0 ? Blocks.CONCRETE.white() : Blocks.CONCRETE.gray());
        }
        b.fill(1, 1, 3, 13, 1, 3, Blocks.CONCRETE.red());
        b.window(2, 2, 3, 5, 3, 3);
        b.window(9, 2, 3, 12, 3, 3);
        b.air(7, 1, 3, 7, 2, 3);
        b.fill(0, 5, 2, 14, 5, 14, Blocks.SMOOTH_STONE_SLAB);
        for (int x = 1; x <= 13; x++) b.fill(x, 4, 1, x, 4, 2,
            x % 2 == 0 ? Blocks.CONCRETE.white() : Blocks.CONCRETE.red());
        b.fill(10, 6, 11, 11, 6, 12, Blocks.POLISHED_ANDESITE);
        b.fill(2, 1, 11, 12, 1, 11, Blocks.SMOOTH_QUARTZ);
        b.face(3, 1, 12, Blocks.SMOKER, Direction.NORTH);
        b.face(4, 1, 12, Blocks.FURNACE, Direction.NORTH);
        b.put(10, 1, 12, Blocks.CAULDRON);
        b.box(6, 2, 11, ZMineBlocks.BOX3, Direction.NORTH, "food");
        b.box(12, 2, 12, ZMineBlocks.MEDKIT, Direction.NORTH, "medical");
        b.box(2, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        for (int x : new int[]{3, 10}) for (int z : new int[]{6, 9}) {
            b.table(x, 1, z);
            b.stairs(x - 1, 1, z, Blocks.RED_NETHER_BRICK_STAIRS, Direction.WEST);
            b.stairs(x + 1, 1, z, Blocks.RED_NETHER_BRICK_STAIRS, Direction.EAST);
        }
        b.box(12, 1, 5, ZMineBlocks.BOX3_1, Direction.WEST, "household");
        b.put(2, 4, 4, Blocks.COBWEB);
        b.put(12, 4, 12, Blocks.COBWEB);
        b.air(1, 4, 9, 2, 5, 10);
        b.lamp(7, 4, 7);
    }

    static void gasStation(BuildingBlueprint b) {
        b.weathered(0, 0, 0, 14, 0, 14, Blocks.CONCRETE.gray(), Blocks.GRAVEL);
        // Forecourt canopy and two disused pumps.
        for (int x : new int[]{2, 12}) b.fill(x, 1, 3, x, 4, 3, Blocks.IRON_BLOCK);
        b.fill(1, 5, 1, 13, 5, 7, Blocks.SMOOTH_QUARTZ);
        b.fill(1, 5, 1, 13, 5, 1, Blocks.CONCRETE.red());
        b.fill(1, 6, 1, 13, 6, 1, Blocks.QUARTZ_SLAB);
        b.air(10, 5, 5, 13, 6, 7);
        for (int x : new int[]{4, 10}) {
            b.fill(x - 1, 0, 3, x + 1, 0, 5, Blocks.SMOOTH_STONE);
            b.put(x, 1, 4, Blocks.CONCRETE.red());
            b.face(x, 2, 4, Blocks.DISPENSER, Direction.NORTH);
            b.put(x, 3, 4, Blocks.QUARTZ_SLAB);
            b.put(x - 1, 1, 4, Blocks.POLISHED_BLACKSTONE_WALL);
        }
        // Small shop at the back, rather than another full-size house shell.
        b.fill(1, 0, 9, 13, 0, 13, Blocks.SMOOTH_STONE);
        b.fill(1, 1, 9, 13, 3, 13, Blocks.DYED_TERRACOTTA.white());
        b.air(2, 1, 10, 12, 3, 12);
        b.fill(0, 4, 8, 14, 4, 14, Blocks.SMOOTH_STONE_SLAB);
        b.fill(1, 3, 9, 13, 3, 9, Blocks.CONCRETE.red());
        b.window(2, 1, 9, 5, 2, 9);
        b.window(9, 1, 9, 12, 2, 9);
        b.air(7, 1, 9, 7, 2, 9);
        b.fill(2, 1, 12, 5, 1, 12, Blocks.SPRUCE_PLANKS);
        b.box(2, 2, 12, ZMineBlocks.BOX3, Direction.NORTH, "food");
        b.box(4, 2, 12, ZMineBlocks.MEDKIT, Direction.NORTH, "medical");
        b.box(12, 1, 12, ZMineBlocks.BOX2, Direction.NORTH, "tools");
        b.box(10, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        b.put(2, 3, 10, Blocks.COBWEB);
        b.put(12, 3, 12, Blocks.COBWEB);
        b.put(1, 1, 7, Blocks.CAULDRON);
    }

    static void garage(BuildingBlueprint b) {
        b.shell(0, 5, Blocks.BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.POLISHED_ANDESITE);
        b.fill(0, 6, 2, 14, 6, 14, Blocks.DEEPSLATE_TILE_SLAB);
        b.air(3, 1, 3, 7, 3, 3);
        b.fill(3, 4, 3, 7, 4, 3, Blocks.IRON_BLOCK);
        b.window(10, 2, 3, 12, 3, 3);
        b.fill(9, 1, 4, 9, 4, 12, Blocks.DYED_TERRACOTTA.lightGray());
        b.air(9, 1, 6, 9, 2, 6);
        car(b, 4, 1, 6, Blocks.CONCRETE.cyan());
        b.fill(2, 1, 12, 7, 1, 12, Blocks.SMOOTH_STONE);
        b.put(2, 1, 12, Blocks.SMITHING_TABLE);
        b.put(7, 1, 12, Blocks.CRAFTING_TABLE);
        b.box(3, 2, 12, ZMineBlocks.BOX2, Direction.NORTH, "tools");
        b.box(6, 2, 12, ZMineBlocks.BOX3, Direction.NORTH, "tools");
        b.box(11, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "tools");
        b.box(12, 2, 12, ZMineBlocks.MEDKIT_WALL, Direction.NORTH, "medical");
        b.table(11, 1, 5);
        b.stairs(11, 1, 6, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        b.fill(10, 1, 9, 12, 2, 9, Blocks.BOOKSHELF);
        b.put(2, 5, 4, Blocks.COBWEB);
        b.air(11, 5, 11, 13, 6, 12);
        b.lamp(5, 5, 5);
    }

    static void checkpoint(BuildingBlueprint b) {
        b.fill(5, 0, 0, 9, 0, 14, Blocks.CONCRETE.gray());
        for (int z = 0; z <= 14; z += 3) b.put(7, 0, z, Blocks.CONCRETE.yellow());
        for (int z : new int[]{3, 10}) {
            b.fill(0, 1, z, 4, 1, z, Blocks.MUD_BRICKS);
            b.fill(10, 1, z, 14, 1, z, Blocks.MUD_BRICKS);
            b.fill(1, 2, z, 3, 2, z, Blocks.MUD_BRICK_SLAB);
            b.fill(11, 2, z, 13, 2, z, Blocks.MUD_BRICK_SLAB);
        }
        // Raised guard tower, with a climbable ladder and access hatch.
        for (int x : new int[]{1, 4}) for (int z : new int[]{5, 8}) b.fill(x, 1, z, x, 6, z, Blocks.DARK_OAK_LOG);
        b.fill(1, 5, 5, 4, 5, 8, Blocks.DARK_OAK_PLANKS);
        b.air(2, 5, 6, 2, 5, 6);
        b.fill(2, 1, 6, 2, 5, 6, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        b.fill(2, 1, 5, 2, 4, 5, Blocks.DARK_OAK_PLANKS);
        b.bars(1, 6, 5, 4, 5);
        b.bars(1, 6, 8, 4, 8);
        b.bars(1, 6, 6, 1, 7);
        b.bars(4, 6, 6, 4, 7);
        b.fill(1, 8, 5, 4, 8, 8, Blocks.DARK_OAK_SLAB);
        b.box(3, 6, 7, ZMineBlocks.BOX3, Direction.NORTH, "military");
        // Supply shelter with beds behind the roadblock.
        b.fill(10, 1, 5, 13, 3, 5, Blocks.DYED_TERRACOTTA.green());
        b.fill(13, 1, 6, 13, 3, 8, Blocks.DYED_TERRACOTTA.green());
        b.fill(10, 4, 5, 13, 4, 8, Blocks.SPRUCE_SLAB);
        b.bed(12, 1, 6, Blocks.BED.green(), Direction.SOUTH);
        b.box(10, 1, 6, ZMineBlocks.BOX2, Direction.WEST, "military");
        b.box(10, 1, 8, ZMineBlocks.MEDKIT_MILITARY, Direction.WEST, "medical");
        b.box(3, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        b.fill(5, 2, 3, 6, 2, 3, Blocks.CONCRETE.red());
        b.put(5, 1, 3, Blocks.COBBLESTONE_WALL);
        b.put(1, 4, 5, Blocks.COBWEB);
    }

    static void camp(BuildingBlueprint b) {
        b.weathered(0, 0, 0, 14, 0, 14, Blocks.COARSE_DIRT, Blocks.PODZOL);
        tent(b, 1, 4, Blocks.WOOL.green());
        tent(b, 9, 8, Blocks.WOOL.gray());
        b.put(7, 1, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        b.fill(5, 1, 5, 7, 1, 5, Blocks.SPRUCE_LOG.defaultBlockState()
            .setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, Direction.Axis.X));
        b.table(8, 1, 11);
        b.face(6, 1, 12, Blocks.FURNACE, Direction.NORTH);
        b.box(5, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        b.box(8, 1, 13, ZMineBlocks.MEDKIT, Direction.NORTH, "medical");
        b.box(2, 1, 11, ZMineBlocks.BOX2, Direction.EAST, "tools");
        b.bars(0, 1, 3, 0, 12);
        b.bars(14, 1, 3, 14, 12);
        b.fill(4, 1, 14, 7, 1, 14, Blocks.MUD_BRICKS);
        b.put(3, 1, 2, Blocks.DEAD_BUSH);
        b.put(12, 1, 2, Blocks.DEAD_BUSH);
        b.put(1, 3, 8, Blocks.COBWEB);
    }

    private static void tent(BuildingBlueprint b, int x, int z, Block cloth) {
        for (int step = 0; step < 3; step++) {
            b.fill(x + step, step + 1, z, x + step, step + 1, z + 4, cloth);
            b.fill(x + 4 - step, step + 1, z, x + 4 - step, step + 1, z + 4, cloth);
        }
        b.fill(x + 1, 1, z + 4, x + 3, 2, z + 4, cloth);
        b.bed(x + 1, 1, z + 1, Blocks.BED.brown(), Direction.SOUTH);
        b.box(x + 3, 1, z + 3, ZMineBlocks.BOX3_1, Direction.NORTH, "household");
    }

    /** Low armored silhouette: continuous tracks, sloped glacis, engine deck and a distinct turret. */
    public static void tank(BuildingBlueprint b, int x, int y, int z, boolean wrecked) {
        Block armor = Blocks.DYED_TERRACOTTA.green();
        b.fill(x + 1, y, z + 1, x + 5, y + 1, z + 8, armor);
        for (int dx : new int[]{0, 6}) {
            b.fill(x + dx, y, z + 1, x + dx, y, z + 8, Blocks.POLISHED_BLACKSTONE);
            for (int dz = 2; dz <= 7; dz++) {
                b.put(x + dx, y, z + dz, dz % 2 == 0 ? Blocks.CHISELED_POLISHED_BLACKSTONE : Blocks.POLISHED_BLACKSTONE);
                b.slab(x + dx, y + 1, z + dz, Blocks.POLISHED_BLACKSTONE_SLAB, true);
            }
            b.stairs(x + dx, y + 1, z + 1, Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.SOUTH);
            b.stairs(x + dx, y + 1, z + 8, Blocks.POLISHED_BLACKSTONE_STAIRS, Direction.NORTH);
        }
        for (int dx = 1; dx <= 5; dx++) {
            b.stairs(x + dx, y + 1, z, Blocks.MUD_BRICK_STAIRS, Direction.SOUTH);
            b.slab(x + dx, y + 2, z + 1, Blocks.MUD_BRICK_SLAB, false);
            b.slab(x + dx, y + 2, z + 7, Blocks.MUD_BRICK_SLAB, false);
            b.stairs(x + dx, y + 1, z + 8, Blocks.MUD_BRICK_STAIRS, Direction.NORTH);
        }
        // Shoulder armor leaves the turret narrower than the hull.
        b.fill(x + 1, y + 2, z + 2, x + 1, y + 2, z + 6, Blocks.MUD_BRICK_SLAB);
        b.fill(x + 5, y + 2, z + 2, x + 5, y + 2, z + 6, Blocks.MUD_BRICK_SLAB);
        b.fill(x + 2, y + 2, z + 3, x + 4, y + 3, z + 5, armor);
        b.stairs(x + 2, y + 3, z + 3, Blocks.MUD_BRICK_STAIRS, Direction.EAST);
        b.stairs(x + 4, y + 3, z + 3, Blocks.MUD_BRICK_STAIRS, Direction.WEST);
        b.fill(x + 2, y + 4, z + 4, x + 4, y + 4, z + 5, Blocks.MUD_BRICK_SLAB);
        b.bars(x + 3, y + 3, z - 2, x + 3, z + 2);
        b.put(x + 3, y + 3, z - 2, Blocks.POLISHED_BLACKSTONE_SLAB);
        b.put(x + 3, y + 4, z + 4, Blocks.SPRUCE_TRAPDOOR);
        b.face(x + 4, y + 4, z + 5, Blocks.LIGHTNING_ROD.waxed().weathered(), Direction.UP);
        b.put(x + 2, y + 2, z + 7, Blocks.IRON_TRAPDOOR);
        b.put(x + 4, y + 2, z + 7, Blocks.IRON_TRAPDOOR);
        b.put(x + 1, y + 1, z, Blocks.SMOOTH_QUARTZ_SLAB);
        b.put(x + 5, y + 1, z, Blocks.SMOOTH_QUARTZ_SLAB);
        // Reachable rear supplies, with torn armor on the wrecked version.
        b.box(x + 3, y + 2, z + 7, ZMineBlocks.BOX3, Direction.SOUTH, "military");
        if (wrecked) {
            b.put(x + 5, y + 1, z + 6, Blocks.BLACKSTONE);
            b.put(x + 5, y + 2, z + 6, Blocks.COBWEB);
            b.put(x + 4, y + 1, z + 8, Blocks.CRACKED_DEEPSLATE_TILES);
            b.put(x + 4, y + 2, z + 6, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        } else b.box(x + 2, y + 2, z + 6, ZMineBlocks.MEDKIT_MILITARY, Direction.WEST, "medical");
    }

    /** Compact wreck, shared by the workshop and abandoned streets. */
    static void car(BuildingBlueprint b, int x, int y, int z, Block paint) {
        b.fill(x, y, z, x + 2, y, z + 4, paint);
        for (int wheelZ : new int[]{z + 1, z + 3}) {
            b.put(x - 1, y, wheelZ, Blocks.POLISHED_BLACKSTONE);
            b.put(x + 3, y, wheelZ, Blocks.POLISHED_BLACKSTONE);
        }
        b.fill(x, y + 1, z + 1, x + 2, y + 1, z + 3, Blocks.STAINED_GLASS.gray());
        b.air(x + 1, y + 1, z + 1, x + 1, y + 1, z + 2);
        b.fill(x, y + 2, z + 2, x + 2, y + 2, z + 3, Blocks.STONE_SLAB);
        b.put(x, y, z, Blocks.SEA_LANTERN);
        b.put(x + 2, y, z, Blocks.SMOOTH_QUARTZ);
        b.put(x, y, z + 4, Blocks.NETHER_BRICKS);
        b.put(x + 2, y, z + 4, Blocks.NETHER_BRICKS);
    }
}
