package com.backpacksrefined.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
//? if >=1.21.2 {
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
//?} else {
/*import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
*///?}
//? if >=1.20.5 && <1.21.2 {
/*import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
*///?}

/**
 * A block item that is also worn in the chest slot, as a chestplate is: used on a block it places the
 * block, used in the air it swaps with what is worn, and the armor slot takes it. 1.21.2 made being
 * worn a component of the item; before, the item implemented {@code Equipable}.
 */
public class WearableBlockItem extends BlockItem
        //? if <1.21.2 {
        /*implements Equipable
        *///?}
{
    private final SoundEvent equipSound;

    public WearableBlockItem(Identifier id, Block block, SoundEvent equipSound, Properties properties) {
        super(block, worn(id, equipSound, properties));
        this.equipSound = equipSound;
    }

    private static Properties worn(Identifier id, SoundEvent equipSound, Properties properties) {
        //? if >=1.21.2 {
        // From 1.21.2 an item knows its own id before it is registered, and a block item is named
        // item.<id> unless told to keep the block's name, as it did before.
        // The equipment asset has no file, so the armor layer draws nothing for it. It is there because
        // a humanoid's render state only carries chest equipment that names an asset, and the mod's own
        // layer reads the item from there.
        return properties
                .setId(ResourceKey.create(Registries.ITEM, id))
                .useBlockDescriptionPrefix()
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                        .setEquipSound(Holder.direct(equipSound))
                        .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, id))
                        .build());
        //?} else {
        /*return properties;
        *///?}
    }

    //? if <1.21.2 {
    /*@Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.CHEST;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return swapWithEquipmentSlot(this, level, player, hand);
    }
    *///?}

    // The equip sound became a holder in 1.20.5.
    //? if >=1.20.5 && <1.21.2 {
    /*@Override
    public Holder<SoundEvent> getEquipSound() {
        return BuiltInRegistries.SOUND_EVENT.wrapAsHolder(equipSound);
    }
    *///?}
    //? if <1.20.5 {
    /*@Override
    public SoundEvent getEquipSound() {
        return equipSound;
    }
    *///?}
}
