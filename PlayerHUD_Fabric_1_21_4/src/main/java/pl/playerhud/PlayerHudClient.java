package pl.playerhud;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import java.util.UUID;

public class PlayerHudClient implements ClientModInitializer {
    public static KeyBinding openSettingsKey;
    
    public static UUID targetPlayerUuid = null;
    public static long lastAttackTime = 0;
    private static final long HUD_DURATION_MS = 15000;

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
            
            if (targetPlayerUuid != null && (System.currentTimeMillis() - lastAttackTime > HUD_DURATION_MS)) {
                targetPlayerUuid = null;
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity) {
                targetPlayerUuid = entity.getUuid();
                lastAttackTime = System.currentTimeMillis();
            }
            return ActionResult.PASS;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> targetPlayerUuid = null);

        HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.currentScreen != null || targetPlayerUuid == null) return;

            PlayerEntity targetPlayer = null;
            for (PlayerEntity p : client.world.getPlayers()) {
                if (p.getUuid().equals(targetPlayerUuid)) {
                    targetPlayer = p;
                    break;
                }
            }

            if (targetPlayer != null && !targetPlayer.isRemoved()) {
                renderTargetStats(drawContext, client, targetPlayer);
            }
        });
    }

    private void renderTargetStats(DrawContext context, MinecraftClient client, PlayerEntity target) {
        if (!HudConfig.isEnabled) return;

        int currentX = HudConfig.hudX;
        int currentY = HudConfig.hudY;

        // 1. Rendering postaci 3D (Zablokowanie leżenia/pływania oraz rotacji)
        if (HudConfig.showSkin) {
            float savedYaw = target.bodyYaw;
            float savedHeadYaw = target.headYaw;
            float savedPitch = target.getPitch();
            EntityPose savedPose = target.getPose();
            
            // CAŁKOWITE WYMUSZENIE POZYCJI STOJĄCEJ DLA 1.21.4
            target.bodyYaw = 0.0F;
            target.headYaw = 0.0F;
            target.setPitch(0.0F);
            target.setPose(EntityPose.STANDING);
            target.prevBodyYaw = 0.0F;
            target.prevHeadYaw = 0.0F;
            target.prevPitch = 0.0F;

            Quaternionf rotation = new Quaternionf().rotationXYZ(0.0F, 3.1415927F, 3.1415927F);
            Quaternionf cameraOrientation = new Quaternionf().rotationXYZ(0.0F, 0.0F, 0.0F);
            
            InventoryScreen.drawEntity(
                    context, 
                    (float)(currentX + 25), 
                    (float)(currentY + 75), 
                    30.0F, 
                    new Vector3f(0.0F, 0.0F, 0.0F), 
                    rotation, 
                    cameraOrientation, 
                    target
            );

            target.bodyYaw = savedYaw;
            target.headYaw = savedHeadYaw;
            target.setPitch(savedPitch);
            target.setPose(savedPose);

            currentX += 60;
        }

        int statsY = currentY + 5;

        if (HudConfig.showName) {
            context.drawText(client.textRenderer, "§6§l" + target.getName().getString(), currentX, statsY, 0xFFFFFF, true);
            statsY += 12;
        }

        // 2. Naprawiony Armor HUD (Prawidłowe ścieżki ikon Minecraft 1.21.4)
        if (HudConfig.showArmor) {
            int armorPoints = target.getArmor();
            for (int i = 0; i < 10; i++) {
                int armorX = currentX + i * 8;
                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/armor_empty"), armorX, statsY, 9, 9);
                if (armorPoints - (i * 2) >= 2) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/armor_full"), armorX, statsY, 9, 9);
                } else if (armorPoints - (i * 2) > 0) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/armor_half"), armorX, statsY, 9, 9);
                }
            }
            statsY += 10;
        }

        // 3. System Serc wyświetlający puste/zbite serduszka (Obsługa np. 30 serc max i 20 aktualnych)
        if (HudConfig.showHearts) {
            float hp = target.getHealth();
            float maxHp = target.getMaxHealth();
            float absorption = target.getAbsorptionAmount();
            int index = 0;

            // Rysowanie pojemników (pustych serc) na podstawie maksymalnego zdrowia gracza
            for (int i = 0; i < maxHp; i += 2) {
                int row = index / 10;
                int col = index % 10;
                int heartX = currentX + col * 9;
                int heartY = statsY + row * 10;

                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                
                // Wypełnianie serduszek czerwonym kolorem (jeśli gracz ma tam zdrowie)
                if (hp - i >= 2) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/full"), heartX, heartY, 9, 9);
                } else if (hp - i > 0) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/half"), heartX, heartY, 9, 9);
                }
                index++;
            }

            // Dodatkowe złote serca absorpcji (w nowych rządkach na samym końcu)
            if (absorption > 0) {
                for (int i = 0; i < absorption; i += 2) {
                    int row = index / 10;
                    int col = index % 10;
                    int heartX = currentX + col * 9;
                    int heartY = statsY + row * 10;

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
    }
}
