package com.backpacksrefined.client.mixin;

import com.backpacksrefined.client.BackpackRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
//?} else {
/*import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
*///?}

/**
 * Skips a player's cape while they wear a backpack, which the cape would hang through, as the old mod
 * did. From 1.21.9 the layer submits from the render state; before, it drew from the player at once.
 * Nodes between 1.21.2 and 1.21.9 would need a third form; there are none.
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerMixin {
    //? if >=1.21.9 {
    @Inject(method = "submit", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$hideUnderBackpack(PoseStack poseStack, SubmitNodeCollector collector, int light,
            AvatarRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (BackpackRendering.isBackpack(state.chestEquipment)) ci.cancel();
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$hideUnderBackpack(PoseStack poseStack, MultiBufferSource buffers, int light,
            AbstractClientPlayer player, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float headYaw, float headPitch, CallbackInfo ci) {
        if (BackpackRendering.isBackpack(player.getItemBySlot(EquipmentSlot.CHEST))) ci.cancel();
    }
    *///?}
}
