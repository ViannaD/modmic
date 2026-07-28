package com.micmod.mixin;

import com.micmod.client.audio.EntitySoundOverride;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void micmod$onPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getWorld().isClient) {
            if (EntitySoundOverride.tryOverride(self, sound, volume, pitch)) {
                ci.cancel();
            }
        }
    }
}
