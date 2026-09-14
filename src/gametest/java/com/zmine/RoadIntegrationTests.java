package com.zmine;

import com.zmine.worldgen.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

@SuppressWarnings("null")
public class RoadIntegrationTests {
    @GameTest public void terrainSamplingReusesSparsePointsAcrossAllColumns(GameTestHelper helper) {
        var level = helper.getLevel();
        long before = OriginalTerrain.evaluatedPoints(level);
        for (int x = -1600; x < -1584; x++) for (int z = -1600; z < -1584; z++) OriginalTerrain.interpolated(level, x, z);
        long cold = OriginalTerrain.evaluatedPoints(level) - before;
        helper.assertTrue(cold <= 25, "A neighboring road recomputed noise for every column");
        before = OriginalTerrain.evaluatedPoints(level);
        for (int x = -1585; x >= -1600; x--) for (int z = -1585; z >= -1600; z--) OriginalTerrain.interpolated(level, x, z);
        helper.assertTrue(OriginalTerrain.evaluatedPoints(level) == before, "Warm terrain planning repeats expensive noise samples");
        ZMineMod.LOGGER.info("Terrain sampling regression: {} cold points for 256 columns; zero warm evaluations", cold);
        helper.succeed();
    }

    @GameTest public void naturalBuildingsOnlyUseTheDistrictGenerator(GameTestHelper helper) {
        var biome = helper.getLevel().getBiome(helper.absolutePos(BlockPos.ZERO)).value();
        boolean district = false;
        for (var step : biome.getGenerationSettings().features()) for (var holder : step) {
            var key = holder.unwrapKey();
            if (key.isEmpty()) continue;
            var id = key.get().identifier();
            helper.assertFalse(id.equals(ZMineMod.id("abandoned_building")), "Standalone buildings are still scattered away from roads");
            district |= id.equals(ZMineMod.id("apocalypse_district"));
        }
        helper.assertTrue(district, "Natural city generation was removed with the standalone buildings");
        helper.succeed();
    }

