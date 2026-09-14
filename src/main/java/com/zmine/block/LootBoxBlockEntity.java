package com.zmine.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

@SuppressWarnings({"null"})
public class LootBoxBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public LootBoxBlockEntity(BlockPos pos, BlockState state) {
        super(ZMineBlocks.LOOT_BOX_ENTITY, pos, state);
    }
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!trySaveLootTable(output)) ContainerHelper.saveAllItems(output, items);
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(input)) ContainerHelper.loadAllItems(input, items);
    }
    @Override public int getContainerSize() { return 27; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return getBlockState().getBlock().getName(); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return ChestMenu.threeRows(id, inventory, this);
    }
    @Override public void startOpen(ContainerUser user) {
        if (level != null && !user.getLivingEntity().isSpectator()) {
            level.playSound(null, worldPosition, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.6f, 0.85f);
        }
    }
    @Override public void stopOpen(ContainerUser user) {
        if (level != null && !user.getLivingEntity().isSpectator()) {
            level.playSound(null, worldPosition, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.5f, 0.85f);
        }
    }
}
