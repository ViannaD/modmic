package com.micmod.mixin;

import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Expõe o método protected getAmbientSound() de MobEntity como público.
 * Esse método NÃO existe em LivingEntity — só em MobEntity e suas
 * subclasses (animais, monstros, etc). PlayerEntity, por exemplo, é
 * LivingEntity mas não é MobEntity, então não tem som ambiente vanilla.
 */
@Mixin(MobEntity.class)
public interface MobEntityAccessor {

    @Invoker("getAmbientSound")
    SoundEvent micmod$getAmbientSound();
}
