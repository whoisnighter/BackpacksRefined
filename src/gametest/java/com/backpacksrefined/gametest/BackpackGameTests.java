package com.backpacksrefined.gametest;

import com.backpacksrefined.block.BackpackBlock;
import com.backpacksrefined.block.BackpackBlockEntity;
import com.backpacksrefined.block.BackpacksBlocks;
import com.backpacksrefined.item.BackpackCrafting;
import com.backpacksrefined.item.BackpackItem;
import com.backpacksrefined.item.BackpackTier;
import com.backpacksrefined.item.BackpacksItems;
import com.backpacksrefined.network.BackpackNetwork.PanelClick;
import com.backpacksrefined.network.BackpackNetwork.PanelOpen;
import com.backpacksrefined.network.BackpackNetwork.PanelStore;
import com.backpacksrefined.panel.PanelServer;
import com.backpacksrefined.platform.BackpackIds;
import com.backpacksrefined.storage.BackpackContents;
import com.backpacksrefined.storage.BackpackStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;
import java.util.UUID;

/**
 * The mod's game tests: what a placed or worn backpack does on the server, checked in a few ticks on a
 * headless server with nobody watching. Each loader runs them through its own registration
 * ({@code src/gametest/<loader>/}), which calls one method here per test; nothing here names a loader.
 *
 * <p>What only a client shows (the screens, the panel's layout, the lid, the models) is the agent
 * client's, in {@code tools/agent/gameplay/}.
 */
public final class BackpackGameTests {
    private static final BlockPos FLOOR = new BlockPos(1, 1, 1);
    private static final BlockPos ABOVE = FLOOR.above();

    /** A backpack over a water source floats, and is set down again once the water is gone. */
    public void floatsOnStillWater(GameTestHelper helper) {
        // The backpack first, so it is the neighbor update from the water that makes it float.
        helper.setBlock(ABOVE, BackpacksBlocks.backpack());
        helper.assertBlockProperty(ABOVE, BackpackBlock.FLOATING, false);
        helper.setBlock(FLOOR, Blocks.WATER);
        helper.assertBlockProperty(ABOVE, BackpackBlock.FLOATING, true);
        helper.setBlock(FLOOR, Blocks.STONE);
        helper.assertBlockProperty(ABOVE, BackpackBlock.FLOATING, false);
        helper.succeed();
    }

