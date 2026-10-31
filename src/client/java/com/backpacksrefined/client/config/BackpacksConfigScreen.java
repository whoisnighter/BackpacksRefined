package com.backpacksrefined.client.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The config screen, built with Cloth Config, so it looks the same on every loader and version. It
 * names Cloth Config's classes, so call {@link #create} only once the loader says Cloth Config is
 * installed: {@code cloth-config} on Fabric, {@code cloth_config} on NeoForge and Forge. ModMenu opens
 * it on Fabric, the mod list's Config button on NeoForge and Forge.
 */
public final class BackpacksConfigScreen {
    private BackpacksConfigScreen() { }

    public static Screen create(Screen parent) {
        BackpacksConfig.Values values = BackpacksConfig.values();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.backpacksrefined.title"))
                // Drawn over the world in a game and over the menu background outside one, as
                // vanilla's own option screens are.
                .setTransparentBackground(true)
                .setSavingRunnable(BackpacksConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory animations = builder.getOrCreateCategory(Component.translatable("config.backpacksrefined.animations"));
        animations.addEntry(entries.startBooleanToggle(Component.translatable("config.backpacksrefined.bounce_when_placed"),
                        values.bounceWhenPlaced)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.backpacksrefined.bounce_when_placed.tooltip"))
                .setSaveConsumer(value -> values.bounceWhenPlaced = value)
                .build());
        animations.addEntry(entries.startBooleanToggle(Component.translatable("config.backpacksrefined.bob_on_water"),
                        values.bobOnWater)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.backpacksrefined.bob_on_water.tooltip"))
                .setSaveConsumer(value -> values.bobOnWater = value)
                .build());
        return builder.build();
    }
}
