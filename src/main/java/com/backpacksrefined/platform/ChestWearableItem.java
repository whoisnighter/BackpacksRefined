package com.backpacksrefined.platform;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
//? if >=1.21.2 {
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.Equippable;
//?} else {
/*import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
*///?}
//? if >=1.20.5 && <1.21.2 {
/*import net.minecraft.core.Holder;
*///?}

/**
 * An item worn in the chest slot, as a chestplate is: the armor slot takes it, and using it swaps it
 * with what is worn there. 1.21.2 made being worn a component of the item; before, the item
 * implemented {@code Equipable}.
 */
public class ChestWearableItem extends Item
        //? if <1.21.2 {
        /*implements Equipable
        *///?}
{
    public ChestWearableItem(Identifier id, Properties properties) {
        super(worn(id, properties));
    }

    private static Properties worn(Identifier id, Properties properties) {
        //? if >=1.21.2 {
        // From 1.21.2 an item knows its own id before it is registered.
        return properties
                .setId(ResourceKey.create(Registries.ITEM, id))
                .component(DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.CHEST)
                        .setEquipSound(SoundEvents.ARMOR_EQUIP_WOLF)
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

    // The wolf armor sound came in 1.20.5, as a holder.
    //? if >=1.20.5 && <1.21.2 {
    /*@Override
    public Holder<SoundEvent> getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_WOLF;
    }
    *///?}
    //? if <1.20.5 {
    /*@Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_LEATHER;
    }
    *///?}
}
