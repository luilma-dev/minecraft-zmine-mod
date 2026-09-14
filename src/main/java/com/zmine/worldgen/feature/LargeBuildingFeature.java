package com.zmine.worldgen.feature;

import com.zmine.worldgen.CityTerrain;
import com.zmine.worldgen.LargeCityBuildings;
import com.zmine.worldgen.RuinsTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Explicit /place previews, with all four lots validated before any terrain is changed. */
@SuppressWarnings("null")
public final class LargeBuildingFeature extends Feature<NoneFeatureConfiguration> {
    private final LargeCityBuildings.Kind kind;
    public LargeBuildingFeature(LargeCityBuildings.Kind kind) { super(NoneFeatureConfiguration.CODEC); this.kind = kind; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        int x = context.origin().getX() & ~15, z = context.origin().getZ() & ~15;
        var level = context.level();
        int y = RuinsTerrain.ground(level, x + 15, z + 15).getY();
        var origin = new BlockPos(x, y, z);
        for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++)
            if (!CityTerrain.canPrepare(level, origin.offset(dx * 16, 0, dz * 16))) return false;
        for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++)
            if (!CityTerrain.prepare(level, origin.offset(dx * 16, 0, dz * 16))) return false;
        LargeCityBuildings.create(kind, context.random().nextLong()).placeOverlay(level, origin, Rotation.NONE, context.random());
        return true;
    }
}
