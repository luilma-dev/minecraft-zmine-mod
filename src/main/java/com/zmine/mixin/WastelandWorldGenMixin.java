package com.zmine.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ProtoChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ProtoChunk.class)
public abstract class WastelandWorldGenMixin {

    @ModifyVariable(method = "setBlockState", at = @At("HEAD"), argsOnly = true)
    private BlockState modifyWorldGenBlocks(BlockState state, BlockPos pos) {
        if (state == null) {
            return null;
        }

        // Substitui Grama por Coarse Dirt (Terra Infértil)
        if (state.is(Blocks.GRASS_BLOCK)) {
            return Blocks.COARSE_DIRT.defaultBlockState();
        }

        // Remove folhas das árvores
        if (state.is(BlockTags.LEAVES)) {
            return Blocks.AIR.defaultBlockState();
        }

        return state;
    }
}
