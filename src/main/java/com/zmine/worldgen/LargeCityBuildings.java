package com.zmine.worldgen;

import com.zmine.block.ZMineBlocks;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;

/** 32x32 complexes, sliced during generation so no feature writes into a neighboring chunk. */
public final class LargeCityBuildings {
    public enum Kind {
        MILITARY_BASE("military_base"), APARTMENT_TOWER("apartment_tower"), HOSPITAL("abandoned_hospital"), RUINED_TOWER("ruined_tower");
        public final String id;
        Kind(String id) { this.id = id; }
    }
    private LargeCityBuildings() { }
    public static BuildingBlueprint create(Kind kind, long seed) {
        var b = new BuildingBlueprint(RandomSource.create(seed), 32);
        b.weathered(0, 0, 0, 31, 0, 31, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        if (kind == Kind.MILITARY_BASE) military(b);
        else tower(b, kind == Kind.HOSPITAL, kind == Kind.RUINED_TOWER);
        return b;
    }
    private static void room(BuildingBlueprint b, int x0, int z0, int x1, int z1, int height, Block wall) {
        b.fill(x0, 1, z0, x1, height, z1, wall);
        b.air(x0 + 1, 1, z0 + 1, x1 - 1, height - 1, z1 - 1);
        b.fill(x0, height + 1, z0, x1, height + 1, z1, Blocks.SMOOTH_STONE_SLAB);
        b.air((x0 + x1) / 2, 1, z0, (x0 + x1) / 2 + 1, 3, z0);
    }
    private static void military(BuildingBlueprint b) {
        b.weathered(1, 0, 1, 30, 0, 30, Blocks.GRAVEL, Blocks.COARSE_DIRT);
        b.fill(13, 0, 0, 18, 0, 31, Blocks.CONCRETE.gray());
        for (int z = 1; z < 30; z += 6) b.fill(15, 0, z, 15, 0, z + 2, Blocks.CONCRETE.yellow());
        for (int y = 1; y <= 2; y++) {
            b.fill(0, y, 0, 31, y, 0, Blocks.MUD_BRICKS);
            b.fill(0, y, 31, 31, y, 31, Blocks.MUD_BRICKS);
            b.fill(0, y, 1, 0, y, 30, Blocks.MUD_BRICKS);
            b.fill(31, y, 1, 31, y, 30, Blocks.MUD_BRICKS);
        }
        b.bars(0, 3, 0, 31, 0); b.bars(0, 3, 31, 31, 31);
        b.bars(0, 3, 1, 0, 30); b.bars(31, 3, 1, 31, 30);
        b.air(13, 1, 0, 18, 3, 0);
        b.fill(12, 1, 0, 12, 6, 0, Blocks.POLISHED_BLACKSTONE_WALL);
        b.fill(19, 1, 0, 19, 6, 0, Blocks.POLISHED_BLACKSTONE_WALL);
        b.fill(12, 6, 0, 19, 7, 0, Blocks.CONCRETE.green());
        b.fill(14, 7, 0, 17, 7, 0, Blocks.CONCRETE.white());
        watchtower(b, 1, 1); watchtower(b, 26, 26);
        room(b, 2, 10, 11, 28, 4, Blocks.DYED_TERRACOTTA.green());
        for (int z : new int[]{13, 17, 21, 25}) {
            b.bed(3, 1, z, Blocks.BED.green(), Direction.SOUTH);
            b.bed(9, 1, z, Blocks.BED.green(), Direction.SOUTH);
            b.box(5, 1, z, ZMineBlocks.BOX2, Direction.EAST, "military");
            b.window(2, 2, z, 2, 3, z + 1);
        }
        b.box(8, 1, 11, ZMineBlocks.MEDKIT_MILITARY, Direction.SOUTH, "medical");
        room(b, 20, 13, 29, 28, 7, Blocks.CONCRETE.lightGray());
        b.air(22, 1, 13, 27, 4, 13);
        StreetBuildings.tank(b, 21, 1, 17, false);
        StreetBuildings.tank(b, 21, 1, 2, true);
        b.box(28, 1, 27, ZMineBlocks.BOX2, Direction.WEST, "tools");
        b.box(28, 2, 27, ZMineBlocks.BOX3, Direction.WEST, "military");
        b.box(20, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        b.fill(6, 0, 2, 11, 0, 7, Blocks.SMOOTH_STONE);
        b.fill(7, 1, 3, 11, 1, 3, Blocks.SPRUCE_PLANKS);
        b.put(8, 2, 3, Blocks.CONCRETE.black());
        b.put(10, 2, 3, Blocks.CARTOGRAPHY_TABLE);
        b.box(7, 1, 5, ZMineBlocks.BOX3, Direction.SOUTH, "military");
        b.fill(6, 4, 2, 11, 4, 7, Blocks.WOOL.green());
        b.fill(6, 1, 7, 6, 3, 7, Blocks.DARK_OAK_FENCE);
        b.fill(11, 1, 7, 11, 3, 7, Blocks.DARK_OAK_FENCE);
    }
    private static void watchtower(BuildingBlueprint b, int x, int z) {
        for (int dx : new int[]{0, 4}) for (int dz : new int[]{0, 4}) b.fill(x + dx, 1, z + dz, x + dx, 10, z + dz, Blocks.POLISHED_ANDESITE);
        b.fill(x, 8, z, x + 4, 8, z + 4, Blocks.SMOOTH_STONE);
        b.fill(x, 11, z, x + 4, 11, z + 4, Blocks.SMOOTH_STONE_SLAB);
        b.bars(x, 9, z, x + 4, z); b.bars(x, 9, z + 4, x + 4, z + 4);
        b.bars(x, 9, z + 1, x, z + 3); b.bars(x + 4, 9, z + 1, x + 4, z + 3);
        b.fill(x + 1, 1, z, x + 1, 7, z, Blocks.POLISHED_ANDESITE);
        b.air(x + 1, 8, z + 1, x + 1, 8, z + 1);
        b.fill(x + 1, 1, z + 1, x + 1, 8, z + 1, Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.SOUTH));
        b.box(x + 3, 9, z + 2, ZMineBlocks.BOX3, Direction.WEST, "military");
    }
    private static void tower(BuildingBlueprint b, boolean hospital, boolean ruined) {
        int floors = hospital ? 4 : 7;
        Block wall = hospital ? Blocks.DYED_TERRACOTTA.white() : Blocks.BRICKS;
        for (int floor = 0; floor < floors; floor++) {
            int y = floor * 5;
            b.fill(3, y, 5, 28, y, 28, hospital ? Blocks.SMOOTH_QUARTZ : Blocks.SPRUCE_PLANKS);
            b.weathered(3, y + 1, 5, 28, y + 4, 5, wall, Blocks.CRACKED_STONE_BRICKS);
            b.weathered(3, y + 1, 28, 28, y + 4, 28, wall, Blocks.CRACKED_STONE_BRICKS);
            b.weathered(3, y + 1, 6, 3, y + 4, 27, wall, Blocks.MOSSY_STONE_BRICKS);
            b.weathered(28, y + 1, 6, 28, y + 4, 27, wall, Blocks.MOSSY_STONE_BRICKS);
            b.air(4, y + 1, 6, 27, y + 4, 27);
            for (int x = 5; x < 27; x += 5) {
                b.window(x, y + 2, 5, x + 2, y + 3, 5);
                b.window(x, y + 2, 28, x + 2, y + 3, 28);
            }
            for (int z = 8; z < 27; z += 6) {
                b.window(3, y + 2, z, 3, y + 3, z + 2);
                b.window(28, y + 2, z, 28, y + 3, z + 2);
            }
            for (int x : new int[]{13, 18}) {
                b.fill(x, y + 1, 6, x, y + 4, 27, Blocks.DYED_TERRACOTTA.lightGray());
                b.air(x, y + 1, 10, x, y + 2, 10);
                b.air(x, y + 1, 22, x, y + 2, 22);
            }
            for (int x : new int[]{5, 20}) for (int z : new int[]{8, 22}) {
                b.bed(x, y + 1, z, hospital ? Blocks.BED.white() : Blocks.BED.brown(), Direction.SOUTH);
                b.box(x + 2, y + 1, z, hospital ? ZMineBlocks.MEDKIT : ZMineBlocks.BOX3_1,
                    Direction.WEST, hospital ? "medical" : "household");
                b.table(x + 4, y + 1, z + 1);
            }
            b.fill(4, y + 1, 16, 12, y + 4, 16, Blocks.DYED_TERRACOTTA.lightGray());
            b.fill(19, y + 1, 16, 27, y + 4, 16, Blocks.DYED_TERRACOTTA.lightGray());
        }
        int roof = floors * 5;
        b.fill(3, roof, 5, 28, roof, 28, Blocks.SMOOTH_STONE);
        b.bars(3, roof + 1, 5, 28, 5); b.bars(3, roof + 1, 28, 28, 28);
        b.bars(3, roof + 1, 6, 3, 27); b.bars(28, roof + 1, 6, 28, 27);
        b.air(14, 1, 5, 17, 3, 5);
        b.fill(12, 4, 2, 19, 4, 4, Blocks.SMOOTH_STONE_SLAB);
        for (int floor = 0; floor < floors; floor++) for (int step = 0; step < 5; step++) {
            int y = floor * 5 + step + 1;
            b.air(15, y + 1, 20 + step, 16, y + 3, 20 + step);
            b.stairs(15, y, 20 + step, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
            b.stairs(16, y, 20 + step, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
        }
        b.fill(7, roof + 1, 20, 10, roof + 3, 24, Blocks.CONCRETE.gray());
        b.box(12, roof + 1, 24, ZMineBlocks.BOX2, Direction.NORTH, "tools");
        if (hospital) {
            b.fill(8, roof, 8, 22, roof, 18, Blocks.CONCRETE.green());
            b.fill(11, roof, 10, 12, roof, 16, Blocks.CONCRETE.white());
            b.fill(18, roof, 10, 19, roof, 16, Blocks.CONCRETE.white());
            b.fill(13, roof, 13, 17, roof, 13, Blocks.CONCRETE.white());
            b.fill(14, 7, 4, 17, 8, 4, Blocks.CONCRETE.red());
            b.fill(15, 6, 4, 16, 9, 4, Blocks.CONCRETE.red());
        }
        if (ruined) {
            // A collapsed corner exposes several floors while preserving the central stairwell.
            for (int y = 6; y <= roof + 3; y++) b.air(3, y, 5, 5 + y / 6, y, 8 + y / 8);
            b.weathered(3, 1, 3, 10, 2, 8, Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE);
            b.fill(4, 3, 5, 6, 4, 7, Blocks.BRICKS);
            b.put(10, 1, 4, Blocks.COBWEB);
            b.fill(7, 1, 3, 10, 1, 3, Blocks.IRON_BARS);
        }
    }
}
