package com.zmine;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;

public class ZMineWorldGen {
    public static void registerWorldGen() {
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
    }
}
