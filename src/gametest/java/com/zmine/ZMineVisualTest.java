package com.zmine;

import com.zmine.worldgen.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Real-client visual coverage of the expanded scenery and actual noise-world generation. */
@SuppressWarnings("null")
public class ZMineVisualTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            var connection = world.getConnection();
            var server = world.getServer();
            server.runCommand("gamemode spectator @a");
            server.runCommand("gamerule minecraft:spectators_generate_chunks true");
            server.runCommand("time set noon");
            context.runOnClient(client -> {
                if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
                client.options.fov().set(70);
                client.options.renderDistance().set(7);
            });
            for (var kind : LargeCityBuildings.Kind.values()) {
                int x = kind.ordinal() * 64;
                var b = LargeCityBuildings.create(kind, 77);
                server.runOnServer(mcServer -> b.place(connection.getServerLevel(), new BlockPos(x, -61, 0), Rotation.NONE, RandomSource.create(77)));
                server.runCommand("tp @a " + (x + 48) + " " + (kind == LargeCityBuildings.Kind.MILITARY_BASE ? -29 : -6) + " -35 40 30");
                context.waitTicks(30);
                connection.waitForClientboundPackets();
                connection.waitForChunksRender(false);
                context.takeScreenshot(TestScreenshotOptions.of("complex_" + kind.name().toLowerCase()).withSize(1600, 1000));
            }
            for (var condition : DistrictLayout.Condition.values()) {
                int x = condition.ordinal() * 32;
                var origin = new BlockPos(x, -61, 112);
                var lot = new DistrictLayout.Lot(DistrictLayout.Zone.ROAD_NS, null, Rotation.NONE, true, false, 12, 8, condition, null);
                server.runOnServer(mcServer -> {
                    previewRoad(connection.getServerLevel(), origin, lot);
                });
                server.runCommand("tp @a " + (x + 23) + " -47 96 40 30");
                context.waitTicks(15);
                connection.waitForChunksRender(false);
                context.takeScreenshot(TestScreenshotOptions.of("road_" + condition.name().toLowerCase()).withSize(1400, 1000));
            }
            for (var kind : new AbandonedBuildings.Kind[]{AbandonedBuildings.Kind.BRICK_HOUSE, AbandonedBuildings.Kind.TOWNHOUSE, AbandonedBuildings.Kind.FARMHOUSE}) {
                int x = (kind.ordinal() - 10) * 32;
                var b = AbandonedBuildings.create(kind, RandomSource.create(9));
                server.runOnServer(mcServer -> b.place(connection.getServerLevel(), new BlockPos(x, -61, 160), Rotation.NONE, RandomSource.create(9)));
                server.runCommand("tp @a " + (x + 25) + " -46 140 40 20");
                context.waitTicks(15);
                connection.waitForChunksRender(false);
                context.takeScreenshot(TestScreenshotOptions.of(kind.id).withSize(1400, 1000));
            }
            server.runOnServer(mcServer -> {
                var level = connection.getServerLevel();
                var origin = new BlockPos(256, -46, 112);
                for (var p : BlockPos.betweenClosed(origin.offset(-6, -8, -4), origin.offset(22, 14, 50)))
                    level.setBlock(p, (p.getY() <= -54 ? net.minecraft.world.level.block.Blocks.STONE : p.getY() <= -50 ? net.minecraft.world.level.block.Blocks.WATER : net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(), 2);
                var lot = new DistrictLayout.Lot(DistrictLayout.Zone.ROAD_NS, null, Rotation.NONE, false, false, 8, 8, DistrictLayout.Condition.BOMBED, null);
                for (int chunk = 2; chunk >= 0; chunk--) {
                    var base = origin.offset(0, 0, chunk * 16);
                    int[][] heights = new int[16][16];
                    for (var row : heights) java.util.Arrays.fill(row, base.getY());
                    var bed = CityTerrain.prepareRoad(level, base, lot, heights);
                    if (bed == null) throw new AssertionError("Bridge preview rejected");
                    CityRoads.paint(level, base, lot, heights, RandomSource.create(11), bed);
                }
            });
            server.runCommand("tp @a 290 -20 97 40 35");
            context.waitTicks(20); connection.waitForClientboundPackets(); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("curved_bridge_water").withSize(1600, 1000));
            server.runOnServer(mcServer -> {
                var level = connection.getServerLevel();
                // A real ninety-degree connection: south approach turns east, with no unused arms.
                for (int part = 0; part < 3; part++) {
                    var base = new BlockPos(320 + (part == 2 ? 16 : 0), -61, 112 + (part == 0 ? 16 : 0));
                    var lot = new DistrictLayout.Lot(part == 1 ? DistrictLayout.Zone.CROSSROADS : part == 0 ? DistrictLayout.Zone.ROAD_NS : DistrictLayout.Zone.ROAD_EW,
                        null, Rotation.NONE, false, false, 8, 8, DistrictLayout.Condition.WORN, null, part == 1 ? 6 : part == 0 ? 5 : 10);
                    previewRoad(level, base, lot);
                }
                var base = new BlockPos(384, -53, 112);
                for (int x = 0; x < 16; x++) for (int z = 0; z < 32; z++) {
                    int top = z < 7 ? -53 : -35 + (int) (2 * Math.sin(x * .3));
                    for (int y = -56; y <= -30; y++) level.setBlock(new BlockPos(base.getX() + x, y, base.getZ() + z),
                        (y <= top ? net.minecraft.world.level.block.Blocks.STONE : net.minecraft.world.level.block.Blocks.AIR).defaultBlockState(), 2);
                }
                var lot = new DistrictLayout.Lot(DistrictLayout.Zone.ROAD_NS, null, Rotation.NONE, false, false, 8, 8, DistrictLayout.Condition.WORN, null);
                for (int part = 1; part >= 0; part--) {
                    int[][] heights = new int[16][16];
                    for (var row : heights) java.util.Arrays.fill(row, base.getY());
                    var origin = base.offset(0, 0, part * 16);
                    var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
                    if (bed == null) throw new AssertionError("Tunnel preview rejected");
                    CityRoads.paint(level, origin, lot, heights, RandomSource.create(9), bed);
                }
            });
            server.runCommand("tp @a 305 -33 97 -42 42");
            context.waitTicks(20); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("connected_right_angle_corner").withSize(1600, 1000));
            server.runCommand("tp @a 391 -49 108 0 0");
            context.waitTicks(20); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("mountain_tunnel_portal").withSize(1600, 1000));
        }
        try (var world = context.worldBuilder().adjustSettings(state -> {
            var normal = state.getSettings().worldgenLoadContext().lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL);
            state.setWorldType(new WorldCreationUiState.WorldTypeEntry(normal));
            state.setSeed("91"); state.setGenerateStructures(true);
            state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE); state.setAllowCommands(true);
        }).create()) {
            var connection = world.getConnection();
            var server = world.getServer();
            int[] focus = {12, 12, 80};
            server.runOnServer(mcServer -> {
                var level = connection.getServerLevel();
                outer: for (int radius = 0; radius <= 2; radius++) for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                    int cx = 12 + dx * 24, cz = 12 + dz * 24;
                    if (!DistrictLayout.urban(level.getSeed(), cx, cz)) continue;
                    focus[0] = cx; focus[1] = cz; focus[2] = CityElevation.node(level, cx, cz);
                    int useful = 0;
                    for (int x = cx - 5; x <= cx + 5; x++) for (int z = cz - 5; z <= cz + 5; z++) {
                        var lot = DistrictLayout.at(level, x, z);
                        if (!lot.isRoad() && lot.zone() != DistrictLayout.Zone.WILDERNESS) useful++;
                    }
                    if (useful >= 16 && CityNetwork.plan(level, Math.floorDiv(cx, 24), Math.floorDiv(cz, 24)).roads().size() >= 30) break outer;
                }
            });
            int wx = focus[0] * 16, wz = focus[1] * 16;
            server.runCommand("gamemode spectator @a");
            server.runCommand("gamerule minecraft:spectators_generate_chunks true");
            server.runCommand("time set noon"); server.runCommand("weather clear");
            server.runCommand("tp @a " + (wx + 64) + " " + (focus[2] + 100) + " " + (wz - 96) + " 30 37");
            context.runOnClient(client -> {
                if (!client.gui.hud.isHidden()) client.gui.hud.toggle();
                client.options.fov().set(75); client.options.renderDistance().set(12);
            });
            context.waitTicks(40);
            server.runOnServer(mcServer -> {
                var level = connection.getServerLevel();
                int roads = 0, lots = 0, fields = 0, waterways = 0;
                var heights = new java.util.HashSet<Integer>();
                for (int cx = focus[0] - 6; cx <= focus[0] + 6; cx++) for (int cz = focus[1] - 6; cz <= focus[1] + 6; cz++) {
                    level.getChunk(cx, cz);
                    var lot = DistrictLayout.at(level, cx, cz);
                    var biome = level.getBiome(new BlockPos(cx * 16 + 8, level.getSeaLevel(), cz * 16 + 8));
                    if (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_OCEAN)) { waterways++; continue; }
                    if (lot.zone() == DistrictLayout.Zone.WILDERNESS) { fields++; continue; }
                    if (!lot.isRoad()) {
                        int ax = Math.floorDiv(cx, 3) * 3 + 1, az = Math.floorDiv(cz, 3) * 3 + 1;
                        if (!CityElevation.buildableBlock(level, ax, az, CityElevation.streetY(level, ax * 16, az * 16 - 1))) { fields++; continue; }
                    }
                    int y;
                    if (lot.isRoad()) { y = CityElevation.streetY(level, cx * 16 + 5, cz * 16 + 5); roads++; }
                    else if (lot.zone() == DistrictLayout.Zone.COMPLEX) {
                        int ax = Math.floorDiv(cx, 3) * 3 + 1, az = Math.floorDiv(cz, 3) * 3 + 1;
                        y = CityElevation.streetY(level, ax * 16 + 15, az * 16 - 1); lots++;
                    } else { y = CityElevation.lotY(level, cx, cz, lot.facing()); lots++; }
                    heights.add(y);
                    if (!lot.isRoad()) {
                        var front = lot.facing().rotate(net.minecraft.core.Direction.NORTH);
                        int ax = lot.zone() == DistrictLayout.Zone.COMPLEX ? Math.floorDiv(cx, 3) * 3 + 1 : cx;
                        int az = lot.zone() == DistrictLayout.Zone.COMPLEX ? Math.floorDiv(cz, 3) * 3 + 1 : cz;
                        int size = lot.zone() == DistrictLayout.Zone.COMPLEX ? 32 : 15;
                        var entry = BuildingBlueprint.rotate(new BlockPos(size / 2, 0, 0), lot.facing(), size);
                        var entryWorld = new BlockPos(ax * 16 + entry.getX(), y, az * 16 + entry.getZ());
                        for (int step = 0; step <= 3; step++) {
                            var access = entryWorld.relative(front, step);
                            if (!level.getBlockState(access).isSolidRender() || !level.getBlockState(access.above()).isAir())
                                throw new AssertionError("Building entrance has a trench or obstruction at " + access);
                        }
                    }
                    // Other chunks' ore veins may replace stone below a road; check solid support,
                    // not an exact stone material. Lane 5 remains clear of deliberate craters.
                    int px = 5, pz = 5;
                    if (lot.isRoad() && lot.straight()) {
                        int across = (int) Math.round(RoadGeometry.center(level.getSeed(), cx, cz, lot, 5) - RoadGeometry.width(lot) / 2.0 + 1);
                        if (RoadGeometry.northSouth(lot)) px = across; else pz = across;
                        y = CityElevation.streetY(level, cx * 16 + px, cz * 16 + pz);
                    }
                    if (lot.bend()) {
                        outer: for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                            if (Math.abs(RoadGeometry.turnOffset(lot, x, z)) < .5) { px = x; pz = z; break outer; }
                        }
                    }
                    var p = new BlockPos(cx * 16 + px, y - 1, cz * 16 + pz);
                    if (!level.getBlockState(p).isSolidRender()) throw new AssertionError("Missing natural city support at " + p + " for " + lot.zone());
                    if (lot.isRoad() && !level.getBlockState(p.above()).isSolidRender()) throw new AssertionError("Interrupted road at " + p.above());
                }
                // Countryside outside the city must still exist, rather than being paved wholesale.
                int country = 0;
                for (int cx = focus[0] + 8; cx <= focus[0] + 10; cx++) for (int cz = focus[1] + 8; cz <= focus[1] + 10; cz++) {
                    if (DistrictLayout.at(level, cx, cz).zone() == DistrictLayout.Zone.WILDERNESS) country++;
                }
                if (country < 6 || roads < 20 || lots < 10) throw new AssertionError("Missing countryside or incomplete city");
                ZMineMod.LOGGER.info("Verified natural ruined city: {} roads, {} building slices, {} waterways, {} field chunks, elevations {}", roads, lots, waterways, fields + country, heights);
            });
            context.waitTicks(25); connection.waitForClientboundPackets(); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("ruined_city_natural_aerial").withSize(1920, 1200));
            int[] streetY = {focus[2]};
            server.runOnServer(mcServer -> streetY[0] = CityElevation.streetY(connection.getServerLevel(), wx + 4, wz + 20));
            server.runCommand("tp @a " + (wx + 4) + " " + (streetY[0] + 2) + " " + (wz + 20) + " 0 0");
            context.waitTicks(20); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("ruined_city_natural_street").withSize(1600, 1000));
            server.runOnServer(mcServer -> streetY[0] = CityElevation.streetY(connection.getServerLevel(), wx + 140, wz + 7));
            server.runCommand("tp @a " + (wx + 140) + " " + (streetY[0] + 12) + " " + (wz + 7) + " -90 12");
            context.waitTicks(20); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("countryside_highway").withSize(1600, 1000));
            int[] hillside = {wx, focus[2], wz};
            server.runOnServer(mcServer -> {
                var level = connection.getServerLevel();
                int best = Integer.MIN_VALUE;
                for (int cx = focus[0] - 6; cx <= focus[0] + 6; cx++) for (int cz = focus[1] - 6; cz <= focus[1] + 6; cz++) {
                    var lot = DistrictLayout.at(level, cx, cz);
                    if (!lot.straight()) continue;
                    int x = cx * 16 + 8, z = cz * 16 + 8, y = CityElevation.streetY(level, x, z);
                    int ground = level.getChunkSource().getGenerator().getBaseHeight(x, z,
                        net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, level, level.getChunkSource().randomState()) - 1;
                    if (ground - y > best) { best = ground - y; hillside[0] = x; hillside[1] = y; hillside[2] = z; }
                }
            });
            server.runCommand("tp @a " + (hillside[0] + 28) + " " + (hillside[1] + 26) + " " + (hillside[2] - 28) + " 45 32");
            context.waitTicks(25); connection.waitForClientboundPackets(); connection.waitForChunksRender(false);
            context.takeScreenshot(TestScreenshotOptions.of("natural_road_mountain_transition").withSize(1600, 1000));
        }
    }

    private static void previewRoad(net.minecraft.server.level.ServerLevel level, BlockPos origin, DistrictLayout.Lot lot) {
        int[][] heights = new int[16][16];
        for (var row : heights) java.util.Arrays.fill(row, origin.getY());
        var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
        if (bed == null) throw new AssertionError("Road preview rejected");
        CityRoads.paint(level, origin, lot, heights, RandomSource.create(11), bed);
    }
}
