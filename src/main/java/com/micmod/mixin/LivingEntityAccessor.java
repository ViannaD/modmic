package com.micmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Expõe o método protected getDeathSound() de LivingEntity como público.
 * getDeathSound() é declarado na própria LivingEntity (ao contrário de
 * getAmbientSound(), que só existe em MobEntity — ver MobEntityAccessor).
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Invoker("getDeathSound")
    SoundEvent micmod$getDeathSound();
}
