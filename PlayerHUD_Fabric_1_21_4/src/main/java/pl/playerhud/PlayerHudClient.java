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
import net.minecraft.item.ItemStack;
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
                fakeRenderPlayer = null;
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

                drawContext.getMatrices().push();
                drawContext.getMatrices().translate(HudConfig.hudX, HudConfig.hudY, 0.0f);
                drawContext.getMatrices().scale(HudConfig.hudScale, HudConfig.hudScale, 1.0f);
                drawContext.getMatrices().translate(-HudConfig.hudX, -HudConfig.hudY, 0.0f);

                renderTargetStats(drawContext, client, targetPlayer);

                drawContext.getMatrices().pop();
            }
        });
    }

    private void renderTargetStats(DrawContext context, MinecraftClient client, PlayerEntity target) {
        int currentX = HudConfig.hudX;
        int currentY = HudConfig.hudY;

        if (HudConfig.showSkin) {
            if (fakeRenderPlayer == null || !fakeRenderPlayer.getUuid().equals(target.getUuid())) {
                fakeRenderPlayer = new OtherClientPlayerEntity(client.world, target.getGameProfile());
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                fakeRenderPlayer.equipStack(slot, target.getEquippedStack(slot));
            }

            fakeRenderPlayer.bodyYaw = 0.0F;
            fakeRenderPlayer.headYaw = 0.0F;
            fakeRenderPlayer.setPitch(0.0F);
            fakeRenderPlayer.prevBodyYaw = 0.0F;
            fakeRenderPlayer.headYaw = 0.0F;
            fakeRenderPlayer.prevPitch = 0.0F;
            
            if (HudConfig.showHurtFlash) {
                fakeRenderPlayer.hurtTime = target.hurtTime;
                fakeRenderPlayer.maxHurtTime = target.maxHurtTime;
            } else {
                fakeRenderPlayer.hurtTime = 0;
            }
            
            fakeRenderPlayer.setPose(EntityPose.STANDING);

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
        int armorPoints = target.getArmor();

        // OBSŁUGA 3 TRYBÓW WYŚWIETLANIA ARMORA
        if (HudConfig.armorDisplayMode == 0) {
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
        else if (HudConfig.armorDisplayMode == 1) {
            int armorPct = (int) ((armorPoints / 20.0f) * 100);
            context.drawText(client.textRenderer, "§9Ochrona: §f" + armorPct + "%", currentX, statsY, 0xFFFFFF, true);
            statsY += 10;
        } 
        else if (HudConfig.armorDisplayMode == 2) {
            context.drawText(client.textRenderer, "§9Armor: §f" + armorPoints + " / 20", currentX, statsY, 0xFFFFFF, true);
            statsY += 10;
        }

        float hp = target.getHealth();
        float maxHp = target.getMaxHealth();
        float absorption = target.getAbsorptionAmount();

        // OBSŁUGA 3 TRYBÓW WYŚWIETLANIA ZDROWIA
        if (HudConfig.hpDisplayMode == 0) {
            int index = 0;
            for (int i = 0; i < maxHp; i += 2) {
                int row = index / 10;
                int col = index % 10;
                int heartX = currentX + col * 9;
                int heartY = statsY + row * 10;

                context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                float diff = hp - i;
                if (diff >= 1.5f) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/full"), heartX, heartY, 9, 9);
                } else if (diff > 0.1f) {
                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/half"), heartX, heartY, 9, 9);
                }
                index++;
            }
            statsY += (((int)maxHp - 1) / 20 + 1) * 10;

            if (absorption > 0) {
                int absIndex = 0;
                for (int i = 0; i < absorption; i += 2) {
                    int row = absIndex / 10;
                    int col = absIndex % 10;
                    int heartX = currentX + col * 9;
                    int heartY = statsY + row * 10;

                    context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/container"), heartX, heartY, 9, 9);
                    float diffAbs = absorption - i;
                    if (diffAbs >= 1.5f) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/absorbing_full"), heartX, heartY, 9, 9);
                    } else if (diffAbs > 0.1f) {
                        context.drawGuiTexture(RenderLayer::getGuiTextured, Identifier.ofVanilla("hud/heart/absorbing_half"), heartX, heartY, 9, 9);
                    }
                    absIndex++;
                }
                statsY += ((int)(absorption - 1) / 20 + 1) * 10;
            }
        } 
        else if (HudConfig.hpDisplayMode == 1) {
            int pct = (int) ((hp / maxHp) * 100);
            context.drawText(client.textRenderer, "§cŻycie: §f" + pct + "%", currentX, statsY, 0xFFFFFF, true);
            statsY += 10;
            
            if (absorption > 0) {
                context.drawText(client.textRenderer, "§eAbsorpcja: §f+" + String.format("%.1f", absorption), currentX, statsY, 0xFFFFFF, true);
                statsY += 10;
            }
        } 
        else if (HudConfig.hpDisplayMode == 2) {
            context.drawText(client.textRenderer, "§cHP: §f" + String.format("%.1f", hp) + " / " + String.format("%.1f", maxHp), currentX, statsY, 0xFFFFFF, true);
            statsY += 10;
            
            if (absorption > 0) {
                context.drawText(client.textRenderer, "§eDodatkowe HP: §f" + String.format("%.1f", absorption), currentX, statsY, 0xFFFFFF, true);
                statsY += 10;
            }
        }

        // PRZEDMIOTY (NA SAMYM DOLE, POD INDYWIDUALNYMI STYLAMI STATYSTYK)
        if (HudConfig.showItems) {
            ItemStack mainHand = target.getMainHandStack();
            ItemStack offHand = target.getOffHandStack();
            int itemY = statsY + 4;

            if (!mainHand.isEmpty()) {
                context.drawItem(mainHand, currentX, itemY);
            }
            if (!offHand.isEmpty()) {
                context.drawItem(offHand, currentX + 18, itemY);
            }
        }
    }
}
