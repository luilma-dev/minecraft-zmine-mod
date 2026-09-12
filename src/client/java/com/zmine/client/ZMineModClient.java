package com.zmine.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.zmine.network.SyncThirstPayload;
import com.zmine.survival.IThirstData;

@SuppressWarnings("null")
public class ZMineModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(SyncThirstPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player instanceof IThirstData thirstData) {
                    thirstData.zmine$getThirstManager().setThirstLevel(payload.thirstLevel());
                }
            });
        });

        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addLast(
            net.minecraft.resources.Identifier.fromNamespaceAndPath("zmine", "thirst"),
            new com.zmine.client.hud.ThirstHudOverlay()
        );
    }
}
