package com.zmine.worldgen;

import com.zmine.ZMineMod;
import com.zmine.block.LootBoxBlock;
import com.zmine.block.LootBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.LinkedHashMap;
import java.util.Map;

/** A complete 15x15 lot. Rotations stay inside the same chunk during decoration. */
@SuppressWarnings("null")
public final class BuildingBlueprint {
    public static final int SIZE = 15;
    private final int size;
    private final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    private final Map<BlockPos, ResourceKey<LootTable>> loot = new LinkedHashMap<>();
    private final RandomSource random;

    public BuildingBlueprint(RandomSource random) { this(random, SIZE); }
    public BuildingBlueprint(RandomSource random, int size) { this.random = random; this.size = size; }
    public int size() { return size; }
    public Map<BlockPos, BlockState> blocks() { return Map.copyOf(blocks); }
    public Map<BlockPos, ResourceKey<LootTable>> loot() { return Map.copyOf(loot); }
    public int height() { return blocks.keySet().stream().mapToInt(BlockPos::getY).max().orElse(0) + 1; }
    public BuildingBlueprint rotated(Rotation rotation) {
        if (rotation == Rotation.NONE) return this;
        var result = new BuildingBlueprint(RandomSource.create(0), size);
        blocks.forEach((pos, state) -> result.blocks.put(rotate(pos, rotation, size), state.rotate(rotation)));
        loot.forEach((pos, table) -> result.loot.put(rotate(pos, rotation, size), table));
        return result;
    }

