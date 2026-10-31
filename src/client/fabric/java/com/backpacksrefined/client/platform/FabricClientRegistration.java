package com.backpacksrefined.client.platform;

import com.backpacksrefined.block.BackpacksBlockEntities;
import com.backpacksrefined.block.BackpacksBlocks;
import com.backpacksrefined.client.BackpackRendering;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
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

/**
 * Fabric API's client registries, which 26.1 renamed to Minecraft's own names. The placed backpack's
 * renderer goes straight into Minecraft's own table, which is what Fabric API's would do.
 */
public final class FabricClientRegistration {
    private FabricClientRegistration() { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register() {
        //? if >=26.1 {
        ModelLayerRegistry.registerModelLayer(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer);
        ModelLayerRegistry.registerModelLayer(BackpackRendering.BLOCK_LAYER, BackpackRendering::createBlockLayer);
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            RenderLayer layer = BackpackWornLayer.create(renderer, context.getModelSet());
            if (layer != null) helper.register(layer);
        });
        // From 26.1 a block's texture says whether it is cut out, for the item drawn with its model.
        //?} else {
        /*EntityModelLayerRegistry.registerModelLayer(BackpackRendering.WORN_LAYER, BackpackRendering::createWornLayer);
        EntityModelLayerRegistry.registerModelLayer(BackpackRendering.BLOCK_LAYER, BackpackRendering::createBlockLayer);
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, context) -> {
            RenderLayer layer = BackpackWornLayer.create(renderer, context.getModelSet());
            if (layer != null) helper.register(layer);
        });
        for (Block block : BackpacksBlocks.all()) BlockRenderLayerMap.putBlock(block, ChunkSectionLayer.CUTOUT);
        *///?}
        BlockEntityRenderers.register(BackpacksBlockEntities.backpack(), BackpackBlockRenderer::new);
    }
}
