package com.zmine.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

@SuppressWarnings("null")
public final class RuinsTerrain {
    private RuinsTerrain() { }

    public static BlockPos ground(WorldGenLevel level, int x, int z) {
        var heightmap = level instanceof ServerLevel ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG;
        BlockPos pos = new BlockPos(x, level.getHeight(heightmap, x, z) - 1, z);
        for (int scan = 0; scan < 32 && vegetation(level.getBlockState(pos)); scan++) pos = pos.below();
        return pos;
    }

    public static boolean dryNaturalGround(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isSolidRender() && state.getFluidState().isEmpty() && level.getFluidState(pos.above()).isEmpty()
            && (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL)
            || stoneOrSediment(state) || state.is(Blocks.SNOW_BLOCK));
    }

    public static boolean natural(BlockState state) {
        return vegetation(state) || !state.getFluidState().isEmpty() || state.is(BlockTags.DIRT)
            || state.is(BlockTags.SAND) || stoneOrSediment(state)
            || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK)
            || state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.BLUE_ICE);
    }

    private static boolean stoneOrSediment(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.TERRACOTTA)
            || state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE)
            || state.is(Blocks.CALCITE);
    }

    /** Keep the local geology on graded slopes, including stable support below loose sand. */
    public static BlockState slopeSurface(BlockState original) {
        if (original.is(BlockTags.DIRT)) return Blocks.COARSE_DIRT.defaultBlockState();
        return original;
    }

    public static BlockState slopeFill(BlockState original) {
        if (original.is(Blocks.SAND) || original.is(Blocks.SANDSTONE)) return Blocks.SANDSTONE.defaultBlockState();
        if (original.is(Blocks.RED_SAND) || original.is(Blocks.RED_SANDSTONE)) return Blocks.RED_SANDSTONE.defaultBlockState();
        if (original.is(BlockTags.TERRACOTTA)) return original;
        if (original.is(BlockTags.DIRT)) return Blocks.DIRT.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    public static boolean vegetation(BlockState state) {
        return state.isAir() || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)
            || state.is(Blocks.SNOW) || state.canBeReplaced();
    }
}
