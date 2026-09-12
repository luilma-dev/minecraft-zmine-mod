package com.zmine.mixin;

import com.zmine.survival.IThirstData;
import com.zmine.survival.ThirstManager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin implements IThirstData {
    @Unique
    private final ThirstManager thirstManager = new ThirstManager();

    @Override
    public ThirstManager zmine$getThirstManager() {
        return this.thirstManager;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide()) {
            this.thirstManager.tick(player);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void onReadSaveData(ValueInput input, CallbackInfo ci) {
        this.thirstManager.readAdditionalSaveData(input);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void onAddSaveData(ValueOutput output, CallbackInfo ci) {
        this.thirstManager.addAdditionalSaveData(output);
    }

    @Inject(method = "causeFoodExhaustion", at = @At("HEAD"))
    private void onCauseFoodExhaustion(float exhaustion, CallbackInfo ci) {
        Player player = (Player) (Object) this;
        if (!player.level().isClientSide()) {
            // A sede cai um pouco mais rápido que a fome (ex: 20% mais rápido)
            this.thirstManager.addExhaustion(exhaustion * 1.2F);
        }
    }
}
