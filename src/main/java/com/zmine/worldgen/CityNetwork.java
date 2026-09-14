package com.zmine.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

/** Plans connections before any blocks are placed: dry streets, at most two useful river crossings. */
public final class CityNetwork {
    public record Bridge(int x0, int z0, int x1, int z1) { }
    public record Plan(Map<Long, Integer> roads, List<Bridge> bridges) {
        public int connections(int cx, int cz) { return roads.getOrDefault(key(cx, cz), 0); }
    }
    private record Link(int a, int b) { }
    private record Candidate(List<Link> links, Bridge bridge, int score) { }
    private static final class Cache {
        final Map<Long, Boolean> dry = new ConcurrentHashMap<>();
        final Map<Long, Plan> plans = new ConcurrentHashMap<>();
        final Map<String, Boolean> connectors = new ConcurrentHashMap<>();
    }
    private static final Map<ServerLevel, Cache> WORLDS = new com.google.common.collect.MapMaker().weakKeys().makeMap();
    private static Cache cache(WorldGenLevel level) { return WORLDS.computeIfAbsent(level.getLevel(), ignored -> new Cache()); }
    private CityNetwork() { }
    private static long key(int x, int z) { return (long) x << 32 ^ (z & 0xffffffffL); }

    public static boolean dryChunk(WorldGenLevel level, int cx, int cz) {
        var values = cache(level).dry;
        var known = values.get(key(cx, cz));
        if (known != null) return known;
        boolean dry = true;
        samples: for (int x : new int[]{0, 8, 16}) for (int z : new int[]{0, 8, 16}) {
            var sample = OriginalTerrain.sample(level, cx * 16 + x, cz * 16 + z);
            if (sample.surface() > sample.ground()) { dry = false; break samples; }
        }
        if (values.size() > 32768) values.clear();
        values.put(key(cx, cz), dry);
        return dry;
    }

    public static Plan plan(WorldGenLevel level, int rx, int rz) {
        var values = cache(level).plans;
        var known = values.get(key(rx, rz));
        if (known != null) return known;
        Plan result = plan(level.getSeed(), rx, rz, (x, z) -> dryChunk(level, x, z));
        if (values.size() > 256) values.clear();
        values.put(key(rx, rz), result);
        return result;
    }

    /** Pure planner also used with synthetic coastlines in regression tests. */
    public static Plan plan(long seed, int rx, int rz, BiPredicate<Integer, Integer> dry) {
        int ox = rx * 24 + 6, oz = rz * 24 + 6;
        if (!DistrictLayout.urban(seed, ox, oz)) return new Plan(Map.of(), List.of());
        var edges = new LinkedHashSet<Link>();
        boolean[] land = new boolean[25];
        for (int i = 0; i < 25; i++) land[i] = dry.test(ox + (i % 5) * 3, oz + (i / 5) * 3);
        for (int i = 0; i < 25; i++) for (int step : new int[]{1, 5}) {
            int j = i + step;
            if (j >= 25 || step == 1 && i % 5 == 4 || !land[i] || !land[j]) continue;
            if (dryLink(dry, ox, oz, i, j)) edges.add(new Link(i, j));
        }
        var candidates = new ArrayList<Candidate>();
        for (boolean ns : new boolean[]{false, true}) for (int line = 0; line < 5; line++) for (int start = 0; start < 4; start++) {
            int a = ns ? start * 5 + line : line * 5 + start;
            if (!land[a]) continue;
            var links = new ArrayList<Link>();
            boolean water = false;
            for (int end = start + 1; end < 5; end++) {
                int b = ns ? end * 5 + line : line * 5 + end;
                int before = b - (ns ? 5 : 1);
                links.add(new Link(before, b));
                water |= !dryLink(dry, ox, oz, before, b);
                if (!land[b]) continue;
                if (water) {
                    int x0 = ox + a % 5 * 3, z0 = oz + a / 5 * 3, x1 = ox + b % 5 * 3, z1 = oz + b / 5 * 3;
                    candidates.add(new Candidate(List.copyOf(links), new Bridge(x0, z0, x1, z1), (end - start) * 10 + Math.abs(line - 2)));
                }
                break;
            }
        }
        candidates.sort(Comparator.comparingInt(c -> c.score()));
        var bridges = new ArrayList<Bridge>();
        for (var candidate : candidates) {
            if (bridges.size() == 2) break;
            int[] component = components(edges);
            int a = candidate.links().getFirst().a(), b = candidate.links().getLast().b();
            // Do not build a bridge when the two shores already have a land route.
            if (component[a] == component[b] || degree(edges, a) == 0 || degree(edges, b) == 0) continue;
            edges.addAll(candidate.links());
            bridges.add(candidate.bridge());
        }
        // A street ending in an empty shore or field is removed back to its last useful junction.
        boolean changed;
        do {
            changed = false;
            for (int node = 0; node < 25; node++) if (degree(edges, node) == 1) {
                final int leaf = node;
                edges.removeIf(e -> e.a() == leaf || e.b() == leaf);
                changed = true;
            }
        } while (changed);
        var roads = new HashMap<Long, Integer>();
        for (var edge : edges) {
            int x = ox + edge.a() % 5 * 3, z = oz + edge.a() / 5 * 3;
            int dx = edge.b() - edge.a() == 1 ? 1 : 0, dz = 1 - dx;
            for (int step = 0; step < 3; step++) join(roads, x + dx * step, z + dz * step, dx == 1 ? Direction.EAST : Direction.SOUTH);
        }
        bridges.removeIf(b -> !roads.containsKey(key(b.x0(), b.z0())) || !roads.containsKey(key(b.x1(), b.z1())));
        return new Plan(Map.copyOf(roads), List.copyOf(bridges));
    }

