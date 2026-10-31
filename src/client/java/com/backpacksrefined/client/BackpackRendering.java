package com.backpacksrefined.client;

import com.backpacksrefined.BackpacksRefined;
import com.backpacksrefined.item.BackpackItem;
import com.backpacksrefined.item.BackpackTier;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** What drawing a worn backpack needs, on every version: its model layer, its texture and its shape. */
public final class BackpackRendering {
    public static final ModelLayerLocation WORN_LAYER = new ModelLayerLocation(BackpacksRefined.id("backpack"), "main");
    private static final Identifier[] WORN_TEXTURES = new Identifier[BackpackTier.values().length];

    static {
        for (BackpackTier tier : BackpackTier.values()) {
            WORN_TEXTURES[tier.ordinal()] = BackpacksRefined.id("textures/entity/" + tier.path() + ".png");
        }
    }

    private BackpackRendering() { }

    public static boolean isBackpack(ItemStack stack) {
        return stack.getItem() instanceof BackpackItem;
    }

    /** The worn texture of {@code stack}'s tier; {@code stack} is a backpack. */
    public static Identifier wornTexture(ItemStack stack) {
        return WORN_TEXTURES[((BackpackItem) stack.getItem()).tier().ordinal()];
    }

    /**
     * A humanoid model with the backpack hung on its body, so that it bends and turns with the body of
     * whoever wears it. The humanoid's own parts are hidden by the model class; only the backpack draws.
     */
    public static LayerDefinition createWornLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition base = mesh.getRoot().getChild("body").addOrReplaceChild("base", CubeListBuilder.create()
                        // The straps, just outside the body.
                        .texOffs(40, 0).addBox(-7.0F, -10.0F, -4.5F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.6F))
                        .texOffs(0, 0).addBox(-7.0F, -9.0F, -1.0F, 8.0F, 9.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(3.0F, 10.0F, 2.5F));
        base.addOrReplaceChild("lid", CubeListBuilder.create()
                        .texOffs(0, 15).addBox(-4.5F, -1.0F, -2.0F, 9.0F, 4.0F, 7.0F, CubeDeformation.NONE)
                        .texOffs(32, 18).addBox(-4.5F, 0.0F, -2.0F, 9.0F, 1.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offset(-3.0F, -9.0F, 0.5F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
