package pl.playerhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
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

        if (HudConfig.showSkin) {
            InventoryScreen.drawEntity(
                    context, currentX + 20, currentY + 70, 30,
                    (float)(currentX + 20) - client.getWindow().getScaledWidth() / 2,
                    (float)(currentY + 35) - client.getWindow().getScaledHeight() / 2,
                    target
            );
            currentX += 50;
        }

        // Zaktualizowany system rysowania serc - zgodny z Minecraft 1.21.4
        if (HudConfig.showHearts) {
            float hp = target.getHealth();
            float absorption = target.getAbsorptionAmount();
            int index = 0;

            // Czerwone serca
            for (int i = 0; i < hp; i += 2) {
                int row = index / 10;
                int col = index % 10;
                int heartX = currentX + col * 9;
                int heartY = currentY + row * 10;

                context.drawGuiTexture(Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                if (hp - i >= 2) {
                    context.drawGuiTexture(Identifier.ofVanilla("hud/heart/full"), heartX, heartY, 9, 9);
                } else {
                    context.drawGuiTexture(Identifier.ofVanilla("hud/heart/half"), heartX, heartY, 9, 9);
                }
                index++;
            }

            // Złote serca absorpcji
            if (absorption > 0) {
                for (int i = 0; i < absorption; i += 2) {
                    int row = index / 10;
                    int col = index % 10;
                    int heartX = currentX + col * 9;
                    int heartY = currentY + row * 10;

                    context.drawGuiTexture(Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                    if (absorption - i >= 2) {
                        context.drawGuiTexture(Identifier.ofVanilla("hud/heart/absorbing_full"), heartX, heartY, 9, 9);
                    } else {
                        context.drawGuiTexture(Identifier.ofVanilla("hud/heart/absorbing_half"), heartX, heartY, 9, 9);
                    }
                    index++;
                }
            }
        }

        if (HudConfig.showArmor) {
            int armorPoints = target.getArmor();
            int textY = currentY + ((target.getHealth() > 20) ? 24 : 14);
            context.drawText(client.textRenderer, "Pancerz: §b" + armorPoints, currentX, textY, 0xFFFFFF, true);
        }
    }
}
