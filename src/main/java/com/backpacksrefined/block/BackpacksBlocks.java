package com.backpacksrefined.block;

import com.backpacksrefined.item.BackpackTier;
import com.backpacksrefined.platform.RegistryKeys;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The mod's blocks: a placed backpack for each tier. Each loader's mod class calls {@link #register} at
 * the point its loader takes new blocks, before the items, which need them.
 */
public final class BackpacksBlocks {
    public static final Identifier BACKPACK_ID = BackpackTier.LEATHER.id();

    /** Wool's sounds, except for the place sound, which is the backpack's own. */
    private static final SoundType BACKPACK_SOUND = new SoundType(1.0F, 1.0F,
            SoundEvents.WOOL_BREAK, SoundEvents.WOOL_STEP, BackpacksSounds.PLACE, SoundEvents.WOOL_HIT, SoundEvents.WOOL_FALL);

    private static List<Block> backpacks;

    private BackpacksBlocks() { }

    /** Builds every block and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, Block> registry) {
        List<Block> built = new ArrayList<>();
        for (BackpackTier tier : BackpackTier.values()) {
            Block block = new BackpackBlock(tier, RegistryKeys.block(tier.id(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BROWN)
                    .strength(0.8F)
                    .sound(BACKPACK_SOUND)
                    .noOcclusion()));
            registry.accept(tier.id(), block);
            built.add(block);
        }
        backpacks = List.copyOf(built);
    }

    /** The plain, leather backpack. */
    public static Block backpack() {
        return backpack(BackpackTier.LEATHER);
    }

    public static Block backpack(BackpackTier tier) {
        return all().get(tier.ordinal());
    }

    /** Every tier's block, smallest first. */
    public static List<Block> all() {
        if (backpacks == null) throw new IllegalStateException("Blocks are not registered yet");
        return backpacks;
    }
}
