package com.micmod.client.gui;

import com.micmod.MicMod;
import com.micmod.client.audio.AudioPlayback;
import com.micmod.client.audio.AudioRecorder;
import com.micmod.client.audio.RecordingStorage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.IOException;

/**
 * Tela que abre ao clicar com o microfone (botão direito) numa entidade.
 * Layout em duas colunas: lista de categorias (com indicador de gravação)
 * à esquerda, preview 3D da entidade + controles de gravação à direita.
 *
 * Os botões de transporte (gravar/ouvir/apagar/mudo) são desenhados a partir
 * de texturas em assets/micmod/textures/gui/. Pra trocar o estilo visual,
 * basta substituir esses arquivos .png (32x32, fundo transparente) — não é
 * preciso mexer neste código.
 */
public class RecordingScreen extends Screen {

    private static final String[] CATEGORIES = {"ambient", "hurt", "death"};
    private static final String[] CATEGORY_LABELS = {"Ambient", "Hurt", "Death"};

    private static final int ICON_TEX_SIZE = 32;
    private static final Identifier ICON_RECORD = MicMod.id("textures/gui/icon_record.png");
    private static final Identifier ICON_RECORD_ACTIVE = MicMod.id("textures/gui/icon_record_active.png");
    private static final Identifier ICON_PLAY = MicMod.id("textures/gui/icon_play.png");
    private static final Identifier ICON_DELETE = MicMod.id("textures/gui/icon_delete.png");
    private static final Identifier ICON_MUTE_OFF = MicMod.id("textures/gui/icon_mute_off.png");
    private static final Identifier ICON_MUTE_ON = MicMod.id("textures/gui/icon_mute_on.png");

    private static final int COLOR_PANEL_BG = 0xFFC6C6C6;
    private static final int COLOR_PANEL_BORDER = 0xFF373737;
    private static final int COLOR_ROW_IDLE = 0xFF8B8B8B;
    private static final int COLOR_ROW_HOVER = 0xFFA0A0A0;
    private static final int COLOR_ROW_SELECTED = 0xFF6B6B6B;
    private static final int COLOR_DOT_RECORDED = 0xFF55CC55;
    private static final int COLOR_DOT_EMPTY = 0xFF5A5A5A;
    private static final int COLOR_PREVIEW_BG = 0xFF0A0A0A;

    private final LivingEntity entity;
    private String selectedCategory = "ambient";

    private final AudioRecorder recorder = new AudioRecorder();
    private byte[] pendingRecording = null;
    private Clip previewClip = null;
    private boolean muted = false;

    private ButtonWidget saveButton;

    private int panelX, panelY, panelW, panelH;
    private final int[] rowBounds = new int[CATEGORIES.length];
    private int listX, listW, rowH;
    private int previewX, previewY, previewW, previewH;
    private int barY, barH;
    private int[] recordIcon;
    private int[] playIcon;
    private int[] deleteIcon;
    private int[] muteIcon;

    public RecordingScreen(LivingEntity entity) {
        super(Text.literal("Mic Voicenator"));
        this.entity = entity;
    }

    @Override
    protected void init() {
        panelW = 250;
        panelH = 216;
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        listX = panelX + 10;
        listW = 130;
        rowH = 22;
        int listY = panelY + 32;
        for (int i = 0; i < CATEGORIES.length; i++) {
            rowBounds[i] = listY + i * (rowH + 2);
        }

        previewX = panelX + 150;
        previewW = panelW - 160;
        previewY = panelY + 30;
        previewH = 78;

        barY = previewY + previewH + 6;
        barH = 8;

        int iconSize = 20;
        int iconY = barY + barH + 22; // espaço livre abaixo do texto de tempo
        int gap = 8;
        recordIcon = new int[]{previewX, iconY, iconSize};
        playIcon = new int[]{previewX + (iconSize + gap), iconY, iconSize};
        deleteIcon = new int[]{previewX + (iconSize + gap) * 2, iconY, iconSize};
        muteIcon = new int[]{panelX + panelW - 10 - iconSize, iconY, iconSize};

        saveButton = ButtonWidget.builder(Text.literal("Salvar"), b -> saveRecording())
                .dimensions(panelX + 10, panelY + panelH - 24, panelW - 20, 18)
                .build();
        addDrawableChild(saveButton);
    }

    private void selectCategory(String cat) {
        if (recorder.isRecording()) return;
        stopPreview();
        pendingRecording = null;
        selectedCategory = cat;
    }