    private static boolean dryLink(BiPredicate<Integer, Integer> dry, int ox, int oz, int a, int b) {
        int x = ox + a % 5 * 3, z = oz + a / 5 * 3, dx = b - a == 1 ? 1 : 0;
        for (int step = 0; step <= 3; step++) if (!dry.test(x + dx * step, z + (1 - dx) * step)) return false;
        return true;
    }
    private static int degree(Set<Link> edges, int node) { return (int) edges.stream().filter(e -> e.a() == node || e.b() == node).count(); }
    private static int[] components(Set<Link> edges) {
        int[] labels = new int[25];
        for (int i = 0; i < 25; i++) labels[i] = i;
        for (var e : edges) {
            int old = labels[e.b()], replacement = labels[e.a()];
            for (int i = 0; i < 25; i++) if (labels[i] == old) labels[i] = replacement;
        }
        return labels;
    }
    private static void join(Map<Long, Integer> roads, int x, int z, Direction direction) {
        roads.merge(key(x, z), DistrictLayout.bit(direction), (a, b) -> a | b);
        roads.merge(key(x + direction.getStepX(), z + direction.getStepZ()), DistrictLayout.bit(direction.getOpposite()), (a, b) -> a | b);
    }

    private static boolean connector(WorldGenLevel level, int rx, int rz, boolean ns) {
        String key = rx + ":" + rz + ":" + ns;
        var values = cache(level).connectors;
        var known = values.get(key);
        if (known != null) return known;
        int x = rx * 24 + (ns ? 12 : 18), z = rz * 24 + (ns ? 18 : 12);
        boolean available = plan(level, rx, rz).connections(x, z) != 0
            && plan(level, rx + (ns ? 0 : 1), rz + (ns ? 1 : 0)).connections(x + (ns ? 0 : 12), z + (ns ? 12 : 0)) != 0;
        for (int step = 0; available && step <= 12; step++) available = dryChunk(level, x + (ns ? 0 : step), z + (ns ? step : 0));
        if (values.size() > 1024) values.clear();
        values.put(key, available);
        return available;
    }

    public static int connections(WorldGenLevel level, int cx, int cz) {
        if (!DistrictLayout.at(level.getSeed(), cx, cz).isRoad()) return 0;
        int rx = Math.floorDiv(cx, 24), rz = Math.floorDiv(cz, 24);
        int mask = plan(level, rx, rz).connections(cx, cz);
        if (Math.floorMod(cz, 24) == 12) {
            int left = Math.floorDiv(cx - 18, 24), coordinate = cx - (left * 24 + 18);
            if (coordinate <= 12 && connector(level, left, rz, false)) mask |= coordinate == 0 ? 2 : coordinate == 12 ? 8 : 10;
        }
        if (Math.floorMod(cx, 24) == 12) {
            int north = Math.floorDiv(cz - 18, 24), coordinate = cz - (north * 24 + 18);
            if (coordinate <= 12 && connector(level, rx, north, true)) mask |= coordinate == 0 ? 4 : coordinate == 12 ? 1 : 5;
        }
        return mask;
    }

    public static DistrictLayout.Lot lot(WorldGenLevel level, int cx, int cz) {
        var raw = DistrictLayout.at(level.getSeed(), cx, cz);
        if (raw.zone() == DistrictLayout.Zone.WILDERNESS) return raw;
        int mask = connections(level, cx, cz);
        if (mask != 0) return new DistrictLayout.Lot(mask == 5 ? DistrictLayout.Zone.ROAD_NS : mask == 10 ? DistrictLayout.Zone.ROAD_EW : DistrictLayout.Zone.CROSSROADS,
            null, Rotation.NONE, raw.avenueNS(), raw.avenueEW(), raw.widthNS(), raw.widthEW(), raw.condition(), null, mask);
        if (raw.isRoad() || raw.zone() == DistrictLayout.Zone.WILDERNESS) return wilderness();
        int ax = Math.floorDiv(cx, 3) * 3 + 1, az = Math.floorDiv(cz, 3) * 3 + 1;
        int y = CityElevation.streetY(level, ax * 16, az * 16 - 1);
        if (!CityElevation.buildableBlock(level, ax, az, y)) return wilderness();
        for (Rotation rotation : new Rotation[]{raw.facing(), Rotation.NONE, Rotation.CLOCKWISE_90, Rotation.CLOCKWISE_180, Rotation.COUNTERCLOCKWISE_90}) {
            Direction front = rotation.rotate(Direction.NORTH);
            boolean access;
            if (raw.zone() == DistrictLayout.Zone.COMPLEX) {
                int x = ax + (front == Direction.EAST ? 2 : front == Direction.WEST ? -1 : 0);
                int z = az + (front == Direction.SOUTH ? 2 : front == Direction.NORTH ? -1 : 0);
                access = connections(level, x, z) != 0 && connections(level, x + (front.getAxis() == Direction.Axis.Z ? 1 : 0), z + (front.getAxis() == Direction.Axis.X ? 1 : 0)) != 0;
            } else access = connections(level, cx + front.getStepX(), cz + front.getStepZ()) != 0;
            if (access) return new DistrictLayout.Lot(raw.zone(), raw.building(), rotation, false, false, 0, 0, raw.condition(), raw.complex());
        }
        return wilderness();
    }
    private static DistrictLayout.Lot wilderness() { return new DistrictLayout.Lot(DistrictLayout.Zone.WILDERNESS, null, Rotation.NONE, false, false); }
}
