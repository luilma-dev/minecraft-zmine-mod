package com.zmine.worldgen;

import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Cross-sections are planned from untouched terrain, shared by the road and its neighboring slopes. */
@SuppressWarnings("null")
public final class RoadLandform {
    private static final Map<ServerLevel, Map<Long, RoadLandform>> WORLDS = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static Map<Long, RoadLandform> cache(WorldGenLevel level) {
        return WORLDS.computeIfAbsent(level.getLevel(), ignored -> com.google.common.cache.CacheBuilder.newBuilder()
            .maximumSize(2048).<Long, RoadLandform>build().asMap());
    }

    final DistrictLayout.Lot lot;
    final int cx, cz;
    final int[][] heights;
    private final int[][] sections = new int[16][16];
    private final boolean[] covered = new boolean[49], bridges = new boolean[49], portals = new boolean[49];

    public static RoadLandform planned(WorldGenLevel level, int cx, int cz, DistrictLayout.Lot lot) {
        var cache = cache(level);
        long key = (long) cx << 32 ^ (cz & 0xffffffffL);
        return cache.computeIfAbsent(key, ignored -> new RoadLandform(level, cx, cz, lot, CityElevation.roadProfile(level, cx, cz), false));
    }

    static RoadLandform forPlacement(WorldGenLevel level, int cx, int cz, DistrictLayout.Lot lot, int[][] heights) {
        // The current chunk already has an exact heightmap: no noise recomputation is needed.
        return new RoadLandform(level, cx, cz, lot, heights, true);
    }

