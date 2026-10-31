package com.backpacksrefined.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockBehaviour;
//? if >=1.21.2 {
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
//?}

/** From 1.21.2 a block knows its own id before it is registered; before, the registry told it. */
public final class RegistryKeys {
    private RegistryKeys() { }

    public static BlockBehaviour.Properties block(Identifier id, BlockBehaviour.Properties properties) {
        //? if >=1.21.2 {
        return properties.setId(ResourceKey.create(Registries.BLOCK, id));
        //?} else {
        /*return properties;
        *///?}
    }
}