    private void toggleRecord() {
        if (recorder.isRecording()) {
            pendingRecording = recorder.stop();
        } else {
            stopPreview();
            try {
                recorder.start();
            } catch (RuntimeException ignored) {
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
            applyMuteToPreview();
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

    private void toggleMute() {
        muted = !muted;
        applyMuteToPreview();
    }

    private void applyMuteToPreview() {
        if (previewClip != null && previewClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            FloatControl gain = (FloatControl) previewClip.getControl(FloatControl.Type.MASTER_GAIN);
            gain.setValue(muted ? gain.getMinimum() : 0f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        context.fill(panelX - 1, panelY - 1, panelX + panelW + 1, panelY + panelH + 1, COLOR_PANEL_BORDER);
        context.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL_BG);

        context.drawText(this.textRenderer, Text.literal("Drift's Mob Voicenator"), panelX + 10, panelY + 8, 0x404040, false);

        renderCategoryList(context, mouseX, mouseY);
        renderPreviewPanel(context, mouseX, mouseY, delta);
        renderTransportControls(context, mouseX, mouseY);

        super.render(context, mouseX, mouseY, delta);
    }

    private void renderCategoryList(DrawContext context, int mouseX, int mouseY) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            String cat = CATEGORIES[i];
            int y = rowBounds[i];
            boolean hovered = isInside(listX, y, listW, rowH, mouseX, mouseY);
            boolean selected = cat.equals(selectedCategory);
            int bg = selected ? COLOR_ROW_SELECTED : hovered ? COLOR_ROW_HOVER : COLOR_ROW_IDLE;
            context.fill(listX, y, listX + listW, y + rowH, bg);
            context.drawBorder(listX, y, listW, rowH, COLOR_PANEL_BORDER);
            context.drawText(this.textRenderer, Text.literal(CATEGORY_LABELS[i]), listX + 8, y + (rowH - 8) / 2, 0xFFFFFF, false);

            int dotSize = 6;
            int dotX = listX + listW - dotSize - 6;
            int dotY = y + (rowH - dotSize) / 2;
            context.fill(dotX, dotY, dotX + dotSize, dotY + dotSize, selected ? COLOR_DOT_RECORDED : COLOR_DOT_EMPTY);
        }
    }

    private void renderPreviewPanel(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawText(this.textRenderer, entity.getDisplayName(), previewX, panelY + 10, 0x202020, false);

        context.fill(previewX, previewY, previewX + previewW, previewY + previewH, COLOR_PREVIEW_BG);
        context.drawBorder(previewX, previewY, previewW, previewH, COLOR_PANEL_BORDER);

        int entX = previewX + previewW / 2;
        int entY = previewY + previewH - 10;
        int scale = (int) (previewH * 0.45);
        InventoryScreen.drawEntity(context, entX, entY, scale,
                (float) (entX - mouseX), (float) (entX - 50 - mouseY), entity);

        int barX = previewX;
        int barW = previewW;
        context.fill(barX, barY, barX + barW, barY + barH, 0xFF2B2B2B);
        double elapsed = recorder.isRecording() ? recorder.getElapsedSeconds() : 0.0;
        double frac = Math.min(1.0, elapsed / AudioRecorder.MAX_SECONDS);
        int segments = 24;
        int filled = (int) Math.round(segments * frac);
        int segW = barW / segments;
        for (int i = 0; i < segments; i++) {
            int x = barX + i * segW;
            int color = i < filled ? 0xFF55CC55 : 0xFF454545;
            context.fill(x + 1, barY + 1, x + segW - 1, barY + barH - 1, color);
        }

        String timeText = String.format("%.1f / %.1fs", elapsed, (double) AudioRecorder.MAX_SECONDS);
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(timeText), previewX + previewW / 2, barY + barH + 4, 0xAAAAAA);
    }

    private void renderTransportControls(DrawContext context, int mouseX, int mouseY) {
        boolean recording = recorder.isRecording();
        drawIconButton(context, recordIcon, recording ? ICON_RECORD_ACTIVE : ICON_RECORD, mouseX, mouseY);
        drawIconButton(context, playIcon, ICON_PLAY, mouseX, mouseY);
        drawIconButton(context, deleteIcon, ICON_DELETE, mouseX, mouseY);
        drawIconButton(context, muteIcon, muted ? ICON_MUTE_ON : ICON_MUTE_OFF, mouseX, mouseY);

        boolean hasSaved = RecordingStorage.exists(entity.getUuid(), selectedCategory);
        boolean hasPending = pendingRecording != null;
        String status = hasPending ? "\u00a7eGravação pronta (não salva)"
                : hasSaved ? "\u00a7aGravação salva" : "\u00a77Sem gravação";
        context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status), panelX + panelW / 2,
                muteIcon[1] + muteIcon[2] + 8, 0xFFFFFF);
    }

    /** Desenha um botão-ícone a partir de uma textura. Troque os .png em
     *  assets/micmod/textures/gui/ pra reestilizar sem tocar em código. */
    private void drawIconButton(DrawContext context, int[] bounds, Identifier texture, int mouseX, int mouseY) {
        int x = bounds[0], y = bounds[1], size = bounds[2];
        boolean hovered = isInside(x, y, size, size, mouseX, mouseY);
        if (hovered) {
            fillCircle(context, x + size / 2, y + size / 2, size / 2 + 2, 0x40FFFFFF);
        }
        context.drawTexture(texture, x, y, 0f, 0f, size, size, ICON_TEX_SIZE, ICON_TEX_SIZE);
    }

    private static void fillCircle(DrawContext context, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.sqrt(Math.max(0, radius * radius - dy * dy));
            context.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    private static boolean isInside(int x, int y, int w, int h, int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < CATEGORIES.length; i++) {
                if (isInside(listX, rowBounds[i], listW, rowH, (int) mouseX, (int) mouseY)) {
                    selectCategory(CATEGORIES[i]);
                    return true;
                }
            }
            if (isInside(recordIcon[0], recordIcon[1], recordIcon[2], recordIcon[2], (int) mouseX, (int) mouseY)) {
                toggleRecord();
                return true;
            }
            if (isInside(playIcon[0], playIcon[1], playIcon[2], playIcon[2], (int) mouseX, (int) mouseY)) {
                playPending();
                return true;
            }
            if (isInside(deleteIcon[0], deleteIcon[1], deleteIcon[2], deleteIcon[2], (int) mouseX, (int) mouseY)) {
                deleteRecording();
                return true;
            }
            if (isInside(muteIcon[0], muteIcon[1], muteIcon[2], muteIcon[2], (int) mouseX, (int) mouseY)) {
                toggleMute();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
        if (recorder.isRecording() && recorder.getElapsedSeconds() >= AudioRecorder.MAX_SECONDS) {
            pendingRecording = recorder.stop();
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