    private RoadLandform(WorldGenLevel level, int cx, int cz, DistrictLayout.Lot lot, int[][] heights, boolean live) {
        this.cx = cx; this.cz = cz; this.lot = lot; this.heights = heights;
        Arrays.fill(covered, true);
        boolean[] seen = new boolean[covered.length];
        long seed = level.getSeed();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) sections[x][z] = geometricSection(x, z);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!RoadGeometry.tunnelShell(seed, cx, cz, lot, x, z)) continue;
            int section = section(x, z), wx = cx * 16 + x, wz = cz * 16 + z;
            int ground = ground(level, wx, wz, live), y = heights[x][z];
            seen[section] = true;
            // The whole arch, including its two supports, needs two blocks of actual cover.
            covered[section] &= ground >= y + RoadGeometry.ceiling(seed, cx, cz, lot, x, z) + 2;
            if (RoadGeometry.pavement(seed, cx, cz, lot, x, z)) {
                boolean wet = live ? !level.getFluidState(new BlockPos(wx, ground + 1, wz)).isEmpty()
                    : surface(level, wx, wz) > ground;
                bridges[section] |= wet || ground < y - 8;
            }
        }
        for (int i = 0; i < covered.length; i++) covered[i] &= seen[i] && !bridges[i];
        // Terrain behind an unconnected junction arm belongs to its nearest real cross-section.
        // Otherwise an unused sector would be treated as an open cut in an entirely buried tunnel.
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) if (!seen[section(x, z)]) {
            int nearest = Integer.MAX_VALUE, selected = 0;
            for (int sx = 0; sx < 16; sx++) for (int sz = 0; sz < 16; sz++) {
                if (!RoadGeometry.tunnelShell(seed, cx, cz, lot, sx, sz)) continue;
                int distance = (sx - x) * (sx - x) + (sz - z) * (sz - z);
                if (distance < nearest) { nearest = distance; selected = geometricSection(sx, sz); }
            }
            sections[x][z] = selected;
        }
        var mouths = new java.util.EnumMap<Direction, Boolean>(Direction.class);
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!RoadGeometry.pavement(seed, cx, cz, lot, x, z) || !tunnel(x, z)) continue;
            for (var d : Direction.Plane.HORIZONTAL) {
                int nx = x + d.getStepX(), nz = z + d.getStepZ();
                if (nx >= 0 && nx < 16 && nz >= 0 && nz < 16) {
                    if (RoadGeometry.pavement(seed, cx, cz, lot, nx, nz) && !tunnel(nx, nz)) portals[section(x, z)] = true;
                } else if (lot.connects(d)) {
                    // Outside columns are read, never modified. Natural generation uses original noise.
                    // Sample the complete neighboring mouth instead of a single center column.
                    if (!mouths.computeIfAbsent(d, direction -> coveredMouth(level, seed, direction, live && level instanceof ServerLevel))) portals[section(x, z)] = true;
                }
            }
        }
    }

    private boolean coveredMouth(WorldGenLevel level, long seed, Direction direction, boolean live) {
        boolean ns = direction.getAxis() == Direction.Axis.Z;
        int along = direction == Direction.NORTH || direction == Direction.WEST ? -1 : 16;
        for (int across = 0; across < 16; across++) {
            int x = ns ? across : along, z = ns ? along : across;
            int lx = Math.clamp(x, 0, 15), lz = Math.clamp(z, 0, 15);
            if (!RoadGeometry.tunnelShell(seed, cx, cz, lot, lx, lz)) continue;
            int y = live ? heights[lx][lz] : CityElevation.streetY(level, cx * 16 + x, cz * 16 + z);
            if (ground(level, cx * 16 + x, cz * 16 + z, live) < y + RoadGeometry.ceiling(seed, cx, cz, lot, lx, lz) + 2) return false;
        }
        return true;
    }

    private static int ground(WorldGenLevel level, int x, int z, boolean live) {
        if (live) return RuinsTerrain.ground(level, x, z).getY();
        return OriginalTerrain.interpolated(level, x, z).ground();
    }

    private static int surface(WorldGenLevel level, int x, int z) {
        return OriginalTerrain.interpolated(level, x, z).surface();
    }

    private int section(int x, int z) { return sections[x][z]; }

    private int geometricSection(int x, int z) {
        if (lot.straight()) return RoadGeometry.northSouth(lot) ? z : x;
        if (lot.bend()) {
            double cornerX = (lot.connections() & 2) != 0 ? 15.5 : -.5;
            double cornerZ = (lot.connections() & 4) != 0 ? 15.5 : -.5;
            return Math.clamp((int) (Math.atan2(Math.abs(z - cornerZ), Math.abs(x - cornerX)) * 32 / Math.PI), 0, 15);
        }
        // A central chamber and complete transverse sections on each connected junction arm.
        double dx = x - 7.5, dz = z - 7.5;
        if (Math.max(Math.abs(dx), Math.abs(dz)) < 4) return 0;
        if (Math.abs(dx) > Math.abs(dz)) return 1 + (dx > 0 ? 0 : 12) + (int) Math.abs(dx) - 4;
        return 1 + (dz > 0 ? 24 : 36) + (int) Math.abs(dz) - 4;
    }

    public boolean tunnel(int x, int z) { return covered[section(x, z)]; }
    public boolean bridge(int x, int z) { return bridges[section(x, z)]; }
    public boolean portal(int x, int z) { return portals[section(x, z)]; }

    /** Finite footprint: disconnected road ends cannot grade distant terrain along an imaginary arm. */
    double distance(long seed, int worldX, int worldZ) {
        return distance(seed, cx, cz, lot, worldX, worldZ);
    }

    static double distance(long seed, int cx, int cz, DistrictLayout.Lot lot, int worldX, int worldZ) {
        int x = worldX - cx * 16, z = worldZ - cz * 16;
        int lx = Math.clamp(x, 0, 15), lz = Math.clamp(z, 0, 15);
        return Math.hypot(x - lx, z - lz) + Math.max(0,
            RoadGeometry.pavementDistance(seed, cx, cz, lot, lx, lz));
    }

    int roadY(int worldX, int worldZ) {
        return heights[Math.clamp(worldX - cx * 16, 0, 15)][Math.clamp(worldZ - cz * 16, 0, 15)];
    }

    int gradeAt(long seed, int terrainY, int worldX, int worldZ) {
        int x = Math.clamp(worldX - cx * 16, 0, 15), z = Math.clamp(worldZ - cz * 16, 0, 15);
        // A nearby open road must not shave the mountain covering a tunnel, or fill a river bank.
        if (tunnel(x, z) || bridge(x, z)) return terrainY;
        return grade(terrainY, roadY(worldX, worldZ), distance(seed, worldX, worldZ));
    }

    static int grade(int terrainY, int roadY, double distance) {
        if (!Double.isFinite(distance) || distance >= 24) return terrainY;
        // Rounded earthworks: gentle near the pavement, steeper only farther up the hillside.
        int rise = (int) Math.floor(distance * .65 + distance * distance * .025);
        int graded = Math.clamp(terrainY, roadY - rise, roadY + rise);
        // Fade any remaining edit out before the search boundary, never a vertical chunk-edge cliff.
        double fade = Math.clamp((distance - 16) / 8, 0.0, 1.0);
        return (int) Math.round(graded + (terrainY - graded) * fade * fade * (3 - 2 * fade));
    }
}
