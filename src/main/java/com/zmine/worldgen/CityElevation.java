package com.zmine.worldgen;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/** Smoothed original terrain, sampled without loading adjacent chunks or reading previous roads. */
public final class CityElevation {
    private static final Map<ServerLevel, ConcurrentHashMap<Long, Integer>> CACHE = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private record Site(int x, int z, int y) { }
    private static final Map<ServerLevel, ConcurrentHashMap<Site, Boolean>> SITES = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private CityElevation() { }
    private static ConcurrentHashMap<Long, Integer> cache(ServerLevel level) {
        return CACHE.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>());
    }
    private static ConcurrentHashMap<Site, Boolean> sites(ServerLevel level) {
        return SITES.computeIfAbsent(level, ignored -> new ConcurrentHashMap<>());
    }
    public static int node(WorldGenLevel level, int cx, int cz) {
        var server = level.getLevel();
        var cache = cache(server);
        long key = ((long) cx << 32) ^ (cz & 0xffffffffL);
        Integer known = cache.get(key);
        if (known != null) return known;
        int total = 0, weight = 0;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            int w = (dx == 0 ? 2 : 1) * (dz == 0 ? 2 : 1);
            total += w * OriginalTerrain.sample(level, cx * 16 + 8 + dx * 48, cz * 16 + 8 + dz * 48).surface();
            weight += w;
        }
        int height = Math.clamp(Math.max(level.getSeaLevel() + 5, Math.round((float) total / weight)),
            level.getMinY() + 3, Math.min(level.getMaxY() - 56, level.getSeaLevel() + 64));
        // Bounded per-world caches disappear when a world closes. Eviction does not change heights.
        if (cache.size() > 8192) cache.clear();
        cache.put(key, height);
        return height;
    }
    public static int streetY(WorldGenLevel level, int worldX, int worldZ) {
        int cx = Math.floorDiv(worldX, 16), cz = Math.floorDiv(worldZ, 16);
        int hubX = Math.floorDiv(cx, 24) * 24 + 12, hubZ = Math.floorDiv(cz, 24) * 24 + 12;
        if (DistrictLayout.urban(level.getSeed(), cx, cz)) return node(level, hubX, hubZ);
        boolean ns = Math.floorMod(cx, 24) == 12;
        if (!ns && Math.floorMod(cz, 24) != 12) return node(level, hubX, hubZ);
        int along = ns ? worldZ : worldX;
        int hub = Math.floorDiv(along - 200, 384) * 384 + 200;
        int startX = ns ? hubX : Math.floorDiv(hub, 16), startZ = ns ? Math.floorDiv(hub, 16) : hubZ;
        return interpolate(node(level, startX, startZ), node(level, startX + (ns ? 0 : 24), startZ + (ns ? 24 : 0)), along - hub);
    }
    private static int interpolate(int start, int end, int coordinate) {
        // Level city platforms with a smooth grade through the countryside between them.
        double t = Math.clamp((coordinate - 112) / 160.0, 0.0, 1.0);
        return (int) Math.round(start + (end - start) * t * t * (3 - 2 * t));
    }
    public static int lotY(WorldGenLevel level, int cx, int cz, net.minecraft.world.level.block.Rotation rotation) {
        var front = rotation.rotate(net.minecraft.core.Direction.NORTH);
        return streetY(level, cx * 16 + 7 + front.getStepX() * 9, cz * 16 + 7 + front.getStepZ() * 9);
    }

    /** Same decision for all four lots: never leave half a complex embedded in a hillside or lake. */
    public static boolean buildableBlock(WorldGenLevel level, int ax, int az, int y) {
        var server = level.getLevel();
        var cache = sites(server);
        var key = new Site(ax, az, y);
        var known = cache.get(key);
        if (known != null) return known;
        for (int x = 0; x <= 32; x += 8) for (int z = 0; z <= 32; z += 8) {
            var sample = OriginalTerrain.sample(level, ax * 16 + x, az * 16 + z);
            int surface = sample.surface(), ground = sample.ground();
            if (surface > ground || Math.abs(ground - y) > 6) { cache.put(key, false); return false; }
        }
        if (cache.size() > 8192) cache.clear();
        cache.put(key, true);
        return true;
    }
    public static int[][] roadProfile(WorldGenLevel level, int cx, int cz) {
        int[][] heights = new int[16][16];
        if (DistrictLayout.urban(level.getSeed(), cx, cz)) {
            int y = streetY(level, cx * 16, cz * 16);
            for (var row : heights) java.util.Arrays.fill(row, y);
        } else {
            boolean ns = Math.floorMod(cx, 24) == 12;
            for (int along = 0; along < 16; along++) {
                int y = streetY(level, cx * 16 + (ns ? 0 : along), cz * 16 + (ns ? along : 0));
                for (int across = 0; across < 16; across++) heights[ns ? across : along][ns ? along : across] = y;
            }
        }
        return heights;
    }
}
