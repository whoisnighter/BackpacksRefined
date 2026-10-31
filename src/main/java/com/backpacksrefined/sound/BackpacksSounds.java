package com.backpacksrefined.sound;

import com.backpacksrefined.BackpacksRefined;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.function.BiConsumer;

/** The mod's sound events. What each one plays is in {@code assets/backpacksrefined/sounds.json}. */
public final class BackpacksSounds {
    public static final Identifier EQUIP_ID = BackpacksRefined.id("item.backpack.equip");
    /** The upper tiers' equip sound: wolf armor's, or leather's before 1.20.5 (see the end of gradle/shared.gradle.kts). */
    public static final Identifier EQUIP_ARMORED_ID = BackpacksRefined.id("item.backpack.equip_armored");
    public static final Identifier PLACE_ID = BackpacksRefined.id("block.backpack.place");

    public static final SoundEvent EQUIP = SoundEvent.createVariableRangeEvent(EQUIP_ID);
    public static final SoundEvent EQUIP_ARMORED = SoundEvent.createVariableRangeEvent(EQUIP_ARMORED_ID);
    public static final SoundEvent PLACE = SoundEvent.createVariableRangeEvent(PLACE_ID);

    private BackpacksSounds() { }

    /** Hands every sound event to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, SoundEvent> registry) {
        registry.accept(EQUIP_ID, EQUIP);
        registry.accept(EQUIP_ARMORED_ID, EQUIP_ARMORED);
        registry.accept(PLACE_ID, PLACE);
    }
}
