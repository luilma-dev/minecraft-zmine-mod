package com.zmine.survival;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.Difficulty;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.zmine.network.SyncThirstPayload;
import net.minecraft.server.level.ServerPlayer;

public class ThirstManager {
    private int thirstLevel = 20;
    private float thirstExhaustion = 0.0f;
    private int tickTimer = 0;
    private int lastSentThirstLevel = -1;

    public void addExhaustion(float exhaustion) {
        this.thirstExhaustion = Math.min(this.thirstExhaustion + exhaustion, 40.0F);
    }

    public void addThirst(int thirst) {
        this.thirstLevel = Math.min(this.thirstLevel + thirst, 20);
    }

    public void tick(Player player) {
        Difficulty difficulty = player.level().getDifficulty();
        
        if (this.thirstExhaustion > 4.0F) {
            this.thirstExhaustion -= 4.0F;
            if (this.thirstLevel > 0) {
                this.thirstLevel = Math.max(this.thirstLevel - 1, 0);
            }
        }

        // Damage when thirst is 0
        if (this.thirstLevel <= 0) {
            this.tickTimer++;
            if (this.tickTimer >= 80) { // Every 4 seconds
                if (player.getHealth() > 1.0F || difficulty == Difficulty.HARD) {
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        player.hurtServer(serverLevel, player.damageSources().starve(), 1.0F); // Use starve damage type or generic
                    }
                }
                this.tickTimer = 0;
            }
        } else {
            this.tickTimer = 0;
        }

        if (player instanceof ServerPlayer serverPlayer && this.thirstLevel != this.lastSentThirstLevel) {
            ServerPlayNetworking.send(serverPlayer, new SyncThirstPayload(this.thirstLevel));
            this.lastSentThirstLevel = this.thirstLevel;
        }
    }

    public void readAdditionalSaveData(ValueInput input) {
        this.thirstLevel = input.getIntOr("ThirstLevel", 20);
        this.thirstExhaustion = input.getFloatOr("ThirstExhaustion", 0.0f);
    }

    public void addAdditionalSaveData(ValueOutput output) {
        output.putInt("ThirstLevel", this.thirstLevel);
        output.putFloat("ThirstExhaustion", this.thirstExhaustion);
    }

    public int getThirstLevel() {
        return thirstLevel;
    }

    public void setThirstLevel(int thirstLevel) {
        this.thirstLevel = thirstLevel;
    }
}
