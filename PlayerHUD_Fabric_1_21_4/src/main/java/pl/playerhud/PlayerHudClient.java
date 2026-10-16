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
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EquipmentSlot;
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
    
    // Wirtualny gracz do stabilnego renderowania skina 3D
    private static OtherClientPlayerEntity fakeRenderPlayer = null;

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
                fakeRenderPlayer = null;
            }
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient() && entity instanceof PlayerEntity) {
                targetPlayerUuid = entity.getUuid();
                lastAttackTime = System.currentTimeMillis();
                fakeRenderPlayer = null; // Reset klona przy zmianie celu
            }
            return ActionResult.PASS;
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            targetPlayerUuid = null;
            fakeRenderPlayer = null;
        });

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
                if (!HudConfig.isEnabled) return;

                // --- AKTYWACJA SUWAKA SKALI ---
                // Zapisujemy stan macierzy przed transformacją
                drawContext.getMatrices().push();
                
                // Przesuwamy punkt zero do pozycji HUD-a, aby skalował się od lewego górnego rogu swojej pozycji
                drawContext.getMatrices().translate(HudConfig.hudX, HudConfig.hudY, 0.0f);
                // Nakładamy wybraną skalę
                drawContext.getMatrices().scale(HudConfig.hudScale, HudConfig.hudScale, 1.0f);
                // Przesuwamy z powrotem, aby logika rysowania myślała, że rysuje od pozycji (0, 0)
                drawContext.getMatrices().translate(-HudConfig.hudX, -HudConfig.hudY, 0.0f);

                // Rysujemy spersonalizowany i wyskalowany HUD
                renderTargetStats(drawContext, client, targetPlayer);

                // Przywracamy domyślny stan macierzy, aby nie zepsuć reszty interfejsu gry
                drawContext.getMatrices().pop();
            }
        });
    }

    private void renderTargetStats(DrawContext context, MinecraftClient client, PlayerEntity target) {
        int currentX = HudConfig.hudX;
        int currentY = HudConfig.hudY;

        // 1. STABILNY RENDERING 3D (Za pomocą wirtualnego klona gracza)
        if (HudConfig.showSkin) {
            if (fakeRenderPlayer == null || !fakeRenderPlayer.getUuid().equals(target.getUuid())) {
                fakeRenderPlayer = new OtherClientPlayerEntity(client.world, target.getGameProfile());
            }

            // Kopiowanie aktualnego pancerza i przedmiotów z prawdziwego gracza na klona
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                fakeRenderPlayer.equipStack(slot, target.getEquippedStack(slot));
            }

            // Całkowite zamrożenie rotacji i wymuszenie pozycji stojącej na klonie
            fakeRenderPlayer.bodyYaw = 0.0F;
            fakeRenderPlayer.headYaw = 0.0F;
            fakeRenderPlayer.setPitch(0.0F);
            fakeRenderPlayer.prevBodyYaw = 0.0F;
            fakeRenderPlayer.prevHeadYaw = 0.0F;
            fakeRenderPlayer.prevPitch = 0.0F;
            fakeRenderPlayer.hurtTime = 0; // Likwiduje telepanie i czerwony błysk po hicie
            fakeRenderPlayer.setPose(EntityPose.STANDING); // Blokuje leżenie i pływanie

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
                    fakeRenderPlayer
            );

            currentX += 60;
        }

        int statsY = currentY + 5;

        if (HudConfig.showName) {
            context.drawText(client.textRenderer, "§6§l" + target.getName().getString(), currentX, statsY, 0xFFFFFF, true);
            statsY += 12;
        }

        // WYRÓWNANIE: Zmiana mnożnika pozycji X z "i * 8" na "i * 9" dopasowuje pancerz do szerokości serc
        if (HudConfig.showArmor) {
            int armorPoints = target.getArmor();
            for (int i = 0; i < 10; i++) {
                int armorX = currentX + i * 9; 
                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/armor_empty"), armorX, statsY, 9, 9);
                if (armorPoints - (i * 2) >= 2) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/armor_full"), armorX, statsY, 9, 9);
                } else if (armorPoints - (i * 2) > 0) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/armor_half"), armorX, statsY, 9, 9);
                }
            }
            statsY += 10;
        }

        if (HudConfig.showHearts) {
            float hp = target.getHealth();
            float maxHp = target.getMaxHealth();
            float absorption = target.getAbsorptionAmount();
            int index = 0;

            for (int i = 0; i < maxHp; i += 2) {
                int row = index / 10;
                int col = index % 10;
                int heartX = currentX + col * 9;
                int heartY = statsY + row * 10;

                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                if (hp - i >= 2) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/full"), heartX, heartY, 9, 9);
                } else if (hp - i > 0) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/half"), heartX, heartY, 9, 9);
                }
                index++;
            }

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
