package com.backpacksrefined.client.forge;

import com.backpacksrefined.client.BackpackRendering;
import com.backpacksrefined.client.BackpacksRefinedClient;
import com.backpacksrefined.client.platform.BackpackWornLayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/** The Forge client half, started by the mod class on a client only. */
public final class BackpacksRefinedForgeClient {
    private BackpacksRefinedForgeClient() { }

    public static void initialize(IEventBus modBus) {
        BackpacksRefinedClient.initialize();
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) ->
                event.registerLayerDefinition(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer));
        modBus.addListener(BackpacksRefinedForgeClient::addLayers);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        // Forge 47 has no list of the entity types it made renderers for, so every one is asked.
        for (EntityType type : BuiltInRegistries.ENTITY_TYPE) addLayer(event.getEntityRenderer(type), event);
        for (String skin : event.getSkins()) addLayer(event.getSkin(skin), event);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayer(Object renderer, EntityRenderersEvent.AddLayers event) {
        RenderLayer layer = BackpackWornLayer.create(renderer, event.getEntityModels());
        if (layer != null) ((LivingEntityRenderer) renderer).addLayer(layer);
    }
}
