package com.micmod.item;

import com.micmod.client.gui.RecordingScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class MicrophoneItem extends Item {

    public MicrophoneItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (hand == Hand.MAIN_HAND && user.getWorld().isClient) {
            openScreen(entity);
        }
        return ActionResult.SUCCESS;
    }

    @Environment(EnvType.CLIENT)
    private void openScreen(LivingEntity entity) {
        MinecraftClient.getInstance().setScreen(new RecordingScreen(entity));
    }
}
