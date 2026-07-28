package com.micmod.client.audio;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;

public class AudioPlayback {

    /**
     * Toca a gravação "no lugar" do som do mob. Não passa pelo motor de áudio
     * (OpenAL) do próprio Minecraft — em vez disso, calcula volume por distância
     * e um pan estéreo simples com base na posição da entidade em relação à
     * câmera do jogador. Funciona bem localmente; não é sincronizado entre
     * jogadores em multiplayer (ver README).
     */
    public static void playForEntity(LivingEntity entity, byte[] wavData, float baseVolume, float pitch) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavData));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);

            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                double dist = mc.player.getPos().distanceTo(entity.getPos());
                float distanceFactor = (float) Math.max(0.0, 1.0 - (dist / 16.0));
                float volume = MathHelper.clamp(baseVolume * distanceFactor, 0f, 1f);

                if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                    FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = volume <= 0.0001f ? gain.getMinimum() : (float) (Math.log10(volume) * 20.0);
                    dB = MathHelper.clamp(dB, gain.getMinimum(), gain.getMaximum());
                    gain.setValue(dB);
                }

                if (clip.isControlSupported(FloatControl.Type.PAN)) {
                    Vec3d rel = entity.getPos().subtract(mc.player.getPos());
                    float yaw = (float) Math.toRadians(mc.player.getYaw());
                    double relX = rel.x * Math.cos(yaw) + rel.z * Math.sin(yaw);
                    float pan = MathHelper.clamp((float) (relX / 8.0), -1f, 1f);
                    FloatControl panControl = (FloatControl) clip.getControl(FloatControl.Type.PAN);
                    panControl.setValue(pan);
                }
            }

            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) clip.close();
            });
            clip.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Toca a gravação simples (usado na tela de preview do item). */
    public static Clip playPreview(byte[] wavData, Runnable onFinish) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavData));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                    if (onFinish != null) onFinish.run();
                }
            });
            clip.start();
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
