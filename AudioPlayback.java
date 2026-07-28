package com.micmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Expõe os métodos protected getAmbientSound()/getDeathSound() de LivingEntity
 * como públicos, para que EntitySoundOverride possa compará-los sem precisar
 * de reflection.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("getAmbientSound")
    SoundEvent micmod$getAmbientSound();

    @Invoker("getDeathSound")
    SoundEvent micmod$getDeathSound();
}
