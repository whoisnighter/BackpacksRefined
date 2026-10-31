package com.backpacksrefined.item;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.platform.FlatIcon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The mod's own creative tab, with the backpack in it. Its icon is the backpack's flat sprite, though
 * the backpack itself is drawn in 3D in every inventory: see {@link FlatIcon}.
 *
 * <p>Each loader starts a tab differently, so its mod class hands in the builder: Fabric API's on
 * Fabric, {@code CreativeModeTab.builder()} on NeoForge and Forge.
 */
public final class BackpacksCreativeTabs {
    public static final Identifier BACKPACKS_ID = BackpacksRefined.id("backpacks");

    private BackpacksCreativeTabs() { }

    /** Builds every tab and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, CreativeModeTab> registry, Supplier<CreativeModeTab.Builder> builder) {
        registry.accept(BACKPACKS_ID, builder.get()
                .title(Component.translatable("itemGroup." + BackpacksRefined.MOD_ID))
                .icon(() -> FlatIcon.of(new ItemStack(BackpacksItems.backpack())))
                .displayItems((parameters, output) -> output.accept(BackpacksItems.backpack()))
                .build());
    }
}
