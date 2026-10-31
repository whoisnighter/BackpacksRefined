package com.backpacksrefined.client.fabric;

import com.backpacksrefined.client.config.BackpacksConfigScreen;
import com.backpacksrefined.platform.Loader;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** ModMenu's {@code modmenu} entrypoint: the config screen, when Cloth Config is there to draw it. */
public final class BackpacksRefinedModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        if (!Loader.isModLoaded("cloth-config")) return parent -> null;
        return BackpacksConfigScreen::create;
    }
}
