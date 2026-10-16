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

        // 1. Rendering postaci 3D gracza (To działa już bezbłędnie na 1.21.4!)
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

        // 2. Wyświetlanie Zdrowia i Absorpcji w postaci czytelnego licznika tekstowego
        if (HudConfig.showHearts) {
            int currentHp = (int) target.getHealth();
            int maxHp = (int) target.getMaxHealth();
            int abs = (int) target.getAbsorptionAmount();

            // Rysowanie tekstu życia
            context.drawText(client.textRenderer, "Zdrowie: §c❤ " + currentHp + "§7/§c" + maxHp, currentX, currentY + 5, 0xFFFFFF, true);
            
            // Rysowanie tekstu absorpcji (jeśli gracz ją ma, np. po zjedzeniu koxa)
            if (abs > 0) {
                context.drawText(client.textRenderer, "Absorpcja: §e💛 " + abs, currentX, currentY + 15, 0xFFFFFF, true);
            }
        }

        // 3. Punkty zbroi pancerza przeciwnika
        if (HudConfig.showArmor) {
            int armorPoints = target.getArmor();
            int textY = currentY + ((target.getAbsorptionAmount() > 0) ? 25 : 15);
            context.drawText(client.textRenderer, "Pancerz: §b🛡 " + armorPoints, currentX, textY, 0xFFFFFF, true);
        }
    }
}
