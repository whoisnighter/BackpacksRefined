package com.backpacksrefined.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What one backpack holds: a fixed number of slots, kept in {@link BackpackStorage} under the backpack's
 * id. Every change marks the storage to be saved with the world.
 */
public final class BackpackContents extends SimpleContainer {
    /** One filled slot, as it is saved. */
    private record SlotItem(int slot, ItemStack item) {
        static final Codec<SlotItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.fieldOf("slot").forGetter(SlotItem::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(SlotItem::item)
        ).apply(instance, SlotItem::new));
    }

    static final Codec<BackpackContents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("size").forGetter(BackpackContents::getContainerSize),
            SlotItem.CODEC.listOf().fieldOf("items").forGetter(BackpackContents::filled)
    ).apply(instance, BackpackContents::new));

    private Runnable onChange = () -> { };

    public BackpackContents(int size) {
        super(size);
    }

    private BackpackContents(int size, List<SlotItem> items) {
        super(size);
        for (SlotItem item : items) {
            if (item.slot() >= 0 && item.slot() < size) setItem(item.slot(), item.item());
        }
    }

    /** The same items in a container of {@code size} slots, which is never smaller. */
    BackpackContents grow(int size) {
        BackpackContents grown = new BackpackContents(size, filled());
        grown.onChange = onChange;
        return grown;
    }

    void onChange(Runnable onChange) {
        this.onChange = onChange;
    }

    private List<SlotItem> filled() {
        List<SlotItem> filled = new ArrayList<>();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) filled.add(new SlotItem(slot, stack));
        }
        return filled;
    }

    /** A backpack holds nothing that refuses to go inside another item: backpacks, shulker boxes. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem().canFitInsideContainerItems();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        onChange.run();
    }
}
