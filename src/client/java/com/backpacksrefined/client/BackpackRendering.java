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

/**
 * What drawing a backpack needs, on every version: the model layers, textures and shapes of the worn
 * backpack and of the placed one.
 */
public final class BackpackRendering {
    public static final ModelLayerLocation WORN_LAYER = new ModelLayerLocation(BackpacksRefined.id("backpack"), "main");
    public static final ModelLayerLocation BLOCK_LAYER = new ModelLayerLocation(BackpacksRefined.id("backpack"), "block");
    private static final Identifier[] WORN_TEXTURES = new Identifier[BackpackTier.values().length];
    private static final Identifier[] BLOCK_TEXTURES = new Identifier[BackpackTier.values().length];

    static {
        for (BackpackTier tier : BackpackTier.values()) {
            WORN_TEXTURES[tier.ordinal()] = BackpacksRefined.id("textures/entity/" + tier.path() + ".png");
            BLOCK_TEXTURES[tier.ordinal()] = BackpacksRefined.id("textures/block/" + tier.path() + ".png");
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

    /** The placed backpack's texture for {@code tier}, the one its block model uses too. */
    public static Identifier blockTexture(BackpackTier tier) {
        return BLOCK_TEXTURES[tier.ordinal()];
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

    /**
     * The placed backpack: the old mod's block entity model, which {@code models/block/backpack.json}
     * copies, on the same 64x64 texture. The lid hangs from the back of the base's top, so turning it
     * on x opens it; its underside is a flat face, seen when it is open.
     */
    public static LayerDefinition createBlockLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition base = mesh.getRoot().addOrReplaceChild("base", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-5.0F, -11.0F, -4.0F, 10.0F, 11.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 24.0F, 0.0F));
        base.addOrReplaceChild("lid", CubeListBuilder.create()
                        .texOffs(0, 19).addBox(-5.5F, -2.0F, -0.5F, 11.0F, 5.0F, 9.0F, CubeDeformation.NONE)
                        .texOffs(-9, 33).addBox(-5.5F, 1.0F, -0.5F, 11.0F, 0.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -11.0F, -4.0F));
        return LayerDefinition.create(mesh, 64, 64);
    }
}