    /** A backpack standing in water does not float, whatever is under it. */
    public void standsInWater(GameTestHelper helper) {
        BlockState waterlogged = BackpacksBlocks.backpack().defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true);
        helper.setBlock(ABOVE, waterlogged);
        helper.setBlock(FLOOR, Blocks.WATER);
        helper.assertBlockProperty(ABOVE, BackpackBlock.FLOATING, false);
        helper.succeed();
    }

    /** A backpack placed from an item takes the item's id, and with it the items already inside. */
    public void placedBackpackKeepsItsItems(GameTestHelper helper) {
        ItemStack stack = new ItemStack(BackpacksItems.backpack());
        UUID id = BackpackIds.getOrCreate(stack);
        storage(helper).contents(id, BackpackTier.LEATHER.size()).setItem(3, new ItemStack(Items.EMERALD, 7));

        place(helper, stack);

        BackpackBlockEntity placed = helper.getBlockEntity(ABOVE, BackpackBlockEntity.class);
        helper.assertValueEqual(placed.id(), id, "the placed backpack's id");
        assertStack(helper, placed.getItem(3), Items.EMERALD, 7, "slot 3 of the placed backpack");
        helper.succeed();
    }

    /** A broken backpack drops as an item with the placed one's id, and the items stay with that id. */
    public void brokenBackpackKeepsItsItems(GameTestHelper helper) {
        helper.setBlock(ABOVE, BackpacksBlocks.backpack());
        BackpackBlockEntity placed = helper.getBlockEntity(ABOVE, BackpackBlockEntity.class);
        placed.setItem(0, new ItemStack(Items.DIAMOND, 5));
        UUID id = placed.id();
        helper.assertTrue(id != null, "a backpack set by a command is given an id when it is first filled");

        helper.getLevel().destroyBlock(helper.absolutePos(ABOVE), true);

        List<ItemEntity> drops = helper.getEntities(EntityTypes.ITEM, ABOVE, 2.0);
        ItemStack dropped = drops.stream().map(ItemEntity::getItem)
                .filter(drop -> drop.getItem() instanceof BackpackItem)
                .findFirst().orElse(ItemStack.EMPTY);
        helper.assertTrue(!dropped.isEmpty(), "the broken backpack drops as an item");
        helper.assertValueEqual(BackpackIds.get(dropped), id, "the dropped backpack's id");
        assertStack(helper, storage(helper).contents(id, BackpackTier.LEATHER.size()).getItem(0),
                Items.DIAMOND, 5, "slot 0 of the dropped backpack");
        helper.succeed();
    }

    /** Each tier's placed backpack has its own number of slots, and its item says the same. */
    public void tiersHaveTheirOwnSize(GameTestHelper helper) {
        for (BackpackTier tier : BackpackTier.values()) {
            BlockPos pos = new BlockPos(tier.ordinal(), 1, 1);
            helper.setBlock(pos, BackpacksBlocks.backpack(tier));
            int size = helper.getBlockEntity(pos, BackpackBlockEntity.class).getContainerSize();
            helper.assertValueEqual(size, tier.size(), "slots of a placed " + tier.path());
            int itemSize = ((BackpackItem) BackpacksItems.backpack(tier)).size();
            helper.assertValueEqual(itemSize, tier.size(), "slots of a " + tier.path() + " item");
        }
        helper.succeed();
    }

    /**
     * A backpack crafted from another keeps its id, and placed, the larger tier shows the smaller one's
     * items where they were, with the new slots empty.
     */
    public void upgradeKeepsTheItems(GameTestHelper helper) {
        ItemStack leather = new ItemStack(BackpacksItems.backpack());
        UUID id = BackpackIds.getOrCreate(leather);
        storage(helper).contents(id, BackpackTier.LEATHER.size()).setItem(7, new ItemStack(Items.GOLD_INGOT, 3));

        ItemStack iron = BackpackCrafting.carryOver(
                List.of(new ItemStack(Items.IRON_INGOT), leather, new ItemStack(Items.IRON_INGOT)),
                new ItemStack(BackpacksItems.backpack(BackpackTier.IRON)));
        helper.assertValueEqual(BackpackIds.get(iron), id, "the crafted iron backpack's id");

        place(helper, iron);
        BackpackBlockEntity placed = helper.getBlockEntity(ABOVE, BackpackBlockEntity.class);
        helper.assertValueEqual(placed.getContainerSize(), BackpackTier.IRON.size(), "slots of the upgraded backpack");
        assertStack(helper, placed.getItem(7), Items.GOLD_INGOT, 3, "slot 7 of the upgraded backpack");
        helper.assertTrue(placed.getItem(15).isEmpty(), "the upgraded backpack's new slots are empty");
        helper.succeed();
    }

    /** A backpack takes what a bundle takes: no backpacks, no shulker boxes. */
    public void refusesBackpacksAndShulkerBoxes(GameTestHelper helper) {
        helper.setBlock(ABOVE, BackpacksBlocks.backpack());
        BackpackBlockEntity placed = helper.getBlockEntity(ABOVE, BackpackBlockEntity.class);
        helper.assertTrue(placed.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "a backpack takes a diamond");
        helper.assertTrue(!placed.canPlaceItem(0, new ItemStack(BackpacksItems.backpack())), "a backpack refuses a backpack");
        helper.assertTrue(!placed.canPlaceItem(0, new ItemStack(Items.SHULKER_BOX)), "a backpack refuses a shulker box");
        helper.succeed();
    }

    /** The storage reads back what it saved: the items, their slots and the backpack's size. */
    public void storageSavesAndLoads(GameTestHelper helper) {
        UUID id = UUID.randomUUID();
        BackpackStorage saved = new BackpackStorage();
        ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.setDamageValue(12);
        saved.contents(id, BackpackTier.GOLD.size()).setItem(19, named);
        saved.contents(id, BackpackTier.GOLD.size()).setItem(0, new ItemStack(Items.COBBLESTONE, 64));

        var ops = helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        Tag tag = BackpackStorage.CODEC.encodeStart(ops, saved).getOrThrow();
        BackpackStorage loaded = BackpackStorage.CODEC.parse(ops, tag).getOrThrow();

        BackpackContents contents = loaded.contents(id, BackpackTier.LEATHER.size());
        helper.assertValueEqual(contents.getContainerSize(), BackpackTier.GOLD.size(), "slots read back");
        assertStack(helper, contents.getItem(0), Items.COBBLESTONE, 64, "slot 0 read back");
        assertStack(helper, contents.getItem(19), Items.DIAMOND_SWORD, 1, "slot 19 read back");
        helper.assertValueEqual(contents.getItem(19).getDamageValue(), 12, "the sword's damage read back");
        helper.succeed();
    }

    /**
     * The worn backpack's panel, as the server sees its messages: a stack stored from the inventory goes
     * into the backpack, a shift click takes it back, and a click past the tier's slots does nothing.
     *
     * <p>The panel answers through the network, so the player needs a connection: the one mock player
     * vanilla gives one, deprecated without a replacement yet.
     */
    @SuppressWarnings("removal")
    public void panelStoresAndTakes(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        ItemStack worn = new ItemStack(BackpacksItems.backpack());
        UUID id = BackpackIds.getOrCreate(worn);
        player.setItemSlot(EquipmentSlot.CHEST, worn);
        // The inventory screen's menu; its slot 9 is the first of the inventory above the hotbar.
        int menu = player.inventoryMenu.containerId;
        player.getInventory().setItem(9, new ItemStack(Items.DIAMOND, 10));

        PanelServer.open(player, new PanelOpen(menu, true));
        try {
            PanelServer.store(player, new PanelStore(menu, 9));
            BackpackContents contents = storage(helper).contents(id, BackpackTier.LEATHER.size());
            assertStack(helper, contents.getItem(0), Items.DIAMOND, 10, "the backpack's slot 0 after storing");
            helper.assertTrue(player.getInventory().getItem(9).isEmpty(), "the inventory slot is empty after storing");

            PanelServer.click(player, new PanelClick(menu, BackpackTier.LEATHER.size(), 0, false));
            helper.assertTrue(player.inventoryMenu.getCarried().isEmpty(), "a click past the tier's slots takes nothing");

            PanelServer.click(player, new PanelClick(menu, 0, 0, true));
            helper.assertTrue(contents.getItem(0).isEmpty(), "the backpack's slot 0 is empty after a shift click");
            helper.assertValueEqual(player.getInventory().countItem(Items.DIAMOND), 10, "diamonds back in the inventory");
        } finally {
            PanelServer.open(player, new PanelOpen(menu, false));
        }
        helper.succeed();
    }

    /** Places {@code stack} on a stone at {@link #FLOOR}, as a player holding it would. */
    private static void place(GameTestHelper helper, ItemStack stack) {
        helper.setBlock(FLOOR, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        // placeAt uses what the player holds, and the stack it is given only to call useOn on.
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, FLOOR, Direction.UP);
    }

    private static BackpackStorage storage(GameTestHelper helper) {
        return BackpackStorage.of(helper.getLevel().getServer());
    }

    private static void assertStack(GameTestHelper helper, ItemStack stack, Item item, int count, String what) {
        helper.assertTrue(stack.is(item) && stack.getCount() == count,
                what + ": expected " + count + " " + item + ", got " + stack.getCount() + " " + stack.getItem());
    }
}
