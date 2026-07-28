package com.micmod.mixin;

import com.micmod.client.audio.EntitySoundOverride;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * playSound(SoundEvent, float, float) é declarado em Entity, não em
 * LivingEntity (LivingEntity só herda o método). Por isso o mixin precisa
 * mirar Entity.class; filtramos para só agir em LivingEntity abaixo.
 */
@Mixin(Entity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "playSound(Lnet/minecraft/sound/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
    private void micmod$onPlaySound(SoundEvent sound, float volume, float pitch, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof LivingEntity living)) {
            return;
        }
        if (living.getWorld().isClient) {
            if (EntitySoundOverride.tryOverride(living, sound, volume, pitch)) {
                ci.cancel();
            }
        }
    }
}
