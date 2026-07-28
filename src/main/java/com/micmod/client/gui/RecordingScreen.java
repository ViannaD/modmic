package com.micmod.client.gui;

import com.micmod.client.audio.AudioPlayback;
import com.micmod.client.audio.AudioRecorder;
import com.micmod.client.audio.RecordingStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;

import javax.sound.sampled.Clip;
import java.io.IOException;

public class RecordingScreen extends Screen {

    private static final String[] CATEGORIES = {"ambient", "hurt", "death"};
    private static final String[] CATEGORY_LABELS = {"Ambient", "Hurt", "Death"};

    private final LivingEntity entity;
    private String selectedCategory = "ambient";

    private final AudioRecorder recorder = new AudioRecorder();
    private byte[] pendingRecording = null;
    private Clip previewClip = null;

    private ButtonWidget[] categoryButtons;
    private ButtonWidget recordButton;
    private ButtonWidget playButton;
    private ButtonWidget deleteButton;
    private ButtonWidget saveButton;

    public RecordingScreen(LivingEntity entity) {
        super(Text.literal("Mic Voicenator"));
        this.entity = entity;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 100;
        int top = 40;

        categoryButtons = new ButtonWidget[CATEGORIES.length];
        for (int i = 0; i < CATEGORIES.length; i++) {
            final String cat = CATEGORIES[i];
            ButtonWidget btn = ButtonWidget.builder(labelFor(cat), b -> selectCategory(cat))
                    .dimensions(left, top + i * 24, 200, 20)
                    .build();
            categoryButtons[i] = btn;
            addDrawableChild(btn);
        }

        int row2 = top + CATEGORIES.length * 24 + 30;
        recordButton = ButtonWidget.builder(Text.literal("\u25CF Gravar"), b -> toggleRecord())
                .dimensions(left, row2, 64, 20).build();
        playButton = ButtonWidget.builder(Text.literal("\u25B6 Ouvir"), b -> playPending())
                .dimensions(left + 68, row2, 64, 20).build();
        deleteButton = ButtonWidget.builder(Text.literal("\uD83D\uDDD1 Apagar"), b -> deleteRecording())
                .dimensions(left + 136, row2, 64, 20).build();

        saveButton = ButtonWidget.builder(Text.literal("Salvar"), b -> saveRecording())
                .dimensions(left, row2 + 30, 200, 20).build();

        addDrawableChild(recordButton);
        addDrawableChild(playButton);
        addDrawableChild(deleteButton);
        addDrawableChild(saveButton);
    }

    private Text labelFor(String cat) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (CATEGORIES[i].equals(cat)) {
                String prefix = cat.equals(selectedCategory) ? "> " : "";
                return Text.literal(prefix + CATEGORY_LABELS[i]);
            }
        }
        return Text.literal(cat);
    }

    private void refreshCategoryLabels() {
        for (int i = 0; i < CATEGORIES.length; i++) {
            categoryButtons[i].setMessage(labelFor(CATEGORIES[i]));
        }
    }

    private void selectCategory(String cat) {
        if (recorder.isRecording()) return;
        stopPreview();
        pendingRecording = null;
        selectedCategory = cat;
        refreshCategoryLabels();
    }

    private void toggleRecord() {
        if (recorder.isRecording()) {
            pendingRecording = recorder.stop();
            recordButton.setMessage(Text.literal("\u25CF Gravar"));
        } else {
            stopPreview();
            try {
                recorder.start();
                recordButton.setMessage(Text.literal("\u25A0 Parar"));
            } catch (RuntimeException e) {
                recordButton.setMessage(Text.literal("Erro no mic!"));
            }
        }
    }

    private void playPending() {
        byte[] data = pendingRecording;
        if (data == null && RecordingStorage.exists(entity.getUuid(), selectedCategory)) {
            try {
                data = RecordingStorage.load(entity.getUuid(), selectedCategory);
            } catch (IOException ignored) {
            }
        }
        if (data != null) {
            stopPreview();
            previewClip = AudioPlayback.playPreview(data, () -> previewClip = null);
        }
    }

    private void deleteRecording() {
        RecordingStorage.delete(entity.getUuid(), selectedCategory);
        pendingRecording = null;
    }

    private void saveRecording() {
        if (pendingRecording != null) {
            try {
                RecordingStorage.save(entity.getUuid(), selectedCategory, pendingRecording);
                pendingRecording = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopPreview() {
        if (previewClip != null) {
            previewClip.close();
            previewClip = null;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, entity.getDisplayName(), this.width / 2, 15, 0xFFFFFF);

        double elapsed = recorder.isRecording() ? recorder.getElapsedSeconds() : 0.0;
        String timeText = String.format("%.1f / %.1fs", elapsed, (double) AudioRecorder.MAX_SECONDS);
        int timeY = 40 + CATEGORIES.length * 24 + 12;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(timeText), this.width / 2, timeY, 0xAAAAAA);

        boolean hasSaved = RecordingStorage.exists(entity.getUuid(), selectedCategory);
        boolean hasPending = pendingRecording != null;
        String status = hasPending ? "\u00a7eGravação pronta (não salva)"
                : hasSaved ? "\u00a7aGravação salva" : "\u00a77Sem gravação";
        int statusY = 40 + CATEGORIES.length * 24 + 12 + 12;
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), this.width / 2, statusY, 0xFFFFFF);
    }

    @Override
    public void tick() {
        super.tick();
        if (recorder.isRecording() && recorder.getElapsedSeconds() >= AudioRecorder.MAX_SECONDS) {
            pendingRecording = recorder.stop();
            recordButton.setMessage(Text.literal("\u25CF Gravar"));
        }
    }

    @Override
    public void close() {
        if (recorder.isRecording()) {
            recorder.stop();
        }
        stopPreview();
        super.close();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
