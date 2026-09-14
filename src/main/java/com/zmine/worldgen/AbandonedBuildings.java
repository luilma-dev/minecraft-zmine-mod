package com.zmine.worldgen;

import com.zmine.block.ZMineBlocks;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

/** Hand-built interiors and silhouettes, with restrained material decay. */
public final class AbandonedBuildings {
    public enum Kind {
        HOUSE("abandoned_house"), APARTMENTS("abandoned_apartments"),
        MARKET("abandoned_market"), CLINIC("abandoned_clinic"), WAREHOUSE("abandoned_warehouse"),
        DINER("abandoned_diner"), GAS_STATION("abandoned_gas_station"),
        GARAGE("abandoned_garage"), CHECKPOINT("abandoned_checkpoint"), CAMP("survivor_camp"),
        BRICK_HOUSE("abandoned_brick_house"), TOWNHOUSE("abandoned_townhouse"), FARMHOUSE("abandoned_farmhouse");
        public final String id;
        Kind(String id) { this.id = id; }
    }

    private AbandonedBuildings() { }

    public static BuildingBlueprint create(Kind kind, RandomSource random) {
        BuildingBlueprint b = new BuildingBlueprint(random);
        b.lot();
        switch (kind) {
            case HOUSE -> house(b);
            case APARTMENTS -> apartments(b);
            case MARKET -> market(b);
            case CLINIC -> clinic(b);
            case WAREHOUSE -> warehouse(b);
            case DINER -> StreetBuildings.diner(b);
            case GAS_STATION -> StreetBuildings.gasStation(b);
            case GARAGE -> StreetBuildings.garage(b);
            case CHECKPOINT -> StreetBuildings.checkpoint(b);
            case CAMP -> StreetBuildings.camp(b);
            case BRICK_HOUSE -> cottage(b, false);
            case FARMHOUSE -> cottage(b, true);
            case TOWNHOUSE -> townhouse(b);
        }
        return b;
    }

