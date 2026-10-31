package com.backpacksrefined.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
//? if <1.20.5 {
/*import net.minecraft.world.InteractionHand;
*///?}

/**
 * A waterloggable block that opens when used with an empty hand, or with an item that does nothing on
 * it. 1.20.5 split using a block into with and without an item; before, {@code use} had the hand.
 * 1.21.2 made success one result for both sides, and 1.21.9 made {@code isClientSide} a method.
 */
public abstract class OpenableBlock extends WaterloggableBlock {
    protected OpenableBlock(Properties properties) {
        super(properties);
    }

    /** Opens the block for {@code player}, on the server. */
    protected abstract void open(Level level, BlockPos pos, Player player);

    //? if >=1.20.5 {
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
    //?} else {
    /*@Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
    *///?}
        //? if >=1.21.9 {
        boolean client = level.isClientSide();
        //?} else {
        /*boolean client = level.isClientSide;
        *///?}
        if (!client && !player.isSpectator()) open(level, pos, player);
        //? if >=1.21.2 {
        return InteractionResult.SUCCESS;
        //?} else {
        /*return InteractionResult.sidedSuccess(client);
        *///?}
    }
}
