package com.zmine.worldgen;

import com.google.common.cache.CacheBuilder;
import com.google.common.collect.MapMaker;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Sparse, shared original-noise samples. Never loads chunks or reads generated buildings. */
public final class OriginalTerrain {
    public record Sample(int ground, int surface) { }
    private static final Map<ServerLevel, ConcurrentMap<Long, Sample>> WORLDS = new MapMaker().weakKeys().makeMap();
    private static final Map<ServerLevel, java.util.concurrent.atomic.LongAdder> EVALUATIONS = new MapMaker().weakKeys().makeMap();
    private OriginalTerrain() { }
    private static ConcurrentMap<Long, Sample> cache(WorldGenLevel level) {
        return WORLDS.computeIfAbsent(level.getLevel(), ignored -> CacheBuilder.newBuilder().maximumSize(32768)
            .<Long, Sample>build().asMap());
    }
    public static Sample sample(WorldGenLevel level, int x, int z) {
        long key = (long) x << 32 ^ (z & 0xffffffffL);
        return cache(level).computeIfAbsent(key, ignored -> {
            EVALUATIONS.computeIfAbsent(level.getLevel(), unused -> new java.util.concurrent.atomic.LongAdder()).increment();
            var server = level.getLevel();
            var generator = server.getChunkSource().getGenerator();
            var noise = server.getChunkSource().randomState();
            return new Sample(generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, noise) - 1,
                generator.getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, noise) - 1);
        });
    }
    public static long evaluatedPoints(WorldGenLevel level) {
        var counter = EVALUATIONS.get(level.getLevel());
        return counter == null ? 0 : counter.sum();
    }
    public static Sample interpolated(WorldGenLevel level, int x, int z) {
        int gx = Math.floorDiv(x, 4) * 4, gz = Math.floorDiv(z, 4) * 4;
        var a = sample(level, gx, gz);
        if (x == gx && z == gz) return a;
        var b = sample(level, gx + 4, gz);
        var c = sample(level, gx, gz + 4);
        var d = sample(level, gx + 4, gz + 4);
        double tx = (x - gx) / 4.0, tz = (z - gz) / 4.0;
        return new Sample(interpolate(a.ground(), b.ground(), c.ground(), d.ground(), tx, tz),
            interpolate(a.surface(), b.surface(), c.surface(), d.surface(), tx, tz));
    }
    private static int interpolate(int a, int b, int c, int d, double x, double z) {
        return (int) Math.round((a * (1 - x) + b * x) * (1 - z) + (c * (1 - x) + d * x) * z);
    }
}
