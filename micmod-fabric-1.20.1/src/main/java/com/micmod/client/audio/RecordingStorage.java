package com.micmod.client.audio;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Salva/lê os arquivos .wav gravados, um por entidade (UUID) + categoria
 * (ambient / hurt / death), em:
 *   .minecraft/config/micmod/recordings/<uuid>/<categoria>.wav
 */
public class RecordingStorage {

    private static Path baseDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("micmod").resolve("recordings");
    }

    public static Path fileFor(UUID entityId, String category) {
        return baseDir().resolve(entityId.toString()).resolve(category + ".wav");
    }

    public static boolean exists(UUID entityId, String category) {
        return Files.exists(fileFor(entityId, category));
    }

    public static void delete(UUID entityId, String category) {
        try {
            Files.deleteIfExists(fileFor(entityId, category));
        } catch (IOException ignored) {
        }
    }

    public static void save(UUID entityId, String category, byte[] wavData) throws IOException {
        Path path = fileFor(entityId, category);
        Files.createDirectories(path.getParent());
        Files.write(path, wavData);
    }

    public static byte[] load(UUID entityId, String category) throws IOException {
        return Files.readAllBytes(fileFor(entityId, category));
    }
}
