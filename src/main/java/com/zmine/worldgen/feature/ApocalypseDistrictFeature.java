package com.zmine.worldgen.feature;

import com.zmine.worldgen.AbandonedBuildings;
import com.zmine.worldgen.CityLots;
import com.zmine.worldgen.CityRoads;
import com.zmine.worldgen.CityTerrain;
import com.zmine.worldgen.DistrictLayout;
import com.zmine.worldgen.CityElevation;
import com.zmine.worldgen.LargeCityBuildings;
import com.zmine.worldgen.ApocalypseScenery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

@SuppressWarnings("null")
public final class ApocalypseDistrictFeature extends Feature<NoneFeatureConfiguration> {
    public ApocalypseDistrictFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int cx = context.origin().getX() >> 4, cz = context.origin().getZ() >> 4;
        var lot = DistrictLayout.at(context.level(), cx, cz);
        var level = context.level();
        if (lot.zone() == DistrictLayout.Zone.WILDERNESS) {
            CityTerrain.blendRoadside(level, cx, cz);
            return ApocalypseScenery.countryside(level, cx << 4, cz << 4, context.random());
        }
        if (lot.isRoad()) {
            int[][] heights = CityElevation.roadProfile(level, cx, cz);
            var origin = new BlockPos(cx << 4, heights[7][7], cz << 4);
            var bed = CityTerrain.prepareRoad(level, origin, lot, heights);
            if (bed == null) return false;
            CityRoads.paint(level, origin, lot, heights, context.random(), bed);
            return true;
        }
        int blockX = Math.floorDiv(cx, 3) * 3 + 1, blockZ = Math.floorDiv(cz, 3) * 3 + 1;
        int blockY = CityElevation.streetY(level, blockX * 16, blockZ * 16 - 1);
        if (!CityElevation.buildableBlock(level, blockX, blockZ, blockY)) {
            CityTerrain.blendRoadside(level, cx, cz);
            return ApocalypseScenery.countryside(level, cx << 4, cz << 4, context.random());
        }
        if (lot.zone() == DistrictLayout.Zone.COMPLEX) {
            int ax = Math.floorDiv(cx, 3) * 3 + 1, az = Math.floorDiv(cz, 3) * 3 + 1;
            int y = CityElevation.streetY(level, ax * 16 + 15, az * 16 - 1);
            var origin = new BlockPos(cx << 4, y, cz << 4);
            if (!CityTerrain.prepare(level, origin)) return false;
            long seed = DistrictLayout.hash(level.getSeed() ^ ax * 341873128712L ^ az * 132897987541L);
            LargeCityBuildings.create(lot.complex(), seed).rotated(lot.facing()).placeSlice(level, new BlockPos(ax << 4, y, az << 4), cx, cz, seed);
            return true;
        }
        var origin = new BlockPos(cx << 4, CityElevation.lotY(level, cx, cz, lot.facing()), cz << 4);
        if (!CityTerrain.prepare(level, origin)) return false;
        if (lot.zone() == DistrictLayout.Zone.BUILDING) {
            AbandonedBuildings.create(lot.building(), context.random()).placeOverlay(context.level(), origin, lot.facing(), context.random());
        } else {
            CityLots.create(lot.zone(), context.random()).placeOverlay(context.level(), origin, lot.facing(), context.random());
        }
        return true;
    }
}
