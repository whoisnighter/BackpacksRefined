package com.backpacksrefined.client.platform;

import com.backpacksrefined.block.BackpacksBlocks;
import com.backpacksrefined.client.BackpackRendering;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
//? if >=26.1 {
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
//?} else {
/*import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Block;
*///?}

/** Fabric API's client registries, which 26.1 renamed to Minecraft's own names. */
public final class FabricClientRegistration {
    private FabricClientRegistration() { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        //? if >=26.1 {
        ModelLayerRegistry.registerModelLayer(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            RenderLayer layer = BackpackWornLayer.create(renderer, context.getModelSet());
            if (layer != null) helper.register(layer);
        });
        // From 26.1 a block's texture says whether it is cut out.
        //?} else {
        /*EntityModelLayerRegistry.registerModelLayer(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            RenderLayer layer = BackpackWornLayer.create(renderer, context.getModelSet());
            if (layer != null) helper.register(layer);
        });
        for (Block block : BackpacksBlocks.all()) BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.CUTOUT);
        *///?}
    }
}
