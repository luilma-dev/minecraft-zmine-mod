package com.zmine.worldgen.feature;

import com.zmine.worldgen.AbandonedBuildings;
import com.zmine.worldgen.BuildingBlueprint;
import com.zmine.worldgen.RuinsTerrain;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

@SuppressWarnings("null")
public final class AbandonedBuildingFeature extends Feature<NoneFeatureConfiguration> {
    private final AbandonedBuildings.Kind kind;
    private final Rotation fixedRotation;

    /** null selects a weighted random building during natural generation. */
    public AbandonedBuildingFeature(AbandonedBuildings.Kind kind) {
        this(kind, null);
    }

    public AbandonedBuildingFeature(AbandonedBuildings.Kind kind, Rotation fixedRotation) {
        super(NoneFeatureConfiguration.CODEC);
        this.kind = kind;
        this.fixedRotation = fixedRotation;
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        var random = context.random();
        AbandonedBuildings.Kind selected = kind;
        if (selected == null) {
            var kinds = AbandonedBuildings.Kind.values();
            selected = kinds[random.nextInt(kinds.length)];
        }
        BuildingBlueprint blueprint = AbandonedBuildings.create(selected, random);
        // One lot per candidate chunk, never writes into neighboring decoration chunks.
        int x0 = context.origin().getX() & ~15;
        int z0 = context.origin().getZ() & ~15;
        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int x = 0; x < BuildingBlueprint.SIZE; x++) for (int z = 0; z < BuildingBlueprint.SIZE; z++) {
            BlockPos pos = RuinsTerrain.ground(level, x0 + x, z0 + z);
            if (!RuinsTerrain.dryNaturalGround(level, pos)) return false;
            low = Math.min(low, pos.getY());
            high = Math.max(high, pos.getY());
        }
        if (high - low > 3 || high + blueprint.height() >= level.getMaxY()) return false;
        BlockPos origin = new BlockPos(x0, high, z0);
        int maxY = blueprint.height();
        for (int x = 0; x < BuildingBlueprint.SIZE; x++) for (int z = 0; z < BuildingBlueprint.SIZE; z++) {
            // Foundation placement can reach below the visible footprint. Protect buried loot too.
            for (int depth = 1; depth <= 4; depth++) {
                BlockPos support = origin.offset(x, -depth, z);
                if (support.getY() < level.getMinY()) break;
                if (!level.ensureCanWrite(support) || level.getBlockState(support).hasBlockEntity()) return false;
                if (level.getBlockState(support).isSolidRender()) break;
            }
            for (int y = 0; y < maxY; y++) {
                BlockPos pos = origin.offset(x, y, z);
                if (!level.ensureCanWrite(pos) || level.getBlockState(pos).hasBlockEntity()) return false;
            }
        }
        blueprint.place(level, origin, fixedRotation != null ? fixedRotation : Rotation.values()[random.nextInt(4)], random);
        return true;
    }

}
