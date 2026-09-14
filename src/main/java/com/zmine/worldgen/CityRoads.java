package com.zmine.worldgen;

import com.zmine.block.ZMineBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;

/** One curved footprint drives pavement, markings, damage and bridge safety edges. */
@SuppressWarnings("null")
public final class CityRoads {
    private CityRoads() { }
    public static boolean lane(DistrictLayout.Lot lot, int x, int z) { return RoadGeometry.lane(0, 0, 0, lot, x, z); }

    public static void paint(WorldGenLevel level, BlockPos origin, DistrictLayout.Lot lot, RandomSource random) {
        int[][] heights = new int[16][16];
        for (int[] row : heights) java.util.Arrays.fill(row, origin.getY());
        paint(level, origin, lot, heights, random, false);
    }

    public static void paint(WorldGenLevel level, BlockPos origin, DistrictLayout.Lot lot, int[][] heights, RandomSource random, boolean bridge) {
        boolean[][] surface = new boolean[16][16], bridges = new boolean[16][16];
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            surface[x][z] = bridge ? RoadGeometry.deck(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, lot, x, z)
                : RoadGeometry.pavement(level.getSeed(), origin.getX() >> 4, origin.getZ() >> 4, lot, x, z);
            bridges[x][z] = bridge;
        }
        paint(level, origin, lot, heights, random, new CityTerrain.RoadBed(surface, bridges, new boolean[16][16]));
    }

    public static void paint(WorldGenLevel level, BlockPos origin, DistrictLayout.Lot lot, int[][] heights, RandomSource random, CityTerrain.RoadBed bed) {
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        long seed = level.getSeed();
        boolean ns = RoadGeometry.northSouth(lot), intersection = !lot.straight();
        Rotation rotation = ns ? Rotation.NONE : Rotation.CLOCKWISE_90;
        boolean restricted = false;
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            restricted |= bed.bridge()[x][z] || bed.tunnel()[x][z];
            low = Math.min(low, heights[x][z]); high = Math.max(high, heights[x][z]);
            if (!bed.surface()[x][z]) continue;
            boolean road = RoadGeometry.lane(seed, cx, cz, lot, x, z);
            long wear = DistrictLayout.hash(seed ^ (origin.getX() + x) * 73428767L ^ (origin.getZ() + z) * 912931L);
            Block material = road ? Blocks.CONCRETE.gray() : Blocks.STONE_BRICKS;
            // A continuous pale curb makes the pedestrian edge readable on every bend.
            if (!road && RoadGeometry.laneDistance(seed, cx, cz, lot, x, z) < 1) material = Blocks.SMOOTH_STONE;
            if (Math.floorMod(wear, lot.condition() == DistrictLayout.Condition.WORN ? 47 : 23) == 0)
                material = road ? Blocks.DYED_TERRACOTTA.gray() : Blocks.CRACKED_STONE_BRICKS;
            int across = ns ? x : z, along = ns ? z : x;
            double center = RoadGeometry.center(seed, cx, cz, lot, along);
            boolean avenue = RoadGeometry.width(lot) >= 12;
            int worldAlong = ns ? origin.getZ() + z : origin.getX() + x;
            boolean stripe = avenue ? across == (int) Math.round(center - 1.5) || across == (int) Math.round(center + 1.5)
                : across == (int) Math.round(center - .5) && Math.floorMod(worldAlong, 8) < 5;
            if (road && !intersection && stripe) material = Blocks.CONCRETE.yellow();
            if (road && lot.bend()) {
                double offset = RoadGeometry.turnOffset(lot, x, z);
                if (Math.abs(offset) < .65) material = Blocks.CONCRETE.yellow();
            }
            if (road && !intersection && avenue && Math.abs(Math.abs(across - center) - 4.5) < .5
                && Math.floorMod(worldAlong, 8) < 4) material = Blocks.CONCRETE.white();
            if (road && intersection && !lot.bend() && ((z <= 1 || z >= 14) && x % 2 == 0 || (x <= 1 || x >= 14) && z % 2 == 0))
                material = Blocks.CONCRETE.white();
            level.setBlock(at(origin, heights, x, 0, z), material.defaultBlockState(), 2);
            if (bed.bridge()[x][z] && RoadGeometry.edge(seed, cx, cz, lot, x, z)) {
                var p = at(origin, heights, x, 1, z);
                int damage = Math.floorMod(wear, 23);
                // Half-height coping, broken sections and exposed rebar instead of tall uniform walls.
                if (damage < 2) continue;
                if (damage < 6) {
                    var bars = Blocks.IRON_BARS.defaultBlockState().setValue(IronBarsBlock.NORTH, ns).setValue(IronBarsBlock.SOUTH, ns)
                        .setValue(IronBarsBlock.EAST, !ns).setValue(IronBarsBlock.WEST, !ns);
                    level.setBlock(p, bars, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                } else level.setBlock(p, (damage < 10 ? Blocks.MOSSY_STONE_BRICK_SLAB : damage < 16 ? Blocks.STONE_BRICK_SLAB : Blocks.ANDESITE_SLAB).defaultBlockState(), 2);
            }
            if (bed.tunnel()[x][z] && !road && RoadGeometry.laneDistance(seed, cx, cz, lot, x, z) >= RoadGeometry.sidewalkWidth(lot) - 1
                && (bed.portal()[x][z] || Math.floorMod(worldAlong, 16) == 8))
                level.setBlock(at(origin, heights, x, RoadGeometry.ceiling(seed, cx, cz, lot, x, z), z), Blocks.SEA_LANTERN.defaultBlockState(), 2);
        }
        // Rigid props use one base height, never independent column heights that shear the model.
        if (!restricted && !lot.bend()) {
            int alongChunk = ns ? cz : cx;
            long slot = DistrictLayout.hash(seed ^ cx * 73428767L ^ cz * 912931L);
            if (intersection || Math.floorMod(alongChunk, 3) == 1) {
                var pole = new BuildingBlueprint(random, 16);
                double middle = RoadGeometry.center(seed, cx, cz, lot, 5);
                int left = Math.max(0, (int) Math.ceil(middle - RoadGeometry.width(lot) / 2.0 - RoadGeometry.sidewalkWidth(lot)));
                int right = Math.min(15, (int) Math.ceil(middle + RoadGeometry.width(lot) / 2.0 + RoadGeometry.sidewalkWidth(lot)) - 1);
                if (!intersection && (lot.condition() == DistrictLayout.Condition.BOMBED || lot.condition() == DistrictLayout.Condition.OVERGROWN)) {
                    for (int z = 3; z <= 11; z++) right = Math.min(right, (int) Math.ceil(RoadGeometry.center(seed, cx, cz, lot, z)
                        + RoadGeometry.width(lot) / 2.0 + RoadGeometry.sidewalkWidth(lot)) - 1);
                    fallenStreetlight(pole, right);
                }
                else if (intersection) {
                    corner: for (int x = 0; x < 4; x++) for (int z = 0; z < 4; z++) {
                        if (bed.surface()[x][z] && !RoadGeometry.lane(seed, cx, cz, lot, x, z)) {
                            streetlight(pole, x, z, 1); break corner;
                        }
                    }
                }
                else streetlight(pole, (slot & 1) == 0 ? left : right, 5, (slot & 1) == 0 ? 1 : -1);
                placeProp(level, origin, heights, bed, pole, rotation, random);
            }
            if (!intersection && high == low) {
                var props = new BuildingBlueprint(random, 16);
                if (lot.condition() == DistrictLayout.Condition.MILITARY && RoadGeometry.width(lot) >= 12 && Math.floorMod(slot, 3) == 0) {
                    StreetBuildings.tank(props, 8, 1, 5, true);
                    props.box(14, 1, 2, ZMineBlocks.MEDKIT_MILITARY, Direction.NORTH, "military");
                } else if (lot.condition() == DistrictLayout.Condition.WORN && random.nextInt(5) == 0) {
                    StreetBuildings.car(props, 9, 1, 8, Blocks.CONCRETE.lightGray());
                }
                int supplyX = Math.max(0, (int) Math.ceil(Math.max(RoadGeometry.center(seed, cx, cz, lot, 5),
                    RoadGeometry.center(seed, cx, cz, lot, 6)) - RoadGeometry.width(lot) / 2.0 - RoadGeometry.sidewalkWidth(lot)));
                props.put(supplyX, 1, 5, Blocks.CAULDRON);
                props.box(supplyX, 1, 6, ZMineBlocks.BOX3_1, Direction.EAST, "household");
                placeProp(level, origin, heights, bed, props, rotation, random);
            }
        }
        if (!intersection && lot.condition() == DistrictLayout.Condition.BOMBED)
            crater(level, origin, lot, heights, bed, rotation, seed);
        if (!restricted && lot.condition() == DistrictLayout.Condition.OVERGROWN) for (int i = 0; i < 12; i++) {
            int x = random.nextInt(16), z = random.nextInt(16);
            if (!bed.surface()[x][z] || RoadGeometry.lane(seed, cx, cz, lot, x, z)
                || RoadGeometry.laneDistance(seed, cx, cz, lot, x, z) < 1) continue;
            var p = at(origin, heights, x, 0, z);
            if (level.getBlockState(p.above()).isAir()) {
                level.setBlock(p, Blocks.COARSE_DIRT.defaultBlockState(), 2);
                level.setBlock(p.above(), Blocks.DEAD_BUSH.defaultBlockState(), 2);
            }
        }
    }

    private static void crater(WorldGenLevel level, BlockPos origin, DistrictLayout.Lot lot, int[][] heights, CityTerrain.RoadBed bed, Rotation rotation, long seed) {
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        for (int x = 7; x <= 13; x++) for (int z = 5; z <= 13; z++) {
            var q = rotate(new BlockPos(x, 0, z), rotation);
            int qx = q.getX(), qz = q.getZ();
            if (!bed.surface()[qx][qz] || bed.tunnel()[qx][qz] || !RoadGeometry.lane(seed, cx, cz, lot, qx, qz)) continue;
            long noise = DistrictLayout.hash(seed ^ (origin.getX() + qx) * 73428767L ^ (origin.getZ() + qz) * 912931L);
            double r = Math.hypot((x - 10.2) / 2.9, (z - 9.1) / 3.7) + Math.floorMod(noise, 7) * .025;
            if (r > 1.12) continue;
            var p = at(origin, heights, qx, 0, qz);
            if (!level.getBlockState(p.above()).isAir()) continue;
            // On a bridge the scar stops above the structural deck; no rectangular holes to the river.
            if (r >= .92 || bed.bridge()[qx][qz]) {
                level.setBlock(p, (Math.floorMod(noise, 3) == 0 ? Blocks.BLACKSTONE : Blocks.CRACKED_DEEPSLATE_TILES).defaultBlockState(), 2);
                continue;
            }
            int depth = r < .36 ? 3 : r < .68 ? 2 : 1;
            for (int y = 0; y < depth; y++) level.setBlock(p.below(y), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(p.below(depth), (Math.floorMod(noise, 3) == 0 ? Blocks.TUFF : Blocks.BLACKSTONE).defaultBlockState(), 2);
        }
    }

    private static void placeProp(WorldGenLevel level, BlockPos origin, int[][] heights, CityTerrain.RoadBed bed, BuildingBlueprint prop, Rotation rotation, RandomSource random) {
        var blocks = prop.blocks();
        if (blocks.isEmpty()) return;
        var anchor = blocks.keySet().stream().filter(p -> p.getY() == 1).findFirst().orElse(blocks.keySet().iterator().next());
        var rotated = rotate(anchor, rotation);
        int y = heights[rotated.getX()][rotated.getZ()];
        var base = new BlockPos(origin.getX(), y, origin.getZ());
        for (var entry : blocks.entrySet()) {
            var q = rotate(entry.getKey(), rotation);
            if (entry.getKey().getY() == 1 && (heights[q.getX()][q.getZ()] != y || !bed.surface()[q.getX()][q.getZ()])) return;
            var target = base.offset(q);
            if (!level.getBlockState(target).isAir() || entry.getKey().getY() == 1 && !level.getBlockState(target.below()).isSolidRender()) return;
        }
        prop.placeOverlay(level, base, rotation, random);
    }

    private static BlockPos at(BlockPos origin, int[][] heights, int x, int y, int z) {
        return new BlockPos(origin.getX() + x, heights[x][z] + y, origin.getZ() + z);
    }
    public static BlockPos rotate(BlockPos p, Rotation rotation) { return BuildingBlueprint.rotate(p, rotation, 16); }

    private static void streetlight(BuildingBlueprint b, int x, int z, int arm) {
        b.put(x, 1, z, Blocks.ANDESITE_WALL);
        b.fill(x, 2, z, x, 6, z, Blocks.IRON_BARS);
        b.bars(Math.min(x, x + arm * 2), 6, z, Math.max(x, x + arm * 2), z);
        b.put(x + arm * 2, 5, z, Blocks.SEA_LANTERN);
        b.slab(x + arm * 2, 6, z, Blocks.SMOOTH_STONE_SLAB, false);
    }

    private static void fallenStreetlight(BuildingBlueprint b, int edge) {
        b.put(edge, 1, 4, Blocks.ANDESITE_WALL);
        for (int z = 5; z <= 10; z++) b.face(edge, 1, z, Blocks.LIGHTNING_ROD.waxed().weathered(), Direction.SOUTH);
        b.face(edge - 1, 1, 10, Blocks.LIGHTNING_ROD.waxed().weathered(), Direction.WEST);
        b.face(edge - 2, 1, 10, Blocks.LIGHTNING_ROD.waxed().weathered(), Direction.WEST);
        b.put(edge - 3, 1, 10, Blocks.SMOOTH_QUARTZ_SLAB);
        b.put(edge - 3, 1, 11, Blocks.STAINED_GLASS_PANE.lightGray());
        b.slab(edge - 1, 1, 3, Blocks.ANDESITE_SLAB, false);
    }
}
