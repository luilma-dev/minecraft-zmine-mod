package com.zmine.mixin.client;

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldCreationUiState.class)
public class WorldCreationUiStateMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        WorldCreationUiState state = (WorldCreationUiState) (Object) this;
        state.setGameMode(WorldCreationUiState.SelectedGameMode.HARDCORE);
    }
}
