package pl.playerhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class PlayerHudClient implements ClientModInitializer {
    public static KeyBinding openSettingsKey;

    @Override
    public void onInitializeClient() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.playerhud.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                "category.playerhud.general"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openSettingsKey.wasPressed()) {
                client.setScreen(new HudSettingsScreen());
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.currentScreen != null) return;

            HitResult hitResult = client.crosshairTarget;
            if (hitResult != null && hitResult.getType() == HitResult.Type.ENTITY) {
                EntityHitResult entityHit = (EntityHitResult) hitResult;
                if (entityHit.getEntity() instanceof PlayerEntity targetPlayer) {
                    renderTargetStats(drawContext, client, targetPlayer);
                }
            }
        });
    }

    private void renderTargetStats(DrawContext context, MinecraftClient client, PlayerEntity target) {
        if (!HudConfig.isEnabled) return;

        int currentX = HudConfig.hudX;
        int currentY = HudConfig.hudY;

        // 1. Rendering postaci 3D gracza (Zgodny z Minecraft 1.21.4)
        if (HudConfig.showSkin) {
            Quaternionf rotation = new Quaternionf().rotationXYZ(0.0F, 0.0F, 3.1415927F);
            Quaternionf cameraOrientation = new Quaternionf().rotationXYZ(0.0F, 0.0F, 0.0F);
            
            InventoryScreen.drawEntity(
                    context, 
                    (float)(currentX + 20), 
                    (float)(currentY + 70), 
                    30.0F, 
                    new Vector3f(0.0F, 0.0F, 0.0F), 
                    rotation, 
                    cameraOrientation, 
                    target
            );
            currentX += 50;
        }

        // 2. Rendering zdrowia w rządkach po max 10 oryginalnych serduszek z Minecrafta
        if (HudConfig.showHearts) {
            float hp = target.getHealth();
            float absorption = target.getAbsorptionAmount();
            int index = 0;

            // Rysowanie czerwonych serduszek (Wersja 1.21.4 wymaga RenderLayer::getGuiTextured)
            for (int i = 0; i < hp; i += 2) {
                int row = index / 10;
                int col = index % 10;
                int heartX = currentX + col * 9;
                int heartY = currentY + row * 10;

                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                if (hp - i >= 2) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/full"), heartX, heartY, 9, 9);
                } else {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/half"), heartX, heartY, 9, 9);
                }
                index++;
            }

            // Rysowanie złotych serduszek absorpcji
            if (absorption > 0) {
                for (int i = 0; i < absorption; i += 2) {
                    int row = index / 10;
                    int col = index % 10;
                    int heartX = currentX + col * 9;
                    int heartY = currentY + row * 10;

                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                    if (absorption - i >= 2) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/absorbing_full"), heartX, heartY, 9, 9);
                    } else {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/absorbing_half"), heartX, heartY, 9, 9);
                    }
                    index++;
                }
            }
        }

        // 3. Punkty zbroi pancerza przeciwnika
        if (HudConfig.showArmor) {
            int armorPoints = target.getArmor();
            int totalHearts = (int) Math.ceil((target.getHealth() + target.getAbsorptionAmount()) / 2.0);
            int rows = (int) Math.ceil(totalHearts / 10.0);
            if (rows < 1) rows = 1;
            int textY = currentY + (rows * 10) + 2;
            
            context.drawText(client.textRenderer, "Pancerz: §b🛡 " + armorPoints, currentX, textY, 0xFFFFFF, true);
        }
    }
}
