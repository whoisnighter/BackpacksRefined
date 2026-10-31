package com.backpacksrefined.mixin;

import com.backpacksrefined.item.BackpackCrafting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.20.5 {
import net.minecraft.world.item.crafting.CraftingInput;
//?} else {
/*import net.minecraft.core.RegistryAccess;
import net.minecraft.world.inventory.CraftingContainer;
*///?}

/**
 * Hands what a shaped recipe made to {@code BackpackCrafting}, so a backpack crafted from another keeps
 * its contents. Vanilla's shaped recipe copies nothing from its ingredients.
 *
 * <p>Where versions differ: {@code assemble} takes the crafting input alone from 26.1, the input and
 * the registries from 1.20.5, and the crafting container and the registries before. The bridge method
 * over {@code RecipeInput} (over {@code Container} before) is left alone.
 */
@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
    //? if >=26.1 {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void backpacksrefined$carryOver(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(BackpackCrafting.carryOver(input.items(), cir.getReturnValue()));
    }
    //?} else if >=1.20.5 {
    /*@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void backpacksrefined$carryOver(CraftingInput input, net.minecraft.core.HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(BackpackCrafting.carryOver(input.items(), cir.getReturnValue()));
    }
    *///?} else {
    /*@Inject(method = "assemble(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"), cancellable = true)
    private void backpacksrefined$carryOver(CraftingContainer input, RegistryAccess registries, CallbackInfoReturnable<ItemStack> cir) {
        cir.setReturnValue(BackpackCrafting.carryOver(input.getItems(), cir.getReturnValue()));
    }
    *///?}
}