    public void put(int x, int y, int z, Block block) { put(x, y, z, block.defaultBlockState()); }
    public void put(int x, int y, int z, BlockState state) {
        if (x < 0 || x >= size || z < 0 || z >= size || y < 0) {
            throw new IllegalArgumentException("Building outside its lot: " + x + "," + y + "," + z);
        }
        BlockPos pos = new BlockPos(x, y, z);
        blocks.put(pos, state);
        loot.remove(pos);
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block block) {
        fill(x0, y0, z0, x1, y1, z1, block.defaultBlockState());
    }
    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            put(x, y, z, state);
        }
    }
    public void air(int x0, int y0, int z0, int x1, int y1, int z1) {
        fill(x0, y0, z0, x1, y1, z1, Blocks.AIR);
    }
    public void weathered(int x0, int y0, int z0, int x1, int y1, int z1, Block primary, Block worn) {
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) {
            put(x, y, z, random.nextInt(6) == 0 ? worn : primary);
        }
    }

    public void shell(int floor, int wallHeight, Block wall, Block worn, Block flooring) {
        fill(1, floor, 3, 13, floor, 13, flooring);
        air(2, floor + 1, 4, 12, floor + wallHeight, 12);
        weathered(1, floor + 1, 3, 13, floor + wallHeight, 3, wall, worn);
        weathered(1, floor + 1, 13, 13, floor + wallHeight, 13, wall, worn);
        weathered(1, floor + 1, 4, 1, floor + wallHeight, 12, wall, worn);
        weathered(13, floor + 1, 4, 13, floor + wallHeight, 12, wall, worn);
    }

    public void window(int x0, int y0, int z0, int x1, int y1, int z1) {
        // Full glass blocks keep intentional broken edges, even without neighbor updates.
        fill(x0, y0, z0, x1, y1, z1, Blocks.STAINED_GLASS.lightGray());
        put(x0, y1, z0, Blocks.AIR);
    }

    public void stairs(int x, int y, int z, Block stairs, Direction facing) {
        put(x, y, z, stairs.defaultBlockState().setValue(StairBlock.FACING, facing));
    }
    public void slab(int x, int y, int z, Block slab, boolean top) {
        put(x, y, z, slab.defaultBlockState().setValue(SlabBlock.TYPE, top ? SlabType.TOP : SlabType.BOTTOM));
    }
    public void face(int x, int y, int z, Block block, Direction direction) {
        BlockState state = block.defaultBlockState();
        put(x, y, z, state.setValue(state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
            ? BlockStateProperties.HORIZONTAL_FACING : BlockStateProperties.FACING, direction));
    }
    public void door(int x, int y, int z, Direction facing) {
        BlockState state = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, facing);
        put(x, y, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
        put(x, y + 1, z, state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
    }
    public void bed(int x, int y, int z, Block bed, Direction direction) {
        put(x, y, z, bed.defaultBlockState().setValue(BedBlock.FACING, direction).setValue(BedBlock.PART, BedPart.FOOT));
        put(x + direction.getStepX(), y, z + direction.getStepZ(),
            bed.defaultBlockState().setValue(BedBlock.FACING, direction).setValue(BedBlock.PART, BedPart.HEAD));
    }
    public void box(int x, int y, int z, Block block, Direction facing, String table) {
        put(x, y, z, block.defaultBlockState().setValue(LootBoxBlock.FACING, facing));
        loot.put(new BlockPos(x, y, z), ResourceKey.create(Registries.LOOT_TABLE, ZMineMod.id("chests/" + table)));
    }
    public void table(int x, int y, int z) {
        put(x, y, z, Blocks.SPRUCE_FENCE);
        put(x, y + 1, z, Blocks.SPRUCE_PRESSURE_PLATE);
    }
    public void lamp(int x, int y, int z) {
        put(x, y, z, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
    }
    public void bars(int x0, int y, int z0, int x1, int z1) {
        BlockState bars = Blocks.IRON_BARS.defaultBlockState();
        if (x0 != x1) bars = bars.setValue(IronBarsBlock.EAST, true).setValue(IronBarsBlock.WEST, true);
        if (z0 != z1) bars = bars.setValue(IronBarsBlock.NORTH, true).setValue(IronBarsBlock.SOUTH, true);
        fill(x0, y, z0, x1, y, z1, bars);
    }

    public void lot() {
        weathered(0, 0, 0, 14, 0, 14, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        fill(2, 0, 1, 4, 0, 1, Blocks.COARSE_DIRT);
        fill(10, 0, 1, 12, 0, 1, Blocks.COARSE_DIRT);
        put(3, 1, 1, Blocks.DEAD_BUSH);
        put(11, 1, 1, Blocks.DEAD_BUSH);
        fill(5, 0, 0, 9, 0, 2, Blocks.SMOOTH_STONE);
    }

    public static BlockPos rotate(BlockPos pos, Rotation rotation) {
        return rotate(pos, rotation, SIZE);
    }

    public static BlockPos rotate(BlockPos pos, Rotation rotation, int size) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(size - 1 - pos.getZ(), pos.getY(), pos.getX());
            case CLOCKWISE_180 -> new BlockPos(size - 1 - pos.getX(), pos.getY(), size - 1 - pos.getZ());
            case COUNTERCLOCKWISE_90 -> new BlockPos(pos.getZ(), pos.getY(), size - 1 - pos.getX());
            default -> pos;
        };
    }

    public void place(WorldGenLevel level, BlockPos origin, Rotation rotation, RandomSource lootRandom) {
        // Clear only this lot. Feature validates terrain and existing block entities first.
        int maxY = height();
        for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
            for (int y = 1; y < maxY; y++) level.setBlock(origin.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
            for (int y = -1; y >= -4; y--) {
                BlockPos support = origin.offset(x, y, z);
                if (support.getY() < level.getMinY()) break;
                if (level.getBlockState(support).isSolidRender()) break;
                level.setBlock(support, Blocks.COBBLESTONE.defaultBlockState(), 2);
            }
        }
        placeOverlay(level, origin, rotation, lootRandom);
    }

    /** Place a prop without clearing the surrounding terrain or creating a foundation. */
    public void placeOverlay(WorldGenLevel level, BlockPos origin, Rotation rotation, RandomSource lootRandom) {
        // Doors, beds and wall decorations must not update while their other half/support is still absent.
        blocks.forEach((pos, state) -> level.setBlock(origin.offset(rotate(pos, rotation, size)), state.rotate(rotation), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE));
        blocks.forEach((pos, state) -> connect(level, origin.offset(rotate(pos, rotation, size))));
        loot.forEach((pos, table) -> {
            BlockPos target = origin.offset(rotate(pos, rotation, size));
            if (!(level.getBlockEntity(target) instanceof LootBoxBlockEntity box)) {
                throw new IllegalStateException("Missing loot box at " + target);
            }
            box.setLootTable(table, lootRandom.nextLong());
            box.setChanged();
        });
    }

    /** Generate a large shared template one chunk at a time, with deterministic containers. */
    public void placeSlice(WorldGenLevel level, BlockPos origin, int chunkX, int chunkZ, long seed) {
        blocks.forEach((pos, state) -> {
            var target = origin.offset(pos);
            if ((target.getX() >> 4) == chunkX && (target.getZ() >> 4) == chunkZ)
                level.setBlock(target, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        });
        blocks.forEach((pos, state) -> {
            var target = origin.offset(pos);
            if ((target.getX() >> 4) == chunkX && (target.getZ() >> 4) == chunkZ
                && Math.floorMod(target.getX(), 16) > 0 && Math.floorMod(target.getX(), 16) < 15
                && Math.floorMod(target.getZ(), 16) > 0 && Math.floorMod(target.getZ(), 16) < 15) connect(level, target);
        });
        loot.forEach((pos, table) -> {
            var target = origin.offset(pos);
            if ((target.getX() >> 4) == chunkX && (target.getZ() >> 4) == chunkZ
                && level.getBlockEntity(target) instanceof LootBoxBlockEntity box) {
                box.setLootTable(table, DistrictLayout.hash(seed ^ target.asLong()));
                box.setChanged();
            }
        });
    }

    private static void connect(WorldGenLevel level, BlockPos target) {
        var state = level.getBlockState(target);
        var block = state.getBlock();
        if (block instanceof FenceBlock || block instanceof WallBlock || block instanceof IronBarsBlock || block instanceof StairBlock)
            level.setBlock(target, Block.updateFromNeighbourShapes(state, level, target), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
}
