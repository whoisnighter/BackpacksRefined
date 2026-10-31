package com.backpacksrefined.menu;

import com.backpacksrefined.item.BackpackTier;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The mod's menu types: one per tier, under the tier's id, so a placed backpack's screen is sized by
 * its type and the server sends nothing more than vanilla does. Each loader's mod class calls
 * {@link #register}, and each loader's client gives every type the backpack's screen.
 */
public final class BackpacksMenus {
    private static final Map<BackpackTier, MenuType<BackpackMenu>> MENUS = new EnumMap<>(BackpackTier.class);

    private BackpacksMenus() { }

    /** Builds every menu type and hands it to {@code registry} under its id. */
    public static void register(BiConsumer<Identifier, MenuType<?>> registry) {
        for (BackpackTier tier : BackpackTier.values()) {
            MenuType<BackpackMenu> type = new MenuType<>(
                    (containerId, inventory) -> BackpackMenu.client(containerId, inventory, tier), FeatureFlags.VANILLA_SET);
            MENUS.put(tier, type);
            registry.accept(tier.id(), type);
        }
    }

    public static MenuType<BackpackMenu> menu(BackpackTier tier) {
        MenuType<BackpackMenu> type = MENUS.get(tier);
        if (type == null) throw new IllegalStateException("Menus are not registered yet");
        return type;
    }

    public static Collection<MenuType<BackpackMenu>> all() {
        return Collections.unmodifiableCollection(MENUS.values());
    }
}