    private static DistrictLayout.Lot road(boolean ns, int width, DistrictLayout.Condition condition) {
        return new DistrictLayout.Lot(ns ? DistrictLayout.Zone.ROAD_NS : DistrictLayout.Zone.ROAD_EW,
            null, Rotation.NONE, width == 12, width == 12, width, width, condition, null);
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void curvedSidewalksKeepTheirWidthWithoutPavingTheWholeLot(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(16, 8, 16));
        var origin = new BlockPos(point.getX() & ~15, point.getY(), point.getZ() & ~15);
        int[][] heights = new int[16][16];
        for (var row : heights) java.util.Arrays.fill(row, origin.getY());
        for (boolean ns : new boolean[]{true, false}) for (int width : new int[]{6, 8, 12}) {
            for (var p : BlockPos.betweenClosed(origin.below(3), origin.offset(15, 12, 15)))
                level.setBlock(p, (p.getY() <= origin.getY() ? Blocks.DIRT : Blocks.AIR).defaultBlockState(), 2);
            var lot = road(ns, width, DistrictLayout.Condition.OVERGROWN);
            var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
            helper.assertTrue(bed != null, "Dry curved road rejected");
            CityRoads.paint(level, origin, lot, heights, RandomSource.create(3), bed);
            for (int along = 0; along < 16; along++) {
                int sidewalks = 0, land = 0;
                for (int across = 0; across < 16; across++) {
                    int x = ns ? across : along, z = ns ? along : across;
                    var p = origin.offset(x, 0, z);
                    if (!RoadGeometry.pavement(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, lot, x, z)) {
                        helper.assertTrue(level.getBlockState(p).is(Blocks.DIRT), "Curved verge became a rectangular stone platform");
                        land++;
                    } else if (!RoadGeometry.lane(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, lot, x, z)) {
                        helper.assertTrue(level.getBlockState(p).isSolidRender(), "Sidewalk has a hole on a bend");
                        sidewalks++;
                    } else helper.assertFalse(level.getBlockState(p.above()).is(Blocks.DEAD_BUSH), "Verge vegetation spills over the traffic markings");
                }
                helper.assertTrue(sidewalks == 2 * RoadGeometry.sidewalkWidth(lot) && land > 0, "A bend clips its pedestrian pavement");
            }
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void sedimentaryRoadCutsKeepNaturalSlopesAndProtectedBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(16, 8, 16));
        var origin = new BlockPos(point.getX() & ~15, point.getY(), point.getZ() & ~15);
        int[][] heights = new int[16][16];
        for (var row : heights) java.util.Arrays.fill(row, origin.getY());
        for (var material : new net.minecraft.world.level.block.Block[]{Blocks.SANDSTONE, Blocks.DYED_TERRACOTTA.orange()}) {
            for (var p : BlockPos.betweenClosed(origin.below(3), origin.offset(15, 14, 15)))
                level.setBlock(p, (p.getY() <= origin.getY() + 5 ? material : Blocks.AIR).defaultBlockState(), 2);
            var lot = road(true, 6, DistrictLayout.Condition.WORN);
            var chest = origin.offset(0, 2, 8);
            level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
            helper.assertTrue(CityTerrain.prepareRoad(level, origin, lot, heights) == null, "Slope grading removed an inventory");
            helper.assertTrue(level.getBlockState(origin.offset(7, 5, 8)).is(material), "Rejected grading left a cut mountain");
            level.setBlock(chest, material.defaultBlockState(), 2);
            var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
            helper.assertTrue(bed != null, "Natural sandstone or terracotta was rejected");
            CityRoads.paint(level, origin, lot, heights, RandomSource.create(3), bed);
            int raisedVerges = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (bed.surface()[x][z]) continue;
                var ground = RuinsTerrain.ground(level, origin.getX() + x, origin.getZ() + z);
                helper.assertTrue(level.getBlockState(ground).is(material), "Road slope lost its local geology");
                if (ground.getY() > origin.getY()) raisedVerges++;
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    int nx = x + d.getStepX(), nz = z + d.getStepZ();
                    if (nx < 0 || nz < 0 || nx > 15 || nz > 15) continue;
                    int next = bed.surface()[nx][nz] ? origin.getY() : RuinsTerrain.ground(level, origin.getX() + nx, origin.getZ() + nz).getY();
                    helper.assertTrue(Math.abs(ground.getY() - next) <= 2, "Sheer roadside cut remains");
                }
            }
            helper.assertTrue(raisedVerges > 0, "The road flattened every column in the chunk");
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void tunnelVaultsStayAboveSidewalksAtCornersAndJunctions(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(16, 8, 16));
        var origin = new BlockPos(point.getX() & ~15, point.getY(), point.getZ() & ~15);
        int[][] heights = new int[16][16];
        for (var row : heights) java.util.Arrays.fill(row, origin.getY());
        for (int mask : new int[]{3, 6, 9, 12, 7, 11, 13, 14, 15, 5, 10}) {
            for (var p : BlockPos.betweenClosed(origin.below(3), origin.offset(15, 24, 15)))
                level.setBlock(p, (p.getY() <= origin.getY() + 20 ? Blocks.SANDSTONE : Blocks.AIR).defaultBlockState(), 2);
            var lot = new DistrictLayout.Lot(DistrictLayout.Zone.CROSSROADS, null, Rotation.NONE, false, false, 8, 8, DistrictLayout.Condition.WORN, null, mask);
            var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
            helper.assertTrue(bed != null, "Sandstone tunnel rejected at a junction");
            CityRoads.paint(level, origin, lot, heights, RandomSource.create(3), bed);
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                helper.assertTrue(level.getBlockState(origin.offset(x, 20, z)).is(Blocks.SANDSTONE), "Junction tunnel sliced off the mountain");
                if (!RoadGeometry.pavement(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, lot, x, z)) continue;
                helper.assertTrue(bed.tunnel()[x][z], "Incomplete tunnel across a connected arm");
                for (int y = 1; y <= 5; y++) helper.assertTrue(level.getBlockState(origin.offset(x, y, z)).isAir(), "Tunnel wall or light blocks the pedestrian pavement");
            }
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void asymmetricMountainsCannotLeaveHalfAnArchOrBlockTheEntrance(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(16, 8, 16));
        var origin = new BlockPos(point.getX() & ~15, point.getY(), point.getZ() & ~15);
        for (boolean ns : new boolean[]{true, false}) {
            for (int x = 0; x < 32; x++) for (int z = 0; z < 32; z++) {
                int along = ns ? z : x, across = ns ? x : z;
                int top = along < 9 ? across < 8 ? 16 : 3 : 20;
                for (int y = -3; y <= 25; y++) level.setBlock(origin.offset(x, y, z), (y <= top ? Blocks.SANDSTONE : Blocks.AIR).defaultBlockState(), 2);
            }
            var lot = road(ns, 8, DistrictLayout.Condition.WORN);
            for (int part = 1; part >= 0; part--) {
                var base = origin.offset(ns ? 0 : part * 16, 0, ns ? part * 16 : 0);
                int[][] heights = new int[16][16];
                for (var row : heights) java.util.Arrays.fill(row, base.getY());
                var bed = CityTerrain.prepareRoad(level, base, lot, heights);
                helper.assertTrue(bed != null, "Asymmetric mountain road rejected");
                CityRoads.paint(level, base, lot, heights, RandomSource.create(3), bed);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    int along = part * 16 + (ns ? z : x);
                    if (!RoadGeometry.pavement(level.getSeed(), base.getX() >> 4, base.getZ() >> 4, lot, x, z)) continue;
                    helper.assertTrue(bed.tunnel()[x][z] == (along >= 9), "Only part of a tunnel cross-section has a roof");
                    for (int y = 1; y <= 5; y++) helper.assertTrue(level.getBlockState(base.offset(x, y, z)).isAir(), "Tunnel entrance or sidewalk blocked");
                    if (along >= 9) helper.assertTrue(level.getBlockState(base.offset(x, 20, z)).is(Blocks.SANDSTONE), "Tunnel removed the mountain above it");
                    else for (int y = 6; y <= 16; y++) helper.assertTrue(level.getBlockState(base.offset(x, y, z)).isAir(), "An unsupported roof strip remains outside the mountain");
                }
            }
        }
        helper.succeed();
    }

    @GameTest public void curvesKeepDecksAndMarkingsConnectedAcrossNegativeChunks(GameTestHelper helper) {
        int bends = 0;
        for (long seed : new long[]{0, 91, -8431, Long.MAX_VALUE}) for (boolean ns : new boolean[]{true, false})
            for (int width : new int[]{6, 8, 12}) for (int along = -192; along <= 192; along++) {
                var lot = road(ns, width, DistrictLayout.Condition.WORN);
                int chunk = Math.floorDiv(along, 16), local = Math.floorMod(along, 16);
                int cx = ns ? -12 : chunk, cz = ns ? chunk : -12;
                double center = RoadGeometry.center(seed, cx, cz, lot, local);
                int previousChunk = Math.floorDiv(along - 1, 16), previousLocal = Math.floorMod(along - 1, 16);
                double previous = RoadGeometry.center(seed, ns ? -12 : previousChunk, ns ? previousChunk : -12, lot, previousLocal);
                helper.assertTrue(Math.abs(center - previous) < .3, "Curve jumps at a chunk seam");
                if (Math.abs(center - 7.5) > .75) bends++;
                int lanes = 0, rails = 0;
                for (int across = 0; across < 16; across++) {
                    int x = ns ? across : local, z = ns ? local : across;
                    boolean lane = RoadGeometry.lane(seed, cx, cz, lot, x, z);
                    boolean edge = RoadGeometry.edge(seed, cx, cz, lot, x, z);
                    if (lane) {
                        lanes++;
                        helper.assertTrue(RoadGeometry.deck(seed, cx, cz, lot, x, z) && !edge, "Curved traffic lane leaves the bridge or hits a parapet");
                    }
                    if (edge) rails++;
                }
                helper.assertTrue(lanes == width && rails == 2, "A curve pinches its carriageway or loses a bridge edge");
            }
        helper.assertTrue(bends > 1000, "Roads remained straight");
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 200)
    public void curvedBridgesKeepWaterPiersAndAnOpenLaneOnBothAxes(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(16, 12, 16));
        var origin = new BlockPos(point.getX() & ~15, point.getY(), point.getZ() & ~15);
        for (boolean ns : new boolean[]{true, false}) {
            for (var p : BlockPos.betweenClosed(origin.offset(0, -8, 0), origin.offset(31, 16, 31)))
                level.setBlock(p, (p.getY() == origin.getY() - 8 ? Blocks.STONE : p.getY() <= origin.getY() - 4 ? Blocks.WATER : Blocks.AIR).defaultBlockState(), 2);
            var lot = road(ns, 12, DistrictLayout.Condition.BOMBED);
            for (int part = 1; part >= 0; part--) {
                var base = origin.offset(ns ? 0 : part * 16, 0, ns ? part * 16 : 0);
                int[][] heights = new int[16][16];
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) heights[x][z] = base.getY() + (part * 16 + (ns ? z : x)) / 8;
                var bed = CityTerrain.prepareRoad(level, base, lot, heights);
                helper.assertTrue(bed != null, "River bridge was rejected");
                CityRoads.paint(level, base, lot, heights, RandomSource.create(11), bed);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                    int cx = base.getX() >> 4, cz = base.getZ() >> 4;
                    if (!RoadGeometry.deck(level.getSeed(), cx, cz, lot, x, z)) continue;
                    var surface = new BlockPos(base.getX() + x, heights[x][z], base.getZ() + z);
                    helper.assertTrue(level.getBlockState(surface).isSolidRender() && level.getBlockState(surface.below()).isSolidRender(), "Bridge crater broke the deck");
                    if (RoadGeometry.lane(level.getSeed(), cx, cz, lot, x, z))
                        helper.assertTrue(level.getBlockState(surface.above()).isAir() && level.getBlockState(surface.above(2)).isAir(), "Parapet or prop crosses a curved lane");
                    if (RoadGeometry.edge(level.getSeed(), cx, cz, lot, x, z))
                        helper.assertTrue(level.getBlockState(surface.above(2)).isAir(), "Bridge parapet is too tall");
                }
            }
            int wet = 0, piers = 0;
            for (int along = 0; along < 32; along++) for (int across = 0; across < 16; across++) {
                var p = origin.offset(ns ? across : along, -6, ns ? along : across);
                if (!level.getFluidState(p).isEmpty()) wet++;
                if (level.getBlockState(p).isSolidRender()) piers++;
            }
            helper.assertTrue(wet > 450 && piers >= 4, "Bridge filled the river or floats without piers");
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 100)
    public void mountainTunnelsPreserveTheSummitAndProtectInventories(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(16, 8, 16));
        var origin = new BlockPos(point.getX() & ~15, point.getY(), point.getZ() & ~15);
        for (var p : BlockPos.betweenClosed(origin.below(3), origin.offset(15, 22, 15)))
            level.setBlock(p, (p.getY() <= origin.getY() + 18 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
        int[][] heights = new int[16][16];
        for (var row : heights) java.util.Arrays.fill(row, origin.getY());
        var lot = road(true, 8, DistrictLayout.Condition.WORN);
        var chest = origin.offset(7, 2, 7);
        level.setBlock(chest, Blocks.CHEST.defaultBlockState(), 2);
        helper.assertTrue(CityTerrain.prepareRoad(level, origin, lot, heights) == null, "Tunnel destroyed inventory");
        helper.assertTrue(level.getBlockState(origin.offset(7, 3, 7)).is(Blocks.STONE), "Preflight left a partial tunnel");
        level.setBlock(chest, Blocks.STONE.defaultBlockState(), 2);
        var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
        helper.assertTrue(bed != null, "Mountain tunnel rejected");
        CityRoads.paint(level, origin, lot, heights, RandomSource.create(5), bed);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            helper.assertTrue(level.getBlockState(origin.offset(x, 18, z)).is(Blocks.STONE), "Road sliced off the mountain");
            if (RoadGeometry.lane(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, lot, x, z))
                for (int y = 1; y <= 5; y++) helper.assertTrue(level.getBlockState(origin.offset(x, y, z)).isAir(), "Tunnel has insufficient headroom");
        }
        helper.succeed();
    }

    @GameTest(structure = "zmine-tests:city", skyAccess = true, maxTicks = 100)
    public void lightingIsSparseAndFallenPolesRemainVisible(GameTestHelper helper) {
        var level = helper.getLevel();
        var point = helper.absolutePos(new BlockPos(0, 6, 0));
        var origin = new BlockPos((point.getX() + 15) & ~15, point.getY(), (point.getZ() + 15) & ~15);
        for (var condition : new DistrictLayout.Condition[]{DistrictLayout.Condition.WORN, DistrictLayout.Condition.BOMBED}) {
            for (var p : BlockPos.betweenClosed(origin.below(3), origin.offset(15, 9, 47)))
                level.setBlock(p, (p.getY() < origin.getY() ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 2);
            for (int chunk = 0; chunk < 3; chunk++) CityRoads.paint(level, origin.offset(0, 0, chunk * 16), road(true, 8, condition), RandomSource.create(11));
            int shafts = 0, lamps = 0;
            for (var p : BlockPos.betweenClosed(origin.above(), origin.offset(15, 7, 47))) {
                if (level.getBlockState(p).is(Blocks.LIGHTNING_ROD.waxed().weathered())) shafts++;
                if (level.getBlockState(p).is(Blocks.SEA_LANTERN) && p.getY() == origin.getY() + 5) lamps++;
            }
            if (condition == DistrictLayout.Condition.WORN) helper.assertTrue(lamps == 1, "Expected one streetlight per 48 blocks");
            else helper.assertTrue(shafts == 8 && lamps == 0, "Fallen shaft or broken light disappeared");
        }
        helper.succeed();
    }

    @GameTest public void cornersOnlyOpenOntoTheirTwoConnectedStreets(GameTestHelper helper) {
        for (int mask : new int[]{3, 6, 9, 12}) {
            var corner = new DistrictLayout.Lot(DistrictLayout.Zone.CROSSROADS, null, Rotation.NONE, false, false, 8, 8, DistrictLayout.Condition.WORN, null, mask);
            int pixels = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                boolean lane = RoadGeometry.lane(91, 6, 6, corner, x, z);
                if (lane) {
                    pixels++;
                    helper.assertTrue(!RoadGeometry.edge(91, 6, 6, corner, x, z), "Corner parapet blocks its turning lane");
                }
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    boolean boundary = d == net.minecraft.core.Direction.NORTH && z == 0 || d == net.minecraft.core.Direction.SOUTH && z == 15
                        || d == net.minecraft.core.Direction.WEST && x == 0 || d == net.minecraft.core.Direction.EAST && x == 15;
                    if (boundary && !corner.connects(d)) helper.assertFalse(lane, "Unconnected arm remains cut off at a corner");
                }
            }
            helper.assertTrue(pixels >= 90 && pixels < 180, "A corner should be a quarter-circle, not a cross");
        }
        helper.succeed();
    }

    @GameTest public void coastlinesHaveNoOceanGridAndAtMostTwoUsefulBridges(GameTestHelper helper) {
        for (long seed : new long[]{91, -8431, Long.MAX_VALUE}) {
            int rx = 0;
            while (!DistrictLayout.urban(seed, rx * 24 + 6, 6)) rx++;
            int offset = rx * 24;
            for (int terrain = 0; terrain < 4; terrain++) {
                final int shape = terrain;
                java.util.function.BiPredicate<Integer, Integer> dry = (x, z) -> shape == 0 || shape == 1 ? shape == 0 || x < offset + 11
                    : shape == 2 ? x < offset + 11 || x > offset + 13 : (x < offset + 11 || x > offset + 13) && (z < 11 || z > 13);
                var plan = CityNetwork.plan(seed, rx, 0, dry);
                helper.assertTrue(plan.equals(CityNetwork.plan(seed, rx, 0, dry)), "Network depends on generation order");
                helper.assertTrue(plan.bridges().size() <= 2, "City built too many river bridges");
                if (shape == 1) helper.assertTrue(plan.bridges().isEmpty(), "Bridge has no opposite ocean shore");
                if (shape == 2) helper.assertTrue(plan.bridges().size() == 1, "Two useful shores should have one crossing");
                for (var bridge : plan.bridges()) helper.assertTrue(dry.test(bridge.x0(), bridge.z0()) && dry.test(bridge.x1(), bridge.z1()), "Bridge does not reach land on both ends");
                for (int x = offset + 6; x <= offset + 18; x++) for (int z = 6; z <= 18; z++) {
                    int mask = plan.connections(x, z);
                    if (mask == 0) continue;
                    helper.assertTrue(Integer.bitCount(mask) >= 2, "Dangling road runs into an empty lot");
                    if (shape == 1) helper.assertTrue(x < offset + 11, "Urban grid extends over the ocean");
                    for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) if ((mask & DistrictLayout.bit(direction)) != 0)
                        helper.assertTrue((plan.connections(x + direction.getStepX(), z + direction.getStepZ()) & DistrictLayout.bit(direction.getOpposite())) != 0, "Road exits into a missing neighbor");
                }
            }
        }
        helper.succeed();
    }
}
