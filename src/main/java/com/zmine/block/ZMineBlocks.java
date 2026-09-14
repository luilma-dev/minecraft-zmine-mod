package com.zmine.block;

import com.zmine.ZMineMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.SoundType;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.world.item.CreativeModeTabs;

@SuppressWarnings("null")
public class ZMineBlocks {

    public static final Block BOX1 = registerBlock("box1");
    public static final Block BOX2 = registerBlock("box2");
    public static final Block BOX3 = registerBlock("box3");
    public static final Block BOX3_1 = registerBlock("box3_1");
    
    public static final Block MEDKIT = registerBlock("medkit");
    public static final Block MEDKIT_MILITARY = registerBlock("medkit_military");
    public static final Block MEDKIT_WALL = registerBlock("medkit_wall");

    public static final List<Block> ALL = List.of(BOX1, BOX2, BOX3, BOX3_1, MEDKIT, MEDKIT_MILITARY, MEDKIT_WALL);
    public static final BlockEntityType<LootBoxBlockEntity> LOOT_BOX_ENTITY = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE, ZMineMod.id("loot_box"),
        new BlockEntityType<>(LootBoxBlockEntity::new, Set.copyOf(ALL)));

    private static Block registerBlock(String name) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, ZMineMod.id(name));
        Block block = new LootBoxBlock(BlockBehaviour.Properties.of().setId(key).strength(2.0f).sound(SoundType.WOOD).noOcclusion());
        registerBlockItem(name, block);
        return Registry.register(BuiltInRegistries.BLOCK, ZMineMod.id(name), block);
    }

    private static Item registerBlockItem(String name, Block block) {
        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, ZMineMod.id(name));
        return Registry.register(BuiltInRegistries.ITEM, ZMineMod.id(name),
            new BlockItem(block, new Item.Properties().setId(itemKey)));
    }

    public static void registerModBlocks() {
        ZMineMod.LOGGER.info("Registrando Caixas de Loot ZMine!");
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
            .register(output -> ALL.forEach(output::accept));
    }
}
