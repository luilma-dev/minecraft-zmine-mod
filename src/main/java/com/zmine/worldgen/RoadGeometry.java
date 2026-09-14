package com.zmine.worldgen;

/** Shared, world-coordinate geometry for asphalt, markings, bridge decks and parapets. */
public final class RoadGeometry {
    private RoadGeometry() { }

    public static boolean northSouth(DistrictLayout.Lot lot) { return (lot.connections() & 5) != 0; }

    public static double center(long seed, int cx, int cz, DistrictLayout.Lot lot, int along) {
        if (!lot.straight()) return 7.5;
        boolean ns = northSouth(lot);
        int world = (ns ? cz : cx) * 16 + along;
        int span = Math.floorDiv(world, 48), coordinate = Math.floorMod(world, 48);
        // Tangents meet the straight intersection approaches, including at negative coordinates.
        if (coordinate < 16) return 7.5;
        double t = (coordinate - 15) / 33.0;
        int width = ns ? lot.widthNS() : lot.widthEW();
        // Reserve room for both pavements AND the tunnel lining at the outside of a bend.
        double amplitude = width >= 12 ? 0.4 : width >= 8 ? 0.9 : 1.9;
        long bend = DistrictLayout.hash(seed ^ (ns ? cx : cz) * 73428767L ^ span * 912931L);
        return 7.5 + ((bend & 1) == 0 ? 1 : -1) * amplitude * Math.pow(Math.sin(Math.PI * t), 2);
    }

    public static double offset(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        return (northSouth(lot) ? x : z) - center(seed, cx, cz, lot, northSouth(lot) ? z : x);
    }

    private static int sidewalkWidth(int width) { return width >= 12 ? 1 : 2; }

    private static double halfWidth(DistrictLayout.Lot lot, int x, int z, boolean pavement) {
        if (!lot.bend()) return width(lot) / 2.0 + (pavement ? sidewalkWidth(width(lot)) : 0);
        double cornerX = (lot.connections() & 2) != 0 ? 15.5 : -.5;
        double cornerZ = (lot.connections() & 4) != 0 ? 15.5 : -.5;
        double t = Math.atan2(Math.abs(z - cornerZ), Math.abs(x - cornerX)) / (Math.PI / 2);
        return (lot.widthNS() / 2.0 + (pavement ? sidewalkWidth(lot.widthNS()) : 0)) * (1 - t)
            + (lot.widthEW() / 2.0 + (pavement ? sidewalkWidth(lot.widthEW()) : 0)) * t;
    }

    /** Signed offset from a quarter-circle for a two-exit corner. */
    public static double turnOffset(DistrictLayout.Lot lot, int x, int z) {
        double cornerX = (lot.connections() & 2) != 0 ? 15.5 : -.5;
        double cornerZ = (lot.connections() & 4) != 0 ? 15.5 : -.5;
        return Math.hypot(x - cornerX, z - cornerZ) - 8;
    }

    public static double laneDistance(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        return distance(seed, cx, cz, lot, x, z, false);
    }

    public static double pavementDistance(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        return distance(seed, cx, cz, lot, x, z, true);
    }

    private static double distance(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z, boolean pavement) {
        if (!lot.isRoad()) return Double.POSITIVE_INFINITY;
        if (lot.straight()) return Math.abs(offset(seed, cx, cz, lot, x, z)) - halfWidth(lot, x, z, pavement);
        if (lot.bend()) return Math.abs(turnOffset(lot, x, z)) - halfWidth(lot, x, z, pavement);
        // T and four-way junctions contain only actual connected approaches.
        double dx = x - 7.5, dz = z - 7.5;
        double ns = lot.widthNS() / 2.0 + (pavement ? sidewalkWidth(lot.widthNS()) : 0);
        double ew = lot.widthEW() / 2.0 + (pavement ? sidewalkWidth(lot.widthEW()) : 0);
        double distance = Double.POSITIVE_INFINITY;
        if ((lot.connections() & 1) != 0) distance = Math.min(distance, Math.hypot(dx, Math.max(0, dz)) - ns);
        if ((lot.connections() & 4) != 0) distance = Math.min(distance, Math.hypot(dx, Math.min(0, dz)) - ns);
        if ((lot.connections() & 2) != 0) distance = Math.min(distance, Math.hypot(Math.min(0, dx), dz) - ew);
        if ((lot.connections() & 8) != 0) distance = Math.min(distance, Math.hypot(Math.max(0, dx), dz) - ew);
        return distance;
    }

    private static boolean footprint(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z, double shoulder) {
        return laneDistance(seed, cx, cz, lot, x, z) < shoulder;
    }

    public static int sidewalkWidth(DistrictLayout.Lot lot) { return sidewalkWidth(lot.straight() ? width(lot) : Math.max(lot.widthNS(), lot.widthEW())); }
    public static boolean pavement(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        return pavementDistance(seed, cx, cz, lot, x, z) < 0;
    }
    public static boolean tunnelShell(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        return pavementDistance(seed, cx, cz, lot, x, z) < 1;
    }
    public static int ceiling(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        double distance = laneDistance(seed, cx, cz, lot, x, z);
        return distance < -2 ? 8 : distance < 0 ? 7 : 6;
    }

    public static boolean lane(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        return footprint(seed, cx, cz, lot, x, z, 0);
    }
    public static int width(DistrictLayout.Lot lot) { return northSouth(lot) ? lot.widthNS() : lot.widthEW(); }
    public static boolean deck(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        // Parapets occupy their own outer strip, leaving the sidewalk usable over water too.
        return tunnelShell(seed, cx, cz, lot, x, z);
    }
    public static boolean edge(long seed, int cx, int cz, DistrictLayout.Lot lot, int x, int z) {
        if (!deck(seed, cx, cz, lot, x, z)) return false;
        for (var direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            int nx = x + direction.getStepX(), nz = z + direction.getStepZ();
            if ((nx < 0 || nx > 15 || nz < 0 || nz > 15) && lot.connects(direction)) continue;
            if (!deck(seed, cx, cz, lot, nx, nz)) return true;
        }
        return false;
    }
}
