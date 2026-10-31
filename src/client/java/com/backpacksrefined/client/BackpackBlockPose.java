package com.backpacksrefined.client;

import com.backpacksrefined.block.BackpackBlock;
import com.backpacksrefined.block.BackpackBlockEntity;
import com.backpacksrefined.client.config.BackpacksConfig;
import com.backpacksrefined.item.BackpackTier;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * How a placed backpack is posed in one frame, worked out from its block entity: the lid, and the drop
 * when set down and the bobbing on water when the config asks for them. The same on every version; the
 * renderer in {@code platform/} takes it from here.
 */
public final class BackpackBlockPose {
    /** How far the lid turns when fully open, in radians. */
    private static final float LID_OPEN = 1.5F;
    /** How long the bobbing on water takes to come round, in ticks. */
    private static final float BOB_TICKS = 90.0F;
    /** How far a floating backpack sinks into the water, in pixels; its block model sinks it as far. */
    private static final float FLOATING_SINK = 3.0F;
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    public Identifier texture = BackpackRendering.blockTexture(BackpackTier.LEATHER);
    /** The facing's turn about y, in degrees. */
    public float facing;
    public boolean floating;
    /** The lid's turn, in radians. */
    public float lid;
    /** How far the backpack is raised, in pixels. */
    public float lift;
    public float tiltX;
    public float tiltZ;

    public void set(BackpackBlockEntity backpack, float partialTick) {
        BlockState state = backpack.getBlockState();
        if (state.getBlock() instanceof BackpackBlock block) texture = BackpackRendering.blockTexture(block.tier());
        facing = state.getValue(HorizontalDirectionalBlock.FACING).toYRot();
        floating = state.getValue(BackpackBlock.FLOATING);
        lid = backpack.lidOpenness(partialTick) * LID_OPEN;
        lift = 0.0F;
        tiltX = 0.0F;
        tiltZ = 0.0F;

        // Set down: it drops the last four pixels and rocks to a stop.
        float placed = BackpacksConfig.bounceWhenPlaced() ? backpack.ticksSincePlaced(partialTick) : -1.0F;
        if (placed >= 0.0F && placed < 4.0F) {
            float fall = placed / 4.0F;
            lift = 4.0F - fall * fall * 4.0F;
        }
        if (placed >= 0.0F && placed < 8.0F) {
            float rock = placed / 8.0F;
            tiltX = Mth.sin(rock * 10.0F) * 0.1F * (1.0F - rock);
            tiltZ = Mth.cos(rock * 10.0F) * 0.1F * (1.0F - rock);
        }

        // Floating: it rises and falls and leans with the water, each backpack at its own point of the swell.
        if (floating && BackpacksConfig.bobOnWater() && backpack.getLevel() != null) {
            BlockPos pos = backpack.getBlockPos();
            float ticks = (backpack.getLevel().getGameTime() % (long) BOB_TICKS) + partialTick + Math.floorMod(pos.getX() * 31 + pos.getZ() * 17, (int) BOB_TICKS);
            float angle = ticks / BOB_TICKS * TWO_PI;
            lift += Mth.sin(angle + 20.0F / BOB_TICKS * TWO_PI) * 0.75F;
            tiltX += Mth.sin(angle) * 0.02F;
            tiltZ += Mth.cos(angle) * 0.02F;
        }
    }

    /**
     * Moves {@code poseStack} from the block's corner to the model's origin, turned to the facing. The
     * model is built as entity models are, upside down and 24 pixels up.
     */
    public void transform(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // Not mulPose, which 26.3 renamed for a rotation: rotateAround is the same call on every version.
        poseStack.rotateAround(Axis.YP.rotationDegrees(-facing), 0.0F, 0.0F, 0.0F);
        poseStack.scale(1.0F, -1.0F, -1.0F);
        poseStack.translate(0.0F, -1.0F + (floating ? FLOATING_SINK / 16.0F : 0.0F), 0.0F);
    }

    /** Poses the model's base and lid; every value is set, so a shared model carries nothing over. */
    public void apply(ModelPart base, ModelPart lid) {
        base.y = 24.0F - lift;
        base.xRot = tiltX;
        base.zRot = tiltZ;
        lid.xRot = this.lid;
    }
}
