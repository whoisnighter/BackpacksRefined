package com.backpacksrefined.client.platform;

import com.backpacksrefined.block.BackpackBlockEntity;
import com.backpacksrefined.client.BackpackBlockPose;
import com.backpacksrefined.client.BackpackRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if >=1.21.9 {
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
*///?}
//? if >=26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else if >=1.21.9 {
/*import net.minecraft.client.renderer.state.CameraRenderState;
*///?}

/**
 * Draws a placed backpack, so its lid can open. {@link BackpackBlockPose} works out the pose. From
 * 1.21.9 the pose is taken into a render state and the model is posed from it when drawn, later in the
 * frame; before, the shared model is posed and drawn at once. 26.x renamed the no-cull cutout render
 * type to {@code entityCutout}, and 26.3 draws the breaking cracks with a call of their own.
 */
//? if >=1.21.9 {
public final class BackpackBlockRenderer implements BlockEntityRenderer<BackpackBlockEntity, BackpackBlockRenderer.State> {
    private final BackpackModel model;

    public BackpackBlockRenderer(BlockEntityRendererProvider.Context context) {
        model = new BackpackModel(context.bakeLayer(BackpackRendering.BLOCK_LAYER));
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BackpackBlockEntity backpack, State state, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(backpack, state, partialTick, camera, breakProgress);
        state.pose.set(backpack, partialTick);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        state.pose.transform(poseStack);
        RenderType renderType = model.renderType(state.pose.texture);
        //? if >=26.3 {
        collector.submitModel(model, state, poseStack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (state.breakProgress != null) {
            collector.order(1).submitCrumblingOverlay(model, state, poseStack, renderType, state.lightCoords,
                    OverlayTexture.NO_OVERLAY, -1, state.breakProgress);
        }
        //?} else {
        /*collector.submitModel(model, state, poseStack, renderType, state.lightCoords, OverlayTexture.NO_OVERLAY, 0,
                state.breakProgress);
        *///?}
        poseStack.popPose();
    }

    public static final class State extends BlockEntityRenderState {
        final BackpackBlockPose pose = new BackpackBlockPose();
    }

    private static final class BackpackModel extends Model<State> {
        private final ModelPart base;
        private final ModelPart lid;

        BackpackModel(ModelPart root) {
            //? if >=26.1 {
            super(root, RenderTypes::entityCutout);
            //?} else {
            /*super(root, RenderTypes::entityCutoutNoCull);
            *///?}
            base = root.getChild("base");
            lid = base.getChild("lid");
        }

        @Override
        public void setupAnim(State state) {
            super.setupAnim(state);
            state.pose.apply(base, lid);
        }
    }
}
//?} else {
/*public final class BackpackBlockRenderer implements BlockEntityRenderer<BackpackBlockEntity> {
    private final ModelPart base;
    private final ModelPart lid;
    private final BackpackBlockPose pose = new BackpackBlockPose();

    public BackpackBlockRenderer(BlockEntityRendererProvider.Context context) {
        base = context.bakeLayer(BackpackRendering.BLOCK_LAYER).getChild("base");
        lid = base.getChild("lid");
    }

    @Override
    public void render(BackpackBlockEntity backpack, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay) {
        pose.set(backpack, partialTick);
        poseStack.pushPose();
        pose.transform(poseStack);
        pose.apply(base, lid);
        base.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(pose.texture)), light, overlay);
        poseStack.popPose();
    }
}
*///?}