    private static void cottage(BuildingBlueprint b, boolean farm) {
        b.shell(0, 4, farm ? Blocks.OAK_PLANKS : Blocks.BRICKS, Blocks.MOSSY_STONE_BRICKS, Blocks.SPRUCE_PLANKS);
        b.door(7, 1, 3, Direction.NORTH);
        b.window(3, 2, 3, 5, 3, 3); b.window(9, 2, 3, 11, 3, 3);
        b.window(1, 2, 7, 1, 3, 9); b.window(13, 2, 7, 13, 3, 9);
        // Roof slopes face each other; solid gables close the old open, floating roof sections.
        Block roofStairs = farm ? Blocks.SPRUCE_STAIRS : Blocks.BRICK_STAIRS;
        for (int step = 0; step < 6; step++) {
            for (int x = 0; x <= 14; x++) {
                b.stairs(x, 5 + step, 2 + step, roofStairs, Direction.SOUTH);
                b.stairs(x, 5 + step, 14 - step, roofStairs, Direction.NORTH);
            }
            if (step < 5) for (int x : new int[]{1, 13})
                b.fill(x, 5 + step, 3 + step, x, 5 + step, 13 - step, farm ? Blocks.OAK_PLANKS : Blocks.BRICKS);
        }
        b.fill(0, 10, 8, 14, 10, 8, farm ? Blocks.SPRUCE_SLAB : Blocks.BRICK_SLAB);
        b.fill(11, 1, 11, 12, 12, 12, Blocks.BRICKS);
        b.bed(3, 1, 11, Blocks.BED.red(), Direction.NORTH);
        b.bed(5, 1, 11, Blocks.BED.white(), Direction.NORTH);
        b.fill(2, 1, 5, 4, 1, 5, Blocks.SPRUCE_STAIRS);
        b.table(6, 1, 6);
        b.face(10, 1, 12, Blocks.FURNACE, Direction.NORTH);
        b.box(2, 1, 12, ZMineBlocks.BOX3_1, Direction.NORTH, "household");
        b.box(9, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        b.box(12, 1, 5, ZMineBlocks.BOX2, Direction.WEST, "tools");
        if (farm) {
            b.fill(0, 4, 0, 14, 4, 2, Blocks.SPRUCE_SLAB);
            for (int x : new int[]{0, 4, 10, 14}) b.fill(x, 1, 0, x, 3, 0, Blocks.OAK_FENCE);
            b.put(2, 1, 1, Blocks.HAY_BLOCK); b.put(12, 1, 1, Blocks.HAY_BLOCK);
        } else {
            b.air(0, 6, 11, 4, 9, 14);
            b.slab(3, 1, 2, Blocks.BRICK_SLAB, false);
            b.put(4, 1, 1, Blocks.COBWEB);
        }
    }
    private static void townhouse(BuildingBlueprint b) {
        for (int floor = 0; floor < 2; floor++) {
            int y = floor * 5;
            b.shell(y, 4, Blocks.DYED_TERRACOTTA.yellow(), Blocks.DYED_TERRACOTTA.brown(), Blocks.SPRUCE_PLANKS);
            b.window(3, y + 2, 3, 5, y + 3, 3); b.window(9, y + 2, 3, 11, y + 3, 3);
            b.box(2, y + 1, 12, ZMineBlocks.BOX3_1, Direction.NORTH, "household");
            b.bed(3, y + 1, 10, Blocks.BED.blue(), Direction.NORTH);
            b.table(7, y + 1, 7);
            b.box(8, y + 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        }
        b.door(7, 1, 3, Direction.NORTH);
        b.fill(0, 10, 2, 14, 10, 14, Blocks.DARK_OAK_SLAB);
        for (int step = 0; step < 5; step++) {
            b.air(11, step + 2, 6 + step, 12, step + 4, 6 + step);
            b.stairs(11, step + 1, 6 + step, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
            b.stairs(12, step + 1, 6 + step, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        }
        b.fill(2, 5, 1, 12, 5, 2, Blocks.STONE_BRICK_SLAB.defaultBlockState()
            .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
        b.bars(2, 6, 1, 12, 1);
        b.door(7, 6, 3, Direction.NORTH);
        b.box(9, 1, 12, ZMineBlocks.MEDKIT, Direction.NORTH, "medical");
    }

    private static void house(BuildingBlueprint b) {
        b.shell(0, 4, Blocks.DYED_TERRACOTTA.white(), Blocks.DYED_TERRACOTTA.lightGray(), Blocks.SPRUCE_PLANKS);
        for (int x : new int[]{1, 13}) for (int z : new int[]{3, 13}) {
            b.fill(x, 1, z, x, 4, z, Blocks.STRIPPED_SPRUCE_LOG);
        }
        b.fill(1, 4, 3, 13, 4, 3, Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        b.window(3, 2, 3, 5, 3, 3);
        b.window(9, 2, 3, 11, 3, 3);
        b.window(1, 2, 6, 1, 3, 8);
        b.window(13, 2, 9, 13, 3, 11);
        b.window(3, 2, 13, 5, 3, 13);
        b.door(7, 1, 3, Direction.NORTH);
        // Deep porch and pitched roof, with one collapsed rear patch.
        b.fill(5, 0, 1, 9, 0, 2, Blocks.SPRUCE_PLANKS);
        b.fill(5, 1, 1, 5, 3, 1, Blocks.SPRUCE_FENCE);
        b.fill(9, 1, 1, 9, 3, 1, Blocks.SPRUCE_FENCE);
        b.fill(5, 4, 1, 9, 4, 2, Blocks.SPRUCE_SLAB);
        for (int step = 0; step <= 6; step++) {
            for (int z = 2; z <= 14; z++) {
                b.slab(step, 5 + step / 2, z, Blocks.DARK_OAK_SLAB, step % 2 == 1);
                b.slab(14 - step, 5 + step / 2, z, Blocks.DARK_OAK_SLAB, step % 2 == 1);
            }
        }
        for (int z = 2; z <= 14; z++) b.slab(7, 8, z, Blocks.DARK_OAK_SLAB, true);
        for (int y = 5; y <= 7; y++) {
            b.fill((y - 5) * 2 + 1, y, 3, 13 - (y - 5) * 2, y, 3, Blocks.SPRUCE_PLANKS);
            b.fill((y - 5) * 2 + 1, y, 13, 13 - (y - 5) * 2, y, 13, Blocks.SPRUCE_PLANKS);
        }
        b.air(3, 5, 10, 4, 8, 11);
        b.fill(11, 5, 11, 11, 9, 11, Blocks.BRICKS);
        b.put(11, 10, 11, Blocks.BRICK_SLAB);
        // Bedroom, kitchen and living room; the middle remains walkable.
        b.fill(8, 1, 8, 8, 4, 12, Blocks.SPRUCE_PLANKS);
        b.fill(9, 1, 8, 12, 4, 8, Blocks.SPRUCE_PLANKS);
        b.door(10, 1, 8, Direction.NORTH);
        b.bed(11, 1, 10, Blocks.BED.red(), Direction.SOUTH);
        b.box(9, 1, 12, ZMineBlocks.BOX3_1, Direction.NORTH, "household");
        b.fill(2, 1, 11, 5, 1, 12, Blocks.SMOOTH_STONE);
        b.face(2, 1, 11, Blocks.FURNACE, Direction.NORTH);
        b.put(3, 1, 12, Blocks.CAULDRON);
        b.put(5, 1, 12, Blocks.CRAFTING_TABLE);
        b.box(4, 2, 12, ZMineBlocks.BOX3, Direction.NORTH, "food");
        b.box(2, 2, 12, ZMineBlocks.MEDKIT, Direction.NORTH, "medical");
        for (int x = 2; x <= 4; x++) b.stairs(x, 1, 5, Blocks.SPRUCE_STAIRS, Direction.NORTH);
        b.fill(2, 1, 6, 4, 1, 7, Blocks.CARPET.gray());
        b.table(5, 1, 8);
        b.stairs(5, 1, 9, Blocks.SPRUCE_STAIRS, Direction.SOUTH);
        b.fill(10, 1, 4, 12, 2, 4, Blocks.BOOKSHELF);
        b.put(10, 3, 4, Blocks.POTTED_DEAD_BUSH);
        b.put(2, 4, 4, Blocks.COBWEB);
        b.put(12, 4, 12, Blocks.COBWEB);
        b.slab(3, 1, 10, Blocks.COBBLESTONE_SLAB, false);
        b.lamp(7, 3, 2);
    }

    private static void apartments(BuildingBlueprint b) {
        for (int floor = 0; floor < 3; floor++) {
            int y = floor * 5;
            b.shell(y, 4, floor == 0 ? Blocks.STONE_BRICKS : Blocks.BRICKS,
                floor == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.DYED_TERRACOTTA.brown(), Blocks.SPRUCE_PLANKS);
            b.fill(1, y, 3, 13, y, 3, Blocks.POLISHED_ANDESITE);
            for (int x : new int[]{1, 9, 13}) b.fill(x, y + 1, 3, x, y + 4, 3, Blocks.POLISHED_ANDESITE);
            b.window(3, y + 2, 3, 5, y + 3, 3);
            b.window(7, y + 2, 3, 8, y + 3, 3);
            b.window(11, y + 2, 3, 12, y + 3, 3);
            b.window(1, y + 2, 5, 1, y + 3, 6);
            b.window(1, y + 2, 10, 1, y + 3, 11);
            b.window(4, y + 2, 13, 6, y + 3, 13);
            b.fill(9, y + 1, 4, 9, y + 4, 12, Blocks.DYED_TERRACOTTA.lightGray());
            b.air(9, y + 1, 5, 9, y + 2, 5);
            b.air(9, y + 1, 10, 9, y + 2, 10);
            if (floor > 0) {
                b.fill(2, y + 1, 8, 8, y + 4, 8, Blocks.DYED_TERRACOTTA.lightGray());
                apartmentRoom(b, y, 4, floor == 1);
                apartmentRoom(b, y, 9, floor != 1);
                b.fill(2, y, 1, 8, y, 2, Blocks.STONE_BRICK_SLAB.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.SlabBlock.TYPE, net.minecraft.world.level.block.state.properties.SlabType.TOP));
                b.bars(2, y + 1, 1, 8, 1);
                b.bars(2, y + 1, 1, 2, 2);
                b.bars(8, y + 1, 1, 8, 2);
                b.door(6, y + 1, 3, Direction.NORTH);
            }
            b.put(12, y + 4, 12, Blocks.COBWEB);
        }
        // Lobby entrance, reception desk and abandoned mail/supplies.
        b.air(6, 1, 3, 7, 3, 3);
        b.fill(5, 4, 1, 9, 4, 2, Blocks.POLISHED_ANDESITE_SLAB);
        b.fill(2, 1, 10, 4, 1, 10, Blocks.SPRUCE_PLANKS);
        b.box(2, 2, 10, ZMineBlocks.BOX3, Direction.NORTH, "household");
        b.box(3, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "tools");
        b.box(8, 2, 12, ZMineBlocks.MEDKIT_WALL, Direction.NORTH, "medical");
        b.fill(2, 1, 5, 2, 2, 7, Blocks.BOOKSHELF);
        b.stairs(4, 1, 6, Blocks.SPRUCE_STAIRS, Direction.WEST);
        b.stairs(4, 1, 7, Blocks.SPRUCE_STAIRS, Direction.WEST);
        b.fill(6, 1, 5, 7, 1, 11, Blocks.CARPET.red());
        b.fill(1, 15, 3, 13, 15, 13, Blocks.SMOOTH_STONE);
        // Three usable flights, with openings and full headroom, reach the roof.
        for (int floor = 0; floor < 3; floor++) for (int step = 0; step < 5; step++) {
            int y = floor * 5 + step + 1;
            b.air(11, y + 1, 7 + step, 12, y + 3, 7 + step);
            for (int x = 11; x <= 12; x++) b.stairs(x, y, 7 + step, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
        }
        b.bars(1, 16, 3, 13, 3);
        b.bars(1, 16, 13, 13, 13);
        b.bars(1, 16, 4, 1, 12);
        b.bars(13, 16, 4, 13, 12);
        // Water tank and maintenance equipment silhouette.
        for (int x : new int[]{3, 6}) for (int z : new int[]{6, 9}) b.fill(x, 16, z, x, 17, z, Blocks.IRON_BARS);
        b.fill(3, 18, 6, 6, 19, 9, Blocks.CONCRETE.gray());
        b.fill(3, 20, 6, 6, 20, 9, Blocks.SMOOTH_STONE_SLAB);
        b.fill(2, 16, 11, 4, 16, 12, Blocks.POLISHED_ANDESITE);
        b.box(7, 16, 12, ZMineBlocks.BOX2, Direction.NORTH, "tools");
        b.fill(9, 16, 4, 9, 20, 4, Blocks.IRON_BARS);
        b.bars(8, 19, 4, 10, 4);
    }

    private static void apartmentRoom(BuildingBlueprint b, int y, int z, boolean redBed) {
        b.bed(3, y + 1, z + 1, redBed ? Blocks.BED.red() : Blocks.BED.gray(), Direction.SOUTH);
        b.box(2, y + 1, z, ZMineBlocks.BOX3_1, Direction.EAST, "household");
        b.put(7, y + 1, z, Blocks.CAULDRON);
        b.face(8, y + 1, z, Blocks.FURNACE, Direction.SOUTH);
        b.table(6, y + 1, z + 2);
        b.stairs(7, y + 1, z + 2, Blocks.SPRUCE_STAIRS, Direction.EAST);
        b.box(8, y + 1, z + 3, ZMineBlocks.BOX3, Direction.WEST, "food");
        b.put(2, y + 4, z, Blocks.COBWEB);
    }

    private static void market(BuildingBlueprint b) {
        b.shell(0, 4, Blocks.DYED_TERRACOTTA.white(), Blocks.DYED_TERRACOTTA.lightGray(), Blocks.SMOOTH_STONE);
        b.fill(1, 4, 3, 13, 4, 3, Blocks.CONCRETE.green());
        b.window(2, 1, 3, 5, 3, 3);
        b.window(9, 1, 3, 12, 3, 3);
        b.air(6, 1, 3, 7, 3, 3);
        flatRoof(b, 5);
        for (int x = 1; x <= 13; x++) {
            b.fill(x, 4, 1, x, 4, 2, x % 2 == 0 ? Blocks.CONCRETE.white() : Blocks.CONCRETE.green());
        }
        b.fill(4, 6, 3, 10, 7, 3, Blocks.CONCRETE.green());
        // White geometric sign, legible without text entities.
        b.fill(6, 6, 2, 8, 6, 2, Blocks.QUARTZ_SLAB);
        b.fill(2, 1, 11, 12, 3, 11, Blocks.DYED_TERRACOTTA.lightGray());
        b.air(10, 1, 11, 11, 2, 11);
        for (int x : new int[]{3, 6}) {
            for (int z = 6; z <= 9; z++) {
                b.put(x, 1, z, Blocks.SPRUCE_PLANKS);
                b.slab(x, 3, z, Blocks.SPRUCE_SLAB, true);
            }
            b.box(x, 2, 6, ZMineBlocks.BOX3, Direction.EAST, "food");
            b.box(x, 2, 8, ZMineBlocks.BOX3_1, Direction.EAST, "food");
        }
        b.fill(9, 1, 5, 11, 1, 5, Blocks.SMOOTH_QUARTZ);
        b.face(10, 2, 5, Blocks.STONE_BRICK_STAIRS, Direction.SOUTH);
        b.put(10, 3, 5, Blocks.STONE_PRESSURE_PLATE);
        b.box(12, 1, 5, ZMineBlocks.BOX3, Direction.WEST, "household");
        b.fill(12, 1, 7, 12, 2, 9, Blocks.IRON_BLOCK);
        b.fill(11, 2, 7, 11, 2, 9, Blocks.STAINED_GLASS.lightBlue());
        b.box(2, 1, 12, ZMineBlocks.BOX1, Direction.NORTH, "food");
        b.box(4, 1, 12, ZMineBlocks.BOX2, Direction.NORTH, "household");
        b.box(7, 2, 12, ZMineBlocks.MEDKIT_WALL, Direction.NORTH, "medical");
        b.put(2, 4, 10, Blocks.COBWEB);
        b.put(12, 4, 4, Blocks.COBWEB);
        b.slab(4, 1, 4, Blocks.COBBLESTONE_SLAB, false);
        b.air(2, 5, 8, 3, 5, 9);
        b.lamp(7, 4, 8);
    }

    private static void clinic(BuildingBlueprint b) {
        b.shell(0, 4, Blocks.DYED_TERRACOTTA.white(), Blocks.DYED_TERRACOTTA.lightGray(), Blocks.SMOOTH_QUARTZ);
        b.fill(1, 1, 3, 13, 1, 3, Blocks.CONCRETE.red());
        b.window(2, 2, 3, 4, 3, 3);
        b.window(10, 2, 3, 12, 3, 3);
        b.window(1, 2, 8, 1, 3, 10);
        b.window(13, 2, 8, 13, 3, 10);
        b.air(6, 1, 3, 8, 3, 3);
        flatRoof(b, 5);
        b.fill(5, 4, 1, 9, 4, 2, Blocks.SMOOTH_QUARTZ_SLAB);
        b.fill(5, 6, 3, 9, 8, 3, Blocks.CONCRETE.white());
        b.fill(7, 6, 2, 7, 8, 2, Blocks.CONCRETE.red());
        b.fill(6, 7, 2, 8, 7, 2, Blocks.CONCRETE.red());
        // Reception, separate ward and examination room.
        b.fill(2, 1, 6, 4, 1, 6, Blocks.SMOOTH_QUARTZ);
        b.box(2, 2, 6, ZMineBlocks.MEDKIT, Direction.SOUTH, "medical");
        b.put(4, 2, 6, Blocks.FLOWER_POT);
        for (int x = 10; x <= 12; x++) b.stairs(x, 1, 5, Blocks.QUARTZ_STAIRS, Direction.NORTH);
        b.fill(2, 1, 7, 5, 4, 7, Blocks.DYED_TERRACOTTA.white());
        b.fill(9, 1, 7, 12, 4, 7, Blocks.DYED_TERRACOTTA.white());
        b.fill(7, 1, 8, 7, 4, 12, Blocks.DYED_TERRACOTTA.white());
        b.air(7, 1, 9, 7, 2, 9);
        b.bed(3, 1, 10, Blocks.BED.white(), Direction.SOUTH);
        b.bed(5, 1, 10, Blocks.BED.lightBlue(), Direction.SOUTH);
        b.put(2, 1, 12, Blocks.SMOOTH_QUARTZ);
        b.box(2, 2, 12, ZMineBlocks.MEDKIT_MILITARY, Direction.NORTH, "medical");
        b.box(6, 2, 12, ZMineBlocks.MEDKIT_WALL, Direction.NORTH, "medical");
        b.bed(10, 1, 10, Blocks.BED.white(), Direction.SOUTH);
        b.fill(12, 1, 9, 12, 1, 12, Blocks.SMOOTH_QUARTZ);
        b.put(12, 1, 10, Blocks.CAULDRON);
        b.box(12, 2, 12, ZMineBlocks.MEDKIT, Direction.WEST, "medical");
        b.box(9, 1, 12, ZMineBlocks.BOX2, Direction.NORTH, "medical");
        b.box(2, 1, 4, ZMineBlocks.BOX3, Direction.SOUTH, "food");
        b.put(12, 4, 12, Blocks.COBWEB);
        b.put(2, 4, 8, Blocks.COBWEB);
        b.fill(6, 1, 4, 8, 1, 6, Blocks.CARPET.lightGray());
        b.lamp(7, 4, 5);
        b.air(10, 5, 9, 11, 5, 10);
    }

    private static void warehouse(BuildingBlueprint b) {
        b.shell(0, 6, Blocks.BRICKS, Blocks.DYED_TERRACOTTA.brown(), Blocks.POLISHED_ANDESITE);
        for (int x : new int[]{1, 5, 9, 13}) {
            b.fill(x, 1, 3, x, 6, 3, Blocks.POLISHED_DEEPSLATE);
            b.fill(x, 1, 13, x, 6, 13, Blocks.POLISHED_DEEPSLATE);
        }
        b.air(6, 1, 3, 8, 3, 3);
        b.fill(6, 4, 3, 8, 5, 3, Blocks.IRON_BLOCK);
        b.window(1, 4, 6, 1, 5, 9);
        b.window(13, 4, 6, 13, 5, 9);
        for (int x = 0; x <= 14; x++) for (int z = 2; z <= 14; z++) {
            b.slab(x, 7 + (x > 3 && x < 11 ? 1 : 0), z, Blocks.DEEPSLATE_TILE_SLAB, false);
        }
        for (int z : new int[]{5, 9, 13}) {
            b.fill(2, 6, z, 12, 6, z, Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        }
        b.air(3, 7, 7, 5, 8, 9);
        // Two storage racks, reachable from a generous central aisle.
        for (int x : new int[]{2, 11}) {
            b.fill(x, 1, 6, x + 1, 1, 10, Blocks.SPRUCE_SLAB);
            b.fill(x, 3, 6, x + 1, 3, 10, Blocks.SPRUCE_SLAB);
            for (int z : new int[]{6, 10}) b.fill(x, 1, z, x, 4, z, Blocks.IRON_BARS);
            Direction facing = x == 2 ? Direction.EAST : Direction.WEST;
            b.box(x + (x == 2 ? 1 : 0), 2, 7, ZMineBlocks.BOX2, facing, "tools");
            b.box(x + (x == 2 ? 1 : 0), 2, 9, ZMineBlocks.BOX1, facing, "tools");
            b.box(x + (x == 2 ? 1 : 0), 4, 8, ZMineBlocks.BOX3_1, facing, "military");
        }
        b.fill(5, 1, 11, 7, 1, 12, Blocks.SPRUCE_SLAB);
        b.box(5, 2, 11, ZMineBlocks.BOX1, Direction.NORTH, "tools");
        b.box(6, 2, 12, ZMineBlocks.BOX2, Direction.NORTH, "military");
        b.box(6, 3, 12, ZMineBlocks.BOX3, Direction.NORTH, "food");
        b.put(10, 1, 12, Blocks.CRAFTING_TABLE);
        b.face(11, 1, 12, Blocks.BLAST_FURNACE, Direction.NORTH);
        b.put(9, 1, 12, Blocks.SMITHING_TABLE);
        b.box(10, 2, 12, ZMineBlocks.MEDKIT_MILITARY, Direction.NORTH, "medical");
        b.box(2, 2, 4, ZMineBlocks.MEDKIT_WALL, Direction.SOUTH, "medical");
        // Loading apron, bollards and hazard stripe.
        for (int x = 4; x <= 10; x++) b.put(x, 0, 2, x % 2 == 0 ? Blocks.CONCRETE.yellow() : Blocks.CONCRETE.black());
        b.put(4, 1, 1, Blocks.COBBLESTONE_WALL);
        b.put(10, 1, 1, Blocks.COBBLESTONE_WALL);
        b.slab(8, 1, 9, Blocks.COBBLESTONE_SLAB, false);
        b.put(2, 6, 12, Blocks.COBWEB);
        b.put(12, 6, 4, Blocks.COBWEB);
        b.lamp(7, 5, 5);
    }

    private static void flatRoof(BuildingBlueprint b, int y) {
        b.weathered(0, y, 2, 14, y, 14, Blocks.SMOOTH_STONE, Blocks.CRACKED_STONE_BRICKS);
        b.fill(1, y + 1, 3, 13, y + 1, 3, Blocks.STONE_BRICK_SLAB);
        b.fill(1, y + 1, 13, 13, y + 1, 13, Blocks.STONE_BRICK_SLAB);
        b.fill(1, y + 1, 4, 1, y + 1, 12, Blocks.STONE_BRICK_SLAB);
        b.fill(13, y + 1, 4, 13, y + 1, 12, Blocks.STONE_BRICK_SLAB);
        b.fill(10, y + 1, 11, 11, y + 1, 12, Blocks.POLISHED_ANDESITE);
        b.put(10, y + 2, 11, Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE);
    }
}
