package com.zmine.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Static Blockbench model, with the same persistent inventory contract as a barrel. */
@SuppressWarnings({"null"})
public class LootBoxBlock extends BaseEntityBlock {
    public static final MapCodec<LootBoxBlock> CODEC = simpleCodec(LootBoxBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public LootBoxBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override public MapCodec<LootBoxBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LootBoxBlockEntity(pos, state);
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                         Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel && level.getBlockEntity(pos) instanceof LootBoxBlockEntity box) {
            player.openMenu(box);
        }
        return InteractionResult.SUCCESS;
    }

    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level,
                                                          BlockPos pos, boolean moved) {
        // BlockEntity.preRemoveSideEffects already drops the inventory in Minecraft 26.2.
        Containers.updateNeighboursAfterDestroy(state, level, pos);
    }

    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        double x0 = 0, z0 = 0, x1 = 16, z1 = 16, height = 16;
        if (this == ZMineBlocks.BOX3) { x0 = 1.85; x1 = 14.15; z0 = 3.84; z1 = 12.15; height = 4.72; }
        if (this == ZMineBlocks.BOX3_1) { x0 = 2; x1 = 14; z0 = 3; z1 = 13; height = 13.5; }
        if (this == ZMineBlocks.MEDKIT) { x0 = 2.3; x1 = 13.7; z0 = 4.15; z1 = 13; height = 4.8; }
        if (this == ZMineBlocks.MEDKIT_MILITARY) { x0 = 3; x1 = 13; z0 = 4.8; z1 = 11; height = 8; }
        if (this == ZMineBlocks.MEDKIT_WALL) { x0 = 4; x1 = 12; z0 = 13.15; z1 = 16; height = 12; }
        return switch (state.getValue(FACING)) {
            case SOUTH -> Block.box(16 - x1, 0, 16 - z1, 16 - x0, height, 16 - z0);
            case EAST -> Block.box(16 - z1, 0, x0, 16 - z0, height, x1);
            case WEST -> Block.box(z0, 0, 16 - x1, z1, height, 16 - x0);
            default -> Block.box(x0, 0, z0, x1, height, z1);
        };
    }
}
