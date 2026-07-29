package com.micmod.client.audio;

import com.micmod.mixin.LivingEntityAccessor;
import com.micmod.mixin.MobEntityAccessor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.sound.SoundEvent;

import java.io.IOException;

/**
 * Usado pelo mixin em Entity/LivingEntity para decidir, sempre que o jogo
 * tentaria tocar um som vanilla daquela entidade, se existe uma gravação do
 * usuário para substituí-lo.
 *
 * Heurística: comparamos o SoundEvent com getAmbientSound() (só existe em
 * MobEntity, ex.: animais e monstros) e getDeathSound() (existe em toda
 * LivingEntity, incluindo jogadores). Se não bater com nenhum dos dois,
 * tratamos como "hurt" (é o outro som chamado com mais frequência por
 * playSound). Isso cobre bem os 3 casos da tela (Ambient/Hurt/Death), mas
 * pode eventualmente também capturar sons raros como splash/equip se você
 * tiver uma gravação "hurt" salva — limitação conhecida, ver README.
 */
public class EntitySoundOverride {

    public static String resolveCategory(LivingEntity entity, SoundEvent sound) {
        if (entity instanceof MobEntity && sound == ((MobEntityAccessor) entity).micmod$getAmbientSound()) {
            return "ambient";
        }
        LivingEntityAccessor accessor = (LivingEntityAccessor) entity;
        if (sound == accessor.micmod$getDeathSound()) return "death";
        return "hurt";
    }

    public static boolean tryOverride(LivingEntity entity, SoundEvent sound, float volume, float pitch) {
        String category = resolveCategory(entity, sound);
        if (!RecordingStorage.exists(entity.getUuid(), category)) return false;
        try {
            byte[] data = RecordingStorage.load(entity.getUuid(), category);
            AudioPlayback.playForEntity(entity, data, volume, pitch);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
