package com.zmine;

import com.zmine.block.LootBoxBlock;
import com.zmine.block.LootBoxBlockEntity;
import com.zmine.block.ZMineBlocks;
import com.zmine.worldgen.AbandonedBuildings;
import com.zmine.worldgen.BuildingBlueprint;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({"null", "unused", "removal"})
public class ZMineGameTests {
    @GameTest(structure = "zmine-tests:lot", maxTicks = 100, skyAccess = true) public void terrainPlacementRejectsWaterCliffsAndExistingBuildings(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(16, 5, 16));
        BlockPos origin = new BlockPos(center.getX() & ~15, center.getY(), center.getZ() & ~15);
        var feature = new com.zmine.worldgen.feature.AbandonedBuildingFeature(AbandonedBuildings.Kind.HOUSE);
        Runnable reset = () -> {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, -4, 0), origin.offset(14, 24, 14))) {
                level.setBlock(pos, (pos.getY() <= origin.getY() ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
            }
        };
        java.util.function.BooleanSupplier place = () -> feature.place(
            net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
            level, level.getChunkSource().getGenerator(), RandomSource.create(73), origin.above());
        reset.run();
        helper.assertTrue(place.getAsBoolean(), "Flat dry terrain must accept a building");
        helper.assertFalse(place.getAsBoolean(), "A second building must not overwrite the first");
        reset.run();
        level.setBlock(origin.offset(7, 1, 7), Blocks.WATER.defaultBlockState(), 2);
        helper.assertFalse(place.getAsBoolean(), "Flooded footprints must be rejected");
        reset.run();
        for (int y = 1; y <= 6; y++) level.setBlock(origin.offset(7, y, 7), Blocks.STONE.defaultBlockState(), 2);
        helper.assertFalse(place.getAsBoolean(), "Steep terrain must be rejected");
        reset.run();
        level.setBlock(origin.offset(7, 1, 7), Blocks.CHEST.defaultBlockState(), 2);
        helper.assertFalse(place.getAsBoolean(), "Existing inventories must be preserved");
        reset.run();
        level.setBlock(origin.offset(7, -1, 7), Blocks.CHEST.defaultBlockState(), 2);
        helper.assertFalse(place.getAsBoolean(), "Building foundations must preserve buried inventories");
        helper.assertTrue(level.getBlockEntity(origin.offset(7, -1, 7)) != null, "Buried inventory was overwritten");
        helper.succeed();
    }

    @GameTest public void neighborhoodsAreDeterministicAndFaceConnectedRoads(GameTestHelper helper) {
        int towns = 0, plots = 0;
        for (int cx = -32; cx < 32; cx++) for (int cz = -32; cz < 32; cz++) {
            var lot = com.zmine.worldgen.DistrictLayout.at(91, cx, cz);
            helper.assertTrue(lot.equals(com.zmine.worldgen.DistrictLayout.at(91, cx, cz)), "Town plan must be deterministic");
            if (lot.zone() == com.zmine.worldgen.DistrictLayout.Zone.CROSSROADS) towns++;
            if (lot.zone() == com.zmine.worldgen.DistrictLayout.Zone.BUILDING) {
                Direction entrance = lot.facing().rotate(Direction.NORTH);
                var facing = com.zmine.worldgen.DistrictLayout.at(91, cx + entrance.getStepX(), cz + entrance.getStepZ());
                helper.assertTrue(facing.zone() == com.zmine.worldgen.DistrictLayout.Zone.ROAD_NS
                    || facing.zone() == com.zmine.worldgen.DistrictLayout.Zone.ROAD_EW,
                    "Every building entrance must face a road, including negative coordinates");
                plots++;
            }
        }
        helper.assertTrue(towns > 0 && plots > 20, "City blocks must group buildings along the street grid");
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:lot", skyAccess = true) public void roadsAndDeadTreesGenerateWithLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(16, 5, 16));
        BlockPos origin = new BlockPos(center.getX() & ~15, center.getY(), center.getZ() & ~15);
        Runnable reset = () -> {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(0, -3, 0), origin.offset(15, 12, 15))) {
                level.setBlock(pos, (pos.getY() <= origin.getY() ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
            }
        };
        reset.run();
        helper.assertTrue(com.zmine.worldgen.ApocalypseScenery.road(level, origin.getX(), origin.getZ(),
            com.zmine.worldgen.DistrictLayout.Zone.ROAD_NS, RandomSource.create(5)), "Road must generate on dry ground");
        double centerLine = com.zmine.worldgen.RoadGeometry.center(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, 
            new com.zmine.worldgen.DistrictLayout.Lot(com.zmine.worldgen.DistrictLayout.Zone.ROAD_NS, null, Rotation.NONE, false, false), 0);
        int expectedX = (int) Math.round(centerLine - 0.5);
        helper.assertTrue(level.getBlockState(origin.offset(expectedX, 0, 0)).is(Blocks.CONCRETE.yellow()), "Road must reach chunk edge");
        boolean supplies = false;
        for (int x = 0; x < 16; x++) if (level.getBlockEntity(origin.offset(x, 1, 6)) instanceof LootBoxBlockEntity box)
            supplies |= !box.isEmpty() && level.getBlockState(origin.offset(x, 0, 6)).isSolidRender();
        helper.assertTrue(supplies, "Street supplies must contain loot on the curved sidewalk");
        reset.run();
        helper.assertTrue(com.zmine.worldgen.ApocalypseScenery.wilderness(level, origin.getX(), origin.getZ(), RandomSource.create(5)),
            "Dead trees must generate on dry ground");
        helper.succeed();
    }

    @GameTest public void inventoriesPersistAndDoNotRefill(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        for (Block block : ZMineBlocks.ALL) {
            level.setBlock(pos, block.defaultBlockState(), 3);
            var box = (LootBoxBlockEntity) level.getBlockEntity(pos);
            helper.assertTrue(box != null, "All seven models need a block entity");
            box.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, ZMineMod.id("chests/household")), 12345L);
            var pending = box.saveWithFullMetadata(level.registryAccess());
            var restored = (LootBoxBlockEntity) BlockEntity.loadStatic(pos, block.defaultBlockState(), pending, level.registryAccess());
            restored.setLevel(level);
            helper.assertTrue(restored.getLootTable() != null, "Unopened loot table must survive saving");
            helper.assertTrue(restored.getLootTableSeed() == 12345L, "Loot seed must survive saving");
            helper.assertFalse(restored.isEmpty(), "Opening a generated box must produce loot");
            helper.assertTrue(restored.getLootTable() == null, "Loot must be consumed only once");
            var inventorySave = restored.saveWithFullMetadata(level.registryAccess());
            var reopened = (LootBoxBlockEntity) BlockEntity.loadStatic(pos, block.defaultBlockState(), inventorySave, level.registryAccess());
            reopened.setLevel(level);
            for (int slot = 0; slot < 27; slot++) {
                helper.assertTrue(ItemStack.matches(restored.getItem(slot), reopened.getItem(slot)), "Slot changed on save/load");
            }
            reopened.clearContent();
            reopened.unpackLootTable(null);
            helper.assertTrue(reopened.isEmpty(), "Empty boxes must never reroll or duplicate loot");
            reopened.setItem(5, new ItemStack(Items.DIAMOND, 3));
            var edited = (LootBoxBlockEntity) BlockEntity.loadStatic(pos, block.defaultBlockState(),
                reopened.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            helper.assertTrue(edited.getItem(5).getCount() == 3, "Player items must survive saving");
        }
        helper.succeed();
    }

    @GameTest public void rightClickOpensInventoryAndBreakingDropsContents(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos local = new BlockPos(2, 2, 2);
        BlockPos pos = helper.absolutePos(local);
        for (Block block : ZMineBlocks.ALL) {
            level.setBlock(pos, block.defaultBlockState(), 3);
            var box = (LootBoxBlockEntity) level.getBlockEntity(pos);
            box.setItem(0, new ItemStack(Items.APPLE, 5));
            helper.useBlock(local, player);
            helper.assertTrue(player.containerMenu instanceof ChestMenu, "Right click must open the vanilla inventory screen");
            helper.assertTrue(((ChestMenu) player.containerMenu).getContainer() == box, "Menu must use the real stored inventory");
            player.closeContainer();
            level.destroyBlock(pos, true);
            helper.assertItemEntityCountIs(Items.APPLE, local, 3, 5);
            helper.assertItemEntityCountIs(block.asItem(), local, 3, 1);
            helper.killAllEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class);
        }
        helper.succeed();
    }

    @GameTest public void lootPoolsAreValidAndVaryAcrossSeeds(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(2, 2, 2));
        level.setBlock(pos, ZMineBlocks.BOX1.defaultBlockState(), 3);
        var box = (LootBoxBlockEntity) level.getBlockEntity(pos);
        for (String name : new String[]{"household", "food", "tools", "medical", "military"}) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ZMineMod.id("chests/" + name));
            Set<String> rolls = new HashSet<>();
            for (int seed = 1; seed <= 32; seed++) {
                box.clearContent();
                box.setLootTable(key, seed);
                helper.assertFalse(box.isEmpty(), "Loot pool cannot be empty: " + name);
                rolls.add(box.saveWithFullMetadata(level.registryAccess()).toString());
            }
            helper.assertTrue(rolls.size() > 1, "Different seeds should give different loot: " + name);
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:lot", maxTicks = 100) public void buildingsPlaceWithWorkingLootInEveryRotation(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(1, 5, 1));
        int total = 0;
        for (var kind : AbandonedBuildings.Kind.values()) for (Rotation rotation : Rotation.values()) {
            var blueprint = AbandonedBuildings.create(kind, RandomSource.create(73));
            // Clear the full test lot between templates, including old containers.
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(14, 22, 14))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            blueprint.place(level, origin, rotation, RandomSource.create(73));
            helper.assertTrue(blueprint.loot().size() >= 3, "Each building must contain several loot containers");
            for (var entry : blueprint.loot().entrySet()) {
                BlockPos pos = origin.offset(BuildingBlueprint.rotate(entry.getKey(), rotation));
                helper.assertTrue(level.getBlockEntity(pos) instanceof LootBoxBlockEntity, "Missing rotated container: " + kind);
                var box = (LootBoxBlockEntity) level.getBlockEntity(pos);
                helper.assertTrue(entry.getValue().equals(box.getLootTable()), "Wrong loot profile");
                helper.assertFalse(box.isEmpty(), "Generated box was empty: " + kind);
                total++;
            }
            for (var entry : blueprint.blocks().entrySet()) {
                BlockPos pos = origin.offset(BuildingBlueprint.rotate(entry.getKey(), rotation));
                if (entry.getValue().getBlock() instanceof net.minecraft.world.level.block.DoorBlock
                    || entry.getValue().getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
                    helper.assertTrue(level.getBlockState(pos).equals(entry.getValue().rotate(rotation)), "Door or bed lost a half: " + kind + " at " + pos);
                    helper.assertTrue(level.getBlockState(pos).canSurvive(level, pos), "Door or bed has no support: " + kind + " at " + pos);
                }
                if (entry.getValue().getBlock() instanceof LootBoxBlock) {
                    Direction expected = rotation.rotate(entry.getValue().getValue(LootBoxBlock.FACING));
                    helper.assertTrue(level.getBlockState(pos).getValue(LootBoxBlock.FACING) == expected, "Box faces wrong direction");
                }
            }
        }
        ZMineMod.LOGGER.info("Verified {} generated loot containers across all buildings and rotations", total);
        helper.succeed();
    }
}
