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
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

public class PlayerHudClient implements ClientModInitializer {
    public static KeyBinding openSettingsKey;
    private static final Identifier HEART_TEXTURES = Identifier.ofVanilla("textures/gui/icons.png");

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

        // 2. Rendering serduszek (Za pomocą uniwersalnych koordynatów UV z pliku icons.png)
        if (HudConfig.showHearts) {
            float hp = target.getHealth();
            float absorption = target.getAbsorptionAmount();
            int index = 0;

            // Rysowanie czerwonych serc (Puste tło: U=16, V=0 | Pełne: U=52, V=0 | Połówka: U=61, V=0)
            for (int i = 0; i < hp; i += 2) {
                int row = index / 10;
                int col = index % 10;
                int heartX = currentX + col * 9;
                int heartY = currentY + row * 10;

                context.drawTexture(HEART_TEXTURES, heartX, heartY, 16, 0, 9, 9, 256, 256);
                if (hp - i >= 2) {
                    context.drawTexture(HEART_TEXTURES, heartX, heartY, 52, 0, 9, 9, 256, 256);
                } else {
                    context.drawTexture(HEART_TEXTURES, heartX, heartY, 61, 0, 9, 9, 256, 256);
                }
                index++;
            }

            // Rysowanie złotej absorpcji (Pełne: U=160, V=0 | Połówka: U=169, V=0)
            if (absorption > 0) {
                for (int i = 0; i < absorption; i += 2) {
                    int row = index / 10;
                    int col = index % 10;
                    int heartX = currentX + col * 9;
                    int heartY = currentY + row * 10;

                    context.drawTexture(HEART_TEXTURES, heartX, heartY, 16, 0, 9, 9, 256, 256);
                    if (absorption - i >= 2) {
                        context.drawTexture(HEART_TEXTURES, heartX, heartY, 160, 0, 9, 9, 256, 256);
                    } else {
                        context.drawTexture(HEART_TEXTURES, heartX, heartY, 169, 0, 9, 9, 256, 256);
                    }
                    index++;
                }
            }
        }

        // 3. Punkty zbroi pancerza przeciwnika
        if (HudConfig.showArmor) {
            int armorPoints = target.getArmor();
            int textY = currentY + ((target.getHealth() > 20) ? 24 : 14);
            context.drawText(client.textRenderer, "Pancerz: §b" + armorPoints, currentX, textY, 0xFFFFFF, true);
        }
    }
}
