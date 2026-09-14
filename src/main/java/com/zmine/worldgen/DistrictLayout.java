package com.zmine.worldgen;

import net.minecraft.world.level.block.Rotation;

/** Seeded cities separated by countryside, with continuous regional highways. */
public final class DistrictLayout {
    public static final int STREET_SPACING = 3;
    public static final int AVENUE_SPACING = 12;
    public static final int REGION_SIZE = 24;
    public enum Zone { WILDERNESS, ROAD_NS, ROAD_EW, CROSSROADS, BUILDING, PARK, PARKING, COMPLEX }
    public enum Condition { WORN, OVERGROWN, BOMBED, MILITARY }
    public record Lot(Zone zone, AbandonedBuildings.Kind building, Rotation facing,
                      boolean avenueNS, boolean avenueEW, int widthNS, int widthEW,
                      Condition condition, LargeCityBuildings.Kind complex, int connections) {
        public Lot(Zone zone, AbandonedBuildings.Kind building, Rotation facing, boolean avenueNS, boolean avenueEW,
                   int widthNS, int widthEW, Condition condition, LargeCityBuildings.Kind complex) {
            this(zone, building, facing, avenueNS, avenueEW, widthNS, widthEW, condition, complex,
                zone == Zone.ROAD_NS ? 5 : zone == Zone.ROAD_EW ? 10 : zone == Zone.CROSSROADS ? 15 : 0);
        }
        public Lot(Zone zone, AbandonedBuildings.Kind building, Rotation facing, boolean avenueNS, boolean avenueEW) {
            this(zone, building, facing, avenueNS, avenueEW, avenueNS ? 12 : 8, avenueEW ? 12 : 8, Condition.WORN, null);
        }
        public boolean isRoad() { return zone == Zone.ROAD_NS || zone == Zone.ROAD_EW || zone == Zone.CROSSROADS; }
        public boolean connects(net.minecraft.core.Direction direction) { return (connections & bit(direction)) != 0; }
        public boolean straight() { return connections == 5 || connections == 10; }
        public boolean bend() { return Integer.bitCount(connections) == 2 && !straight(); }
    }
    private DistrictLayout() { }
    public static int bit(net.minecraft.core.Direction direction) {
        return switch (direction) { case NORTH -> 1; case EAST -> 2; case SOUTH -> 4; case WEST -> 8; default -> 0; };
    }
    public static Lot at(net.minecraft.world.level.WorldGenLevel level, int cx, int cz) { return CityNetwork.lot(level, cx, cz); }
    public static boolean urban(long seed, int cx, int cz) {
        int x = Math.floorMod(cx, REGION_SIZE), z = Math.floorMod(cz, REGION_SIZE);
        long region = hash(seed ^ Math.floorDiv(cx, REGION_SIZE) * 341873128712L ^ Math.floorDiv(cz, REGION_SIZE) * 132897987541L);
        // 208-block cities, at least 176 blocks of fields between urban edges. Some regions stay rural.
        return (region & 3) != 0 && x >= 6 && x <= 18 && z >= 6 && z <= 18;
    }
    public static Lot at(long seed, int cx, int cz) {
        int x = Math.floorMod(cx, 3), z = Math.floorMod(cz, 3);
        boolean city = urban(seed, cx, cz);
        boolean highwayNS = Math.floorMod(cx, REGION_SIZE) == 12;
        boolean highwayEW = Math.floorMod(cz, REGION_SIZE) == 12;
        boolean ns = highwayNS || city && x == 0;
        boolean ew = highwayEW || city && z == 0;
        long plot = hash(seed ^ cx * 73428767L ^ cz * 912931L);
        Condition condition = Condition.values()[(int) Math.floorMod(plot >>> 5, 4)];
        boolean avenueNS = highwayNS, avenueEW = highwayEW;
        int widthNS = avenueNS ? 12 : Math.floorMod(cx, 6) == 0 ? 8 : 6;
        int widthEW = avenueEW ? 12 : Math.floorMod(cz, 6) == 0 ? 8 : 6;
        if (ns || ew) return new Lot(ns && ew ? Zone.CROSSROADS : ns ? Zone.ROAD_NS : Zone.ROAD_EW,
            null, Rotation.NONE, avenueNS, avenueEW, widthNS, widthEW, condition, null);
        if (!city || x == 0 || z == 0) return new Lot(Zone.WILDERNESS, null, Rotation.NONE, false, false);
        // The urban edge ends after complete 2x2 blocks; no orphan entrances face open fields.
        if (!urban(seed, cx + (x == 1 ? -1 : 1), cz) || !urban(seed, cx, cz + (z == 1 ? -1 : 1)))
            return new Lot(Zone.WILDERNESS, null, Rotation.NONE, false, false);
        long block = hash(seed ^ Math.floorDiv(cx, 3) * 341873128712L ^ Math.floorDiv(cz, 3) * 132897987541L);
        Rotation facing = (plot & 1) == 0 ? (x == 1 ? Rotation.COUNTERCLOCKWISE_90 : Rotation.CLOCKWISE_90)
            : (z == 1 ? Rotation.NONE : Rotation.CLOCKWISE_180);
        int complexRoll = (int) Math.floorMod(block, 12);
        if (complexRoll < 4) {
            var kind = LargeCityBuildings.Kind.values()[complexRoll];
            return new Lot(Zone.COMPLEX, null, Rotation.NONE, false, false, 0, 0, condition, kind);
        }
        int roll = (int) Math.floorMod(plot, 24);
        if (roll <= 1) return new Lot(Zone.PARK, null, facing, false, false);
        if (roll <= 3) return new Lot(Zone.PARKING, null, facing, false, false);
        var kinds = AbandonedBuildings.Kind.values();
        return new Lot(Zone.BUILDING, kinds[(int) Math.floorMod(plot >>> 8, kinds.length)], facing, false, false,
            0, 0, condition, null);
    }
    public static long hash(long value) {
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        return value ^ (value >>> 31);
    }
}
