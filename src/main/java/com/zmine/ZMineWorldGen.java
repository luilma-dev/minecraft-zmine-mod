package com.zmine;

import com.zmine.worldgen.feature.AbandonedBuildingFeature;
import com.zmine.worldgen.AbandonedBuildings;
import com.zmine.worldgen.feature.ApocalypseDistrictFeature;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

@SuppressWarnings("null")
public class ZMineWorldGen {
    public static final Feature<NoneFeatureConfiguration> ABANDONED_BUILDING = new AbandonedBuildingFeature(null);

    public static void registerWorldGen() {
        Registry.register(BuiltInRegistries.FEATURE, ZMineMod.id("abandoned_building"), ABANDONED_BUILDING);
        Registry.register(BuiltInRegistries.FEATURE, ZMineMod.id("apocalypse_district"), new ApocalypseDistrictFeature());
        for (AbandonedBuildings.Kind kind : AbandonedBuildings.Kind.values()) {
            Registry.register(BuiltInRegistries.FEATURE, ZMineMod.id(kind.id), new AbandonedBuildingFeature(kind));
        }
        for (var kind : com.zmine.worldgen.LargeCityBuildings.Kind.values()) {
            Registry.register(BuiltInRegistries.FEATURE, ZMineMod.id(kind.id), new com.zmine.worldgen.feature.LargeBuildingFeature(kind));
        }

        // Aumenta o spawn de Zumbis em todos os biomas
        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            EntityTypes.ZOMBIE,
            100, // peso bem alto
            4,   // mínimo no grupo
            15   // máximo no grupo
        );

        BiomeModifications.addSpawn(
            BiomeSelectors.all(),
            MobCategory.MONSTER,
            EntityTypes.HUSK,
            50,
            2,
            8
        );

        // Trees, lakes and springs can write across chunk borders after their neighbor's streets.
        // Keep underground resources, but replace vanilla surface decoration with the city.
        BiomeModifications.create(ZMineMod.id("city_surface")).add(ModificationPhase.REPLACEMENTS,
            BiomeSelectors.foundInOverworld(), (selection, context) -> {
                var features = selection.getBiome().getGenerationSettings().features();
                for (var step : new GenerationStep.Decoration[]{GenerationStep.Decoration.LAKES,
                    GenerationStep.Decoration.LOCAL_MODIFICATIONS, GenerationStep.Decoration.SURFACE_STRUCTURES,
                    GenerationStep.Decoration.FLUID_SPRINGS, GenerationStep.Decoration.VEGETAL_DECORATION,
                    GenerationStep.Decoration.TOP_LAYER_MODIFICATION}) {
                    if (step.ordinal() >= features.size()) continue;
                    // Snapshot keys: biome settings are being modified during this callback.
                    var keys = features.get(step.ordinal()).stream().flatMap(holder -> holder.unwrapKey().stream()).toList();
                    keys.stream().filter(key -> key.identifier().getNamespace().equals("minecraft"))
                        .forEach(key -> context.getGenerationSettings().removeFeature(step, key));
                }
            });

        // A road every three chunks, with avenues shared across district boundaries.
        ResourceKey<net.minecraft.world.level.levelgen.placement.PlacedFeature> districtKey = ResourceKey.create(
            Registries.PLACED_FEATURE,
            ZMineMod.id("apocalypse_district")
        );

        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Decoration.TOP_LAYER_MODIFICATION,
            districtKey
        );

        // Standalone buildings are /place previews. Only the district generates natural buildings.
    }
}
