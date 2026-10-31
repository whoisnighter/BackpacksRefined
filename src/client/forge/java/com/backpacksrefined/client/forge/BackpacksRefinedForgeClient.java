package com.backpacksrefined.client.forge;

import com.backpacksrefined.block.BackpacksBlockEntities;
import com.backpacksrefined.client.BackpackRendering;
import com.backpacksrefined.client.BackpacksRefinedClient;
import com.backpacksrefined.client.config.BackpacksConfigScreen;
import com.backpacksrefined.client.platform.BackpackBlockRenderer;
import com.backpacksrefined.client.platform.BackpackWornLayer;
import com.backpacksrefined.platform.Loader;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;

/** The Forge client half, started by the mod class on a client only. */
public final class BackpacksRefinedForgeClient {
    private BackpacksRefinedForgeClient() { }

    public static void initialize(IEventBus modBus) {
        BackpacksRefinedClient.initialize();
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer);
            event.registerLayerDefinition(BackpackRendering.BLOCK_LAYER, BackpackRendering::createBlockLayer);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(BackpacksBlockEntities.backpack(), BackpackBlockRenderer::new));
        modBus.addListener(BackpacksRefinedForgeClient::addLayers);
        // The mod list's Config button, when Cloth Config is there to draw the screen.
        if (Loader.isModLoaded("cloth_config")) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> BackpacksConfigScreen.create(parent)));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Forge 47 has no list of the entity types it made renderers for, so every living one is asked.
        // getRenderer casts to a living entity's renderer, so only types with attributes, which every
        // living entity has, are asked; getEntityRenderer, which does not cast, needs Forge 47.4.1.
        for (EntityType type : BuiltInRegistries.ENTITY_TYPE) {
            if (!DefaultAttributes.hasSupplier(type)) continue;
            try {
                addLayer(event.getRenderer(type), event);
            } catch (ClassCastException notLiving) {
                // A mod's type with attributes but a renderer of another kind.
            }
        }
        for (String skin : event.getSkins()) addLayer(event.getSkin(skin), event);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayer(Object renderer, EntityRenderersEvent.AddLayers event) {
        RenderLayer layer = BackpackWornLayer.create(renderer, event.getEntityModels());
        if (layer != null) ((LivingEntityRenderer) renderer).addLayer(layer);
    }
}
