package com.zmine;

import com.zmine.worldgen.CityRoads;
import com.zmine.worldgen.CityTerrain;
import com.zmine.worldgen.DistrictLayout;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

@SuppressWarnings("null")
public class CityGameTests {
    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void largeComplexesAssembleAcrossChunksWithWorkingLoot(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(16, 6, 16));
        var origin = new BlockPos(center.getX() & ~15, center.getY(), center.getZ() & ~15);
        for (var kind : com.zmine.worldgen.LargeCityBuildings.Kind.values()) {
            for (var p : BlockPos.betweenClosed(origin, origin.offset(31, 42, 31))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            var blueprint = com.zmine.worldgen.LargeCityBuildings.create(kind, 77);
            helper.assertTrue(blueprint.size() == 32 && blueprint.height() <= 48, "Invalid complex dimensions");
            // Reverse order deliberately: each chunk must reconstruct the same larger building.
            for (int dx = 1; dx >= 0; dx--) for (int dz = 1; dz >= 0; dz--)
                blueprint.placeSlice(level, origin, (origin.getX() >> 4) + dx, (origin.getZ() >> 4) + dz, 77);
            for (var entry : blueprint.blocks().entrySet()) {
                // Bars, walls and stairs update connection properties as adjacent slices arrive.
                helper.assertTrue(level.getBlockState(origin.offset(entry.getKey())).is(entry.getValue().getBlock()), "Broken complex seam: " + kind + " at " + entry.getKey());
            }
            helper.assertTrue(blueprint.loot().size() >= 8, "Large complexes need substantial supplies");
            for (var entry : blueprint.loot().entrySet()) {
                var box = (com.zmine.block.LootBoxBlockEntity) level.getBlockEntity(origin.offset(entry.getKey()));
                helper.assertTrue(box != null && !box.isEmpty(), "Missing complex loot: " + kind);
            }
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 100)
    public void manualComplexChecksAllLotsBeforeBuilding(GameTestHelper helper) {
        var level = helper.getLevel();
        var center = helper.absolutePos(new BlockPos(16, 6, 16));
        var origin = new BlockPos(center.getX() & ~15, center.getY(), center.getZ() & ~15);
        for (var p : BlockPos.betweenClosed(origin.below(3), origin.offset(31, 49, 31)))
            level.setBlock(p, (p.getY() <= origin.getY() ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
        var chest = origin.offset(30, 1, 30);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        var feature = new com.zmine.worldgen.feature.LargeBuildingFeature(com.zmine.worldgen.LargeCityBuildings.Kind.MILITARY_BASE);
        helper.assertFalse(feature.place(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
            level, level.getChunkSource().getGenerator(), RandomSource.create(77), origin.above()), "Far-lot inventory was not protected");
        helper.assertTrue(level.getBlockState(origin).is(Blocks.DIRT) && level.getBlockEntity(chest) != null, "Rejected complex left a partial build");
        level.setBlock(chest, Blocks.AIR.defaultBlockState(), 2);
        helper.assertTrue(feature.place(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE,
            level, level.getChunkSource().getGenerator(), RandomSource.create(77), origin.above()), "Clear 32x32 area rejected a military base");
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true)
    public void devastatedStreetsHaveWrecksCracksAndVegetation(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(new BlockPos(16, 6, 16));
        for (var condition : DistrictLayout.Condition.values()) {
            for (var p : BlockPos.betweenClosed(origin.below(2), origin.offset(15, 8, 15)))
                level.setBlock(p, (p.getY() < origin.getY() ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
            var lot = new DistrictLayout.Lot(DistrictLayout.Zone.ROAD_NS, null, Rotation.NONE, true, false, 12, 8, condition, null);
            CityRoads.paint(level, origin, lot, RandomSource.create(11));
            if (condition == DistrictLayout.Condition.BOMBED) {
                helper.assertTrue(level.getBlockState(origin.offset(10, 0, 9)).isAir(), "Missing crater");
                helper.assertTrue(level.getBlockState(origin.offset(10, -3, 9)).isSolidRender(), "Crater falls into void");
            }
            if (Math.floorMod(origin.getZ() >> 4, 3) == 1 && (condition == DistrictLayout.Condition.BOMBED || condition == DistrictLayout.Condition.OVERGROWN))
                helper.assertTrue(level.getBlockState(origin.offset(14, 1, 7)).is(Blocks.LIGHTNING_ROD.waxed().weathered()), "Missing fallen pole shaft");
            for (int z = 0; z < 16; z++) helper.assertTrue(level.getBlockState(origin.offset(4, 1, z)).isAir(), "Wrecks completely block the street");
        }
        helper.succeed();
    }

    @GameTest public void citiesHaveFieldsHighwaysAndCompleteComplexes(GameTestHelper helper) {
        int roads = 0, lots = 0, fields = 0, complexes = 0;
        var widths = new java.util.HashSet<Integer>();
        for (long seed : new long[]{0, 91, -8431, Long.MAX_VALUE}) {
            for (int cx = -64; cx <= 64; cx++) for (int cz = -64; cz <= 64; cz++) {
                var lot = DistrictLayout.at(seed, cx, cz);
                helper.assertTrue(lot.equals(DistrictLayout.at(seed, cx, cz)), "Layout depends on generation order");
                if (lot.zone() == DistrictLayout.Zone.WILDERNESS) { fields++; continue; }
                if (lot.isRoad()) {
                    roads++;
                    widths.add(lot.widthNS()); widths.add(lot.widthEW());
                    if (Math.floorMod(cx, 24) == 12) for (int dz : new int[]{-1, 1})
                        helper.assertTrue(DistrictLayout.at(seed, cx, cz + dz).isRoad(), "Regional highway ends");
                    if (Math.floorMod(cz, 24) == 12) for (int dx : new int[]{-1, 1})
                        helper.assertTrue(DistrictLayout.at(seed, cx + dx, cz).isRoad(), "Regional highway ends");
                } else if (lot.zone() == DistrictLayout.Zone.COMPLEX) {
                    complexes++;
                    int ax = Math.floorDiv(cx, 3) * 3 + 1, az = Math.floorDiv(cz, 3) * 3 + 1;
                    for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++) {
                        var part = DistrictLayout.at(seed, ax + dx, az + dz);
                        helper.assertTrue(part.zone() == DistrictLayout.Zone.COMPLEX && part.complex() == lot.complex(), "Complex is cut in half at a city boundary");
                    }
                    helper.assertTrue(DistrictLayout.at(seed, ax, az - 1).isRoad(), "Complex has no entrance road");
                } else {
                    lots++;
                    Direction entrance = lot.facing().rotate(Direction.NORTH);
                    helper.assertTrue(DistrictLayout.at(seed, cx + entrance.getStepX(), cz + entrance.getStepZ()).isRoad(), "Lot has no street entrance");
                }
            }
        }
        helper.assertTrue(fields > roads + lots && lots > 100 && complexes > 100, "Cities need open countryside and large buildings");
        helper.assertTrue(widths.containsAll(java.util.Set.of(6, 8, 12)), "Missing road width variety");
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void roadsCrossWaterSlopesAndChunkSeamsWithoutSteps(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(16, 6, 16));
        BlockPos origin = new BlockPos(center.getX() & ~15, center.getY(), center.getZ() & ~15);
        // Two adjacent chunks: a flooded valley and a hill higher than the old rejection limit.
        for (int x = -1; x <= 32; x++) for (int z = -1; z <= 16; z++) {
            for (int y = -3; y <= 34; y++) {
                var block = y <= -3 ? Blocks.DIRT : y <= 1 && x < 16 ? Blocks.WATER
                    : x >= 16 && x <= 31 && y <= (x - 16) / 2 ? Blocks.STONE : Blocks.AIR;
                level.setBlock(origin.offset(x, y, z), block.defaultBlockState(), 2);
            }
        }
        // Generate right to left, so a road cannot depend on its already-generated neighbor.
        for (int chunk = 1; chunk >= 0; chunk--) {
            var base = origin.offset(chunk * 16, 0, 0);
            helper.assertTrue(CityTerrain.prepare(level, base), "Water or slopes caused a missing chunk");
            CityRoads.paint(level, base, new DistrictLayout.Lot(DistrictLayout.Zone.ROAD_EW,
                null, Rotation.NONE, false, true), RandomSource.create(9));
        }
        for (int x = 0; x < 32; x++) {
            var lot = new DistrictLayout.Lot(DistrictLayout.Zone.ROAD_EW, null, Rotation.NONE, false, true);
            double centerLine = com.zmine.worldgen.RoadGeometry.center(level.getSeed(), (origin.getX() + x) >> 4, origin.getZ() >> 4, lot, x % 16);
            helper.assertTrue(level.getBlockState(origin.offset(x, 0, (int) Math.round(centerLine - 1.5))).is(Blocks.CONCRETE.yellow()), "Broken curved center line at seam");
            helper.assertTrue(level.getBlockState(origin.offset(x, 1, 5)).isAir()
                && level.getBlockState(origin.offset(x, 2, 5)).isAir(), "Through lane is obstructed");
            for (int z = 0; z < 16; z++) {
                helper.assertTrue(level.getBlockState(origin.offset(x, -1, z)).is(Blocks.STONE), "Missing sealed foundation");
                helper.assertTrue(level.getFluidState(origin.offset(x, 1, z)).isEmpty(), "Water on street");
            }
        }
        helper.assertTrue(level.getBlockState(origin.offset(-1, 0, 7)).is(Blocks.WATER), "Terrain preparation wrote outside chunk");
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 100)
    public void terrainPreflightPreservesInventoriesAndPlayerBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos center = helper.absolutePos(new BlockPos(16, 6, 16));
        BlockPos origin = new BlockPos(center.getX() & ~15, center.getY(), center.getZ() & ~15);
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -2, 0), origin.offset(15, 34, 15))) {
            level.setBlock(p, (p.getY() <= origin.getY() ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
        }
        for (int y : new int[]{-1, 1, 28}) {
            var inventory = origin.offset(15, y, 15);
            level.setBlock(inventory, Blocks.CHEST.defaultBlockState(), 2);
            helper.assertFalse(CityTerrain.prepare(level, origin), "Inventory overwritten by city preparation");
            helper.assertTrue(level.getBlockState(origin).is(Blocks.DIRT), "Preflight must not leave half a road");
            helper.assertTrue(level.getBlockEntity(inventory) != null, "Inventory removed");
            level.setBlock(inventory, (y < 0 ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
        }
        level.setBlock(origin.offset(10, 3, 10), Blocks.BRICKS.defaultBlockState(), 2);
        helper.assertFalse(CityTerrain.prepare(level, origin), "Manual placement destroyed a player wall");
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true)
    public void crossingsStayOnAsphaltAndLeavePedestrianHeadroom(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(16, 6, 16));
        for (boolean avenueNS : new boolean[]{false, true}) for (boolean avenueEW : new boolean[]{false, true}) {
            for (BlockPos p : BlockPos.betweenClosed(origin, origin.offset(15, 8, 15))) level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            var lot = new DistrictLayout.Lot(DistrictLayout.Zone.CROSSROADS, null, Rotation.NONE, avenueNS, avenueEW);
            CityRoads.paint(level, origin, lot, RandomSource.create(5));
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (level.getBlockState(origin.offset(x, 0, z)).is(Blocks.CONCRETE.white())) {
                    helper.assertTrue(CityRoads.lane(lot, x, z), "Crosswalk painted on sidewalk");
                    helper.assertTrue(level.getBlockState(origin.offset(x, 1, z)).isAir()
                        && level.getBlockState(origin.offset(x, 2, z)).isAir(), "Crosswalk blocked by props");
                }
            }
        }
        helper.succeed();
    }
}
