package com.micmod;

import com.micmod.item.MicrophoneItem;
import net.fabricmc.api.ModInitializer;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class MicMod implements ModInitializer {
    public static final String MOD_ID = "micmod";

    public static final Item MICROPHONE = new MicrophoneItem(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, new Identifier(MOD_ID, "microphone"), MICROPHONE);
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }
}
