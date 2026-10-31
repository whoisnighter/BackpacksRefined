package com.backpacksrefined.block;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.platform.RegistryKeys;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.BiConsumer;

/**
 * The mod's blocks. Each loader's mod class calls {@link #register} at the point its loader takes new
 * blocks, before the items, which need them.
 */
public final class BackpacksBlocks {
    public static final Identifier BACKPACK_ID = BackpacksRefined.id("backpack");

    /** Wool's sounds, except for the place sound, which is the backpack's own. */
    private static final SoundType BACKPACK_SOUND = new SoundType(1.0F, 1.0F,
            SoundEvents.WOOL_BREAK, SoundEvents.WOOL_STEP, BackpacksSounds.PLACE, SoundEvents.WOOL_HIT, SoundEvents.WOOL_FALL);

    private static Block backpack;

    private BackpacksBlocks() { }

    /** Builds every block and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, Block> registry) {
        backpack = new BackpackBlock(RegistryKeys.block(BACKPACK_ID, BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BROWN)
                .strength(0.8F)
                .sound(BACKPACK_SOUND)
                .noOcclusion()));
        registry.accept(BACKPACK_ID, backpack);
    }

    public static Block backpack() {
        if (backpack == null) throw new IllegalStateException("Blocks are not registered yet");
        return backpack;
    }
}
