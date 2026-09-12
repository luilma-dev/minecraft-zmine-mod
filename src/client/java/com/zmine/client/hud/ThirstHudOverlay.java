package com.zmine.client.hud;

import com.zmine.survival.IThirstData;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

@SuppressWarnings("null")
public class ThirstHudOverlay implements HudElement {
    private static final Identifier THIRST_EMPTY = Identifier.fromNamespaceAndPath("zmine", "hud/thirst_empty");
    private static final Identifier THIRST_FULL = Identifier.fromNamespaceAndPath("zmine", "hud/thirst_full");
    private static final Identifier THIRST_HALF = Identifier.fromNamespaceAndPath("zmine", "hud/thirst_half");
    
    @Override
    public void extractRenderState(GuiGraphicsExtractor extractor, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.player.isSpectator() || client.player.isCreative()) {
            return;
        }

        if (client.player instanceof IThirstData thirstData) {
            int thirstLevel = thirstData.zmine$getThirstManager().getThirstLevel();
            
            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();

            // Posição: acima da barra de fome
            int x = width / 2 + 91;
            int y = height - 49;

            // Desenha 10 gotinhas (cada uma vale 2 pontos)
            for (int i = 0; i < 10; i++) {
                int iconX = x - i * 8 - 9;
                
                // Draw background first
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_EMPTY, iconX, y, 9, 9);

                // Draw full or half drop on top
                if (thirstLevel > i * 2 + 1) {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_FULL, iconX, y, 9, 9);
                } else if (thirstLevel == i * 2 + 1) {
                    extractor.blitSprite(RenderPipelines.GUI_TEXTURED, THIRST_HALF, iconX, y, 9, 9);
                }
            }
        }
    }
}
