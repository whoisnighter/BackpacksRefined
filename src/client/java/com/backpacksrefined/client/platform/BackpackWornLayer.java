package com.backpacksrefined.client.platform;

import com.backpacksrefined.client.BackpackRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
//? if >=1.21.9 {
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
*///?}

/**
 * Draws the backpack on whoever wears one in the chest slot: players, armor stands and every mob drawn
 * with a humanoid model. From 1.21.9 a layer submits its model with the render state, and the model is
 * posed from that state when it is drawn; before, it copies the wearer's pose and draws at once.
 * Nodes between 1.21.2 and 1.21.9 would need a third form; there are none.
 */
//? if >=1.21.9 {
public final class BackpackWornLayer<S extends HumanoidRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    private final BackpackWornModel model;
//?} else {
/*public final class BackpackWornLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    private final BackpackWornModel<T> model;
*///?}

    //? if >=1.21.9 {
    public BackpackWornLayer(RenderLayerParent<S, M> parent, EntityModelSet models) {
        super(parent);
        model = new BackpackWornModel(models.bakeLayer(BackpackRendering.WORN_LAYER));
    }
    //?} else {
    /*public BackpackWornLayer(RenderLayerParent<T, M> parent, EntityModelSet models) {
        super(parent);
        model = new BackpackWornModel<>(models.bakeLayer(BackpackRendering.WORN_LAYER));
    }
    *///?}

    /**
     * The layer for {@code renderer}, or null when its model is not humanoid and so has no body to hang
     * a backpack on. Takes any renderer, since what a renderer's type parameters are changed in 1.21.2.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static RenderLayer<?, ?> create(Object renderer, EntityModelSet models) {
        //? if >=1.21.9 {
        if (renderer instanceof LivingEntityRenderer<?, ?, ?> living && living.getModel() instanceof HumanoidModel<?>) {
        //?} else {
        /*if (renderer instanceof LivingEntityRenderer<?, ?> living && living.getModel() instanceof HumanoidModel<?>) {
        *///?}
            return new BackpackWornLayer((RenderLayerParent) living, models);
        }
        return null;
    }

    //? if >=1.21.9 {
    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        if (!BackpackRendering.isBackpack(state.chestEquipment)) return;
        collector.submitModel(model, state, poseStack, RenderTypes.armorCutoutNoCull(BackpackRendering.wornTexture(state.chestEquipment)),
                //? if >=26.3 {
                light, OverlayTexture.NO_OVERLAY, state.outlineColor);
                //?} else {
                /*light, OverlayTexture.NO_OVERLAY, state.outlineColor, null);
                *///?}
    }
    //?} else {
    /*@Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity, float limbSwing,
            float limbSwingAmount, float partialTick, float ageInTicks, float headYaw, float headPitch) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (!BackpackRendering.isBackpack(chest)) return;
        getParentModel().copyPropertiesTo(model);
        model.body.render(poseStack, buffers.getBuffer(RenderType.armorCutoutNoCull(BackpackRendering.wornTexture(chest))),
                light, OverlayTexture.NO_OVERLAY);
    }
    *///?}
}
