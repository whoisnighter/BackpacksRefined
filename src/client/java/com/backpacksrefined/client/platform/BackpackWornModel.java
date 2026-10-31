package com.backpacksrefined.client.platform;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
//? if >=1.21.9 {
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
//?} else {
/*import net.minecraft.world.entity.LivingEntity;
*///?}

/**
 * The worn backpack, built on a humanoid model so it is posed as a body is: from the render state, or
 * copied from the wearer's model before render states. Every humanoid part but the backpack under the
 * body is hidden. The split is at 1.21.9 to match {@link BackpackWornLayer}.
 */
//? if >=1.21.9 {
public final class BackpackWornModel extends HumanoidModel<HumanoidRenderState> {
//?} else {
/*public final class BackpackWornModel<T extends LivingEntity> extends HumanoidModel<T> {
*///?}
    public BackpackWornModel(ModelPart root) {
        super(root);
        head.visible = false;
        hat.visible = false;
        rightArm.visible = false;
        leftArm.visible = false;
        rightLeg.visible = false;
        leftLeg.visible = false;
        // The body's own cubes stay undrawn; its children, the backpack, still draw.
        body.skipDraw = true;
    }
}
