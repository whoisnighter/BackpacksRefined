package com.backpacksrefined.client.neoforge;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.client.BackpacksRefinedClient;
import com.backpacksrefined.client.config.BackpacksConfigScreen;
import com.backpacksrefined.client.platform.NeoForgeClientRegistration;
import com.backpacksrefined.platform.Loader;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * The NeoForge client mod class, the counterpart of the Fabric {@code client} entrypoint. NeoForge only
 * constructs it on a client, which is what keeps the client sources off a dedicated server here.
 */
@Mod(value = BackpacksRefined.MOD_ID, dist = Dist.CLIENT)
public final class BackpacksRefinedNeoForgeClient {
    public BackpacksRefinedNeoForgeClient(ModContainer container) {
        BackpacksRefinedClient.initialize();
        NeoForgeClientRegistration.register(container.getEventBus());
        // The mod list's Config button, when Cloth Config is there to draw the screen.
        if (Loader.isModLoaded("cloth_config")) {
            container.registerExtensionPoint(IConfigScreenFactory.class,
                    (IConfigScreenFactory) (mod, parent) -> BackpacksConfigScreen.create(parent));
        }
    }
}
