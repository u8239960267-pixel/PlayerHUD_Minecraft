package pl.playerhud.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.playerhud.PlayerHudClient;

@Mixin(ClientPlayerEntity.class)
public class AttackEntityMixin {
    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void onAttackPlayer(Entity target, CallbackInfo ci) {
        if (target instanceof PlayerEntity) {
            // Zapisanie UUID uderzonego gracza i zresetowanie licznika do 15 sekund
            PlayerHudClient.targetPlayerUuid = target.getUuid();
            PlayerHudClient.lastAttackTime = System.currentTimeMillis();
        }
    }
}
