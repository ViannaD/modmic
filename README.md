package com.micmod.client.audio;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Captura áudio do microfone real do sistema (TargetDataLine) e devolve
 * um .wav pronto para salvar.
 */
public class AudioRecorder {

    public static final float SAMPLE_RATE = 44100f;
    public static final int MAX_SECONDS = 10;

    private TargetDataLine line;
    private ByteArrayOutputStream buffer;
    private Thread recordThread;
    private volatile boolean recording = false;
    private long startTime;

    public boolean isRecording() {
        return recording;
    }

    public double getElapsedSeconds() {
        return recording ? (System.currentTimeMillis() - startTime) / 1000.0 : 0.0;
    }

    private AudioFormat format() {
        // 16-bit PCM mono, formato clássico de microfone
        return new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
    }

    public void start() {
        if (recording) return;
        try {
            AudioFormat format = format();
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if (!AudioSystem.isLineSupported(info)) {
                throw new LineUnavailableException("Nenhum microfone disponível");
            }
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();

            buffer = new ByteArrayOutputStream();
            recording = true;
            startTime = System.currentTimeMillis();

            recordThread = new Thread(() -> {
                byte[] chunk = new byte[4096];
                while (recording) {
                    int n = line.read(chunk, 0, chunk.length);
                    if (n > 0) buffer.write(chunk, 0, n);
                    if (System.currentTimeMillis() - startTime >= MAX_SECONDS * 1000L) {
                        recording = false;
                    }
                }
            }, "micmod-recorder-thread");
            recordThread.setDaemon(true);
            recordThread.start();
        } catch (LineUnavailableException e) {
            recording = false;
            throw new RuntimeException("Não consegui acessar o microfone: " + e.getMessage(), e);
        }
    }

    /** Para a gravação e retorna os bytes de um arquivo .wav válido. */
    public byte[] stop() {
        if (!recording && buffer == null) return new byte[0];
        recording = false;
        if (line != null) {
            line.stop();
            line.close();
        }
        try {
            if (recordThread != null) recordThread.join(300);
        } catch (InterruptedException ignored) {
        }
        byte[] pcm = buffer != null ? buffer.toByteArray() : new byte[0];
        buffer = null;
        return wrapAsWav(pcm);
    }

    private byte[] wrapAsWav(byte[] pcmData) {
        AudioFormat format = format();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (AudioInputStream ais = new AudioInputStream(
                    new ByteArrayInputStream(pcmData), format, pcmData.length / format.getFrameSize())) {
                AudioSystem.write(ais, AudioFileFormat.Type.WAVE, out);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
