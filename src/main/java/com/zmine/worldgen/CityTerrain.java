package com.zmine.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;

/** Local grading only: countryside and river beds retain their original terrain. */
@SuppressWarnings("null")
public final class CityTerrain {
    public record RoadBed(boolean[][] surface, boolean[][] bridge, boolean[][] tunnel, boolean[][] portal) {
        public RoadBed(boolean[][] surface, boolean[][] bridge, boolean[][] tunnel) { this(surface, bridge, tunnel, new boolean[16][16]); }
    }
    private CityTerrain() { }
    private record Margin(int cx, int cz, DistrictLayout.Lot lot) { }

    /** Plan every affected column before writing; tunnel walls stay outside the pedestrian pavement. */
    public static RoadBed prepareRoad(WorldGenLevel level, BlockPos origin, DistrictLayout.Lot lot, int[][] heights) {
        var surface = new boolean[16][16];
        var bridge = new boolean[16][16];
        var tunnel = new boolean[16][16];
        var portal = new boolean[16][16];
        var affected = new boolean[16][16];
        int[][] bottoms = new int[16][16], tops = new int[16][16], floors = new int[16][16];
        var geology = new net.minecraft.world.level.block.state.BlockState[16][16];
        int cx = origin.getX() >> 4, cz = origin.getZ() >> 4;
        long seed = level.getSeed();
        RoadLandform form = RoadLandform.forPlacement(level, cx, cz, lot, heights);
        var frontages = new java.util.ArrayList<net.minecraft.core.Direction>();
        if (!(level instanceof ServerLevel)) for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            var neighbor = DistrictLayout.at(level, cx + direction.getStepX(), cz + direction.getStepZ());
            if (!neighbor.isRoad() && neighbor.zone() != DistrictLayout.Zone.WILDERNESS) frontages.add(direction);
        }
        var pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = origin.getX() + x, wz = origin.getZ() + z, roadY = heights[x][z];
            var ground = RuinsTerrain.ground(level, wx, wz);
            int terrainY = ground.getY();
            geology[x][z] = level.getBlockState(ground);
            boolean wet = !level.getFluidState(ground.above()).isEmpty();
            bridge[x][z] = form.bridge(x, z);
            tunnel[x][z] = form.tunnel(x, z);
            portal[x][z] = form.portal(x, z);
            boolean pavement = RoadGeometry.pavement(seed, cx, cz, lot, x, z);
            boolean shell = RoadGeometry.tunnelShell(seed, cx, cz, lot, x, z);
            if (bridge[x][z] && !RoadGeometry.deck(seed, cx, cz, lot, x, z) || tunnel[x][z] && !shell) continue;
            surface[x][z] = bridge[x][z] ? RoadGeometry.deck(seed, cx, cz, lot, x, z) : pavement;
            // Connect real building fronts on their own side; wilderness keeps its curved verge.
            if (!bridge[x][z] && !tunnel[x][z]) for (var direction : frontages) {
                if (direction == net.minecraft.core.Direction.WEST && x < 8 || direction == net.minecraft.core.Direction.EAST && x >= 8
                    || direction == net.minecraft.core.Direction.NORTH && z < 8 || direction == net.minecraft.core.Direction.SOUTH && z >= 8)
                    surface[x][z] = true;
            }
            int floor = roadY;
            if (!surface[x][z] && !tunnel[x][z]) {
                if (wet || !RuinsTerrain.dryNaturalGround(level, ground)) continue;
                floor = RoadLandform.grade(terrainY, roadY, form.distance(seed, wx, wz));
                if (floor == terrainY) continue;
            }
            floors[x][z] = floor;
            int bottom = Math.min(floor - 3, terrainY);
            if (bridge[x][z]) {
                bottom = floor - 2;
                int along = RoadGeometry.northSouth(lot) ? wz : wx;
                double offset = Math.abs(RoadGeometry.offset(seed, cx, cz, lot, x, z));
                if (Math.floorMod(along, 32) >= 7 && Math.floorMod(along, 32) <= 8
                    && offset >= RoadGeometry.width(lot) / 2.0 - 2 && offset < RoadGeometry.width(lot) / 2.0) bottom = terrainY;
            }
            int top = Math.max(floor + (surface[x][z] ? 7 : 1), terrainY + 1);
            if (tunnel[x][z]) top = floor + RoadGeometry.ceiling(seed, cx, cz, lot, x, z) + 1;
            bottom = Math.max(level.getMinY() + 1, bottom);
            if (top >= level.getMaxY() || floor - 3 < level.getMinY()) return null;
            bottoms[x][z] = bottom; tops[x][z] = top; affected[x][z] = true;
            if (!level.ensureCanWrite(pos.set(wx, bottom, wz)) || !level.ensureCanWrite(pos.set(wx, top, wz))) return null;
            for (int y = bottom; y <= top; y++) {
                var state = level.getBlockState(pos.set(wx, y, wz));
                if (state.hasBlockEntity() || state.is(Blocks.BEDROCK)
                    || level instanceof ServerLevel && !RuinsTerrain.natural(state)) return null;
            }
        }
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!affected[x][z]) continue;
            int wx = origin.getX() + x, wz = origin.getZ() + z, floor = floors[x][z];
            for (int y = tops[x][z]; y > floor; y--) level.setBlock(pos.set(wx, y, wz), Blocks.AIR.defaultBlockState(), 2);
            for (int y = bottoms[x][z]; y < floor; y++) {
                int along = RoadGeometry.northSouth(lot) ? wz : wx;
                boolean girder = RoadGeometry.edge(seed, cx, cz, lot, x, z) || Math.floorMod(along, 32) == 7 || Math.floorMod(along, 32) == 8;
                if (bridge[x][z] && y == floor - 2 && !girder) continue;
                long wear = DistrictLayout.hash(seed ^ wx * 73428767L ^ wz * 912931L ^ Math.floorDiv(y, 3));
                var material = bridge[x][z] ? (y == floor - 1 ? Blocks.SMOOTH_STONE : Math.floorMod(wear, 5) == 0 ? Blocks.MOSSY_STONE_BRICKS
                    : Math.floorMod(wear, 5) == 1 ? Blocks.CRACKED_STONE_BRICKS : Blocks.POLISHED_ANDESITE).defaultBlockState()
                    : !surface[x][z] && !tunnel[x][z] ? RuinsTerrain.slopeFill(geology[x][z]) : Blocks.STONE.defaultBlockState();
                level.setBlock(pos.set(wx, y, wz), material, 2);
            }
            level.setBlock(pos.set(wx, floor, wz), surface[x][z] || tunnel[x][z] ? Blocks.STONE_BRICKS.defaultBlockState()
                : RuinsTerrain.slopeSurface(geology[x][z]), 2);
            if (tunnel[x][z]) {
                int ceiling = tops[x][z] - 1;
                var lining = portal[x][z] ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS;
                // A closed two-block vault; no exposed, disconnected roof teeth above the entrance.
                level.setBlock(pos.set(wx, ceiling, wz), lining.defaultBlockState(), 2);
                level.setBlock(pos.set(wx, ceiling + 1, wz), lining.defaultBlockState(), 2);
                if (!surface[x][z]) for (int y = floor + 1; y < ceiling; y++)
                    level.setBlock(pos.set(wx, y, wz), (portal[x][z] && y <= floor + 2 ? Blocks.POLISHED_DEEPSLATE : lining).defaultBlockState(), 2);
            }
        }
        return new RoadBed(surface, bridge, tunnel, portal);
    }

    /** Neighboring chunks reconstruct the same finite road footprints; no writes cross their border. */
    public static void blendRoadside(WorldGenLevel level, int cx, int cz) {
        var margins = new java.util.ArrayList<Margin>();
        var platforms = new java.util.ArrayList<int[]>();
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (dx == 0 && dz == 0) continue;
            int nx = cx + dx, nz = cz + dz;
            var raw = DistrictLayout.at(level.getSeed(), nx, nz);
            if (!raw.isRoad() && (Math.abs(dx) > 1 || Math.abs(dz) > 1 || raw.zone() == DistrictLayout.Zone.WILDERNESS)) continue;
            var lot = DistrictLayout.at(level, nx, nz);
            if (lot.isRoad()) margins.add(new Margin(nx, nz, lot));
            else if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1 && lot.zone() != DistrictLayout.Zone.WILDERNESS)
                platforms.add(new int[]{nx * 16, nz * 16, CityElevation.lotY(level, nx, nz, lot.facing())});
        }
        if (margins.isEmpty() && platforms.isEmpty()) return;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int wx = cx * 16 + x, wz = cz * 16 + z;
            var ground = RuinsTerrain.ground(level, wx, wz);
            if (!RuinsTerrain.dryNaturalGround(level, ground)) continue;
            double closest = 24;
            int roadY = ground.getY();
            Margin nearest = null;
            for (var margin : margins) {
                double distance = RoadLandform.distance(level.getSeed(), margin.cx(), margin.cz(), margin.lot(), wx, wz);
                if (distance >= closest) continue;
                closest = distance;
                nearest = margin;
            }
            if (nearest != null) roadY = CityElevation.streetY(level, Math.clamp(wx, nearest.cx() * 16, nearest.cx() * 16 + 15),
                Math.clamp(wz, nearest.cz() * 16, nearest.cz() * 16 + 15));
            for (var platform : platforms) {
                double distance = Math.hypot(wx - Math.clamp(wx, platform[0], platform[0] + 15), wz - Math.clamp(wz, platform[1], platform[1] + 15));
                if (distance >= closest) continue;
                closest = distance;
                nearest = null;
                roadY = platform[2];
            }
            int target = RoadLandform.grade(ground.getY(), roadY, closest);
            if (target == ground.getY()) continue;
            // Flat fields never synthesize complete neighboring road profiles.
            if (nearest != null) target = RoadLandform.planned(level, nearest.cx(), nearest.cz(), nearest.lot())
                .gradeAt(level.getSeed(), ground.getY(), wx, wz);
            gradeColumn(level, ground, target);
        }
    }

    private static void gradeColumn(WorldGenLevel level, BlockPos ground, int target) {
        if (target == ground.getY()) return;
        var geology = level.getBlockState(ground);
        int bottom = Math.min(target, ground.getY()) - 1, top = Math.max(target, ground.getY()) + 1;
        var pos = new BlockPos.MutableBlockPos(ground.getX(), bottom, ground.getZ());
        if (!level.ensureCanWrite(pos) || !level.ensureCanWrite(pos.setY(top))) return;
        for (int y = bottom; y <= top; y++) {
            var state = level.getBlockState(pos.setY(y));
            if (state.hasBlockEntity() || !RuinsTerrain.natural(state)) return;
        }
        for (int y = top; y > target; y--) level.setBlock(pos.setY(y), Blocks.AIR.defaultBlockState(), 2);
        for (int y = bottom; y < target; y++) level.setBlock(pos.setY(y), RuinsTerrain.slopeFill(geology), 2);
        level.setBlock(pos.setY(target), RuinsTerrain.slopeSurface(geology), 2);
    }
    public static int surfaceY(WorldGenLevel level) { return Math.max(level.getMinY() + 3, level.getSeaLevel() + 1); }
    public static boolean prepare(WorldGenLevel level, BlockPos origin) {
        int[][] heights = new int[16][16];
        for (int[] row : heights) java.util.Arrays.fill(row, origin.getY());
        return prepare(level, origin, heights);
    }
    public static boolean prepare(WorldGenLevel level, BlockPos origin, int[][] heights) {
        return prepare(level, origin, heights, true);
    }
    public static boolean canPrepare(WorldGenLevel level, BlockPos origin) {
        int[][] heights = new int[16][16];
        for (int[] row : heights) java.util.Arrays.fill(row, origin.getY());
        return prepare(level, origin, heights, false);
    }
    private static boolean prepare(WorldGenLevel level, BlockPos origin, int[][] heights, boolean write) {
        int[][] tops = new int[16][16];
        int[][] bottoms = new int[16][16];
        var surfaceMap = level instanceof ServerLevel ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
        var floorMap = level instanceof ServerLevel ? Heightmap.Types.OCEAN_FLOOR : Heightmap.Types.OCEAN_FLOOR_WG;
        var pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int floor = heights[x][z], wx = origin.getX() + x, wz = origin.getZ() + z;
            if (floor - 2 < level.getMinY() || floor + 48 >= level.getMaxY()) return false;
            int top = Math.min(level.getMaxY() - 1, Math.max(floor + 48, level.getHeight(surfaceMap, wx, wz)));
            // Buildings have full foundations, not thin floating platforms on four corner stilts.
            int bottom = Math.min(floor - 2, Math.max(level.getMinY() + 1, level.getHeight(floorMap, wx, wz) - 1));
            tops[x][z] = top;
            bottoms[x][z] = bottom;
            if (!level.ensureCanWrite(pos.set(wx, bottom, wz)) || !level.ensureCanWrite(pos.set(wx, top, wz))) return false;
            for (int y = bottom; y <= top; y++) {
                var state = level.getBlockState(pos.set(wx, y, wz));
                if (state.hasBlockEntity() || state.is(Blocks.BEDROCK)) return false;
                if (level instanceof ServerLevel && !RuinsTerrain.natural(state)) return false;
            }
        }
        if (!write) return true;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int floor = heights[x][z], wx = origin.getX() + x, wz = origin.getZ() + z;
            for (int y = tops[x][z]; y > floor; y--) {
                pos.set(wx, y, wz);
                if (!level.getBlockState(pos).isAir()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
            for (int y = bottoms[x][z]; y < floor; y++) level.setBlock(pos.set(wx, y, wz), Blocks.STONE.defaultBlockState(), 2);
            level.setBlock(pos.set(wx, floor, wz), Blocks.STONE_BRICKS.defaultBlockState(), 2);
        }
        return true;
    }
}
