package com.backpacksrefined.client.platform;

import com.backpacksrefined.block.BackpacksBlockEntities;
import com.backpacksrefined.client.BackpackRendering;
import com.backpacksrefined.menu.BackpacksMenus;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** NeoForge's client registry events; where the player renderers are found changed in 1.21.9. */
public final class NeoForgeClientRegistration {
    private NeoForgeClientRegistration() { }

    public static void register(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterLayerDefinitions event) -> {
            event.registerLayerDefinition(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer);
            event.registerLayerDefinition(BackpackRendering.BLOCK_LAYER, BackpackRendering::createBlockLayer);
        });
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(BackpacksBlockEntities.backpack(), BackpackBlockRenderer::new));
        modBus.addListener(NeoForgeClientRegistration::addLayers);
        modBus.addListener((RegisterMenuScreensEvent event) ->
                BackpacksMenus.all().forEach(type -> event.register(type, BackpackScreen::new)));
    }

    private static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> type : event.getEntityTypes()) addLayer(event.getRenderer(type), event);
        for (var skin : event.getSkins()) {
            //? if >=1.21.9 {
            addLayer(event.getPlayerRenderer(skin), event);
            //?} else {
            /*addLayer(event.getSkin(skin), event);
            *///?}
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void addLayer(Object renderer, EntityRenderersEvent.AddLayers event) {
        RenderLayer layer = BackpackWornLayer.create(renderer, event.getEntityModels());
        if (layer != null) ((LivingEntityRenderer) renderer).addLayer(layer);
    }
}
