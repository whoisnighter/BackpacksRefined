package com.backpacksrefined.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
//? if >=1.21.2 {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
//?} else {
/*import net.minecraft.world.level.LevelAccessor;
*///?}

/**
 * A block that can stand in water, and that hears about its neighbors changing through
 * {@link #updateWithNeighbors}. The block itself adds {@code waterlogged} to its state definition.
 * 1.21.2 changed the arguments of {@code updateShape} and gave the tick scheduling its own argument.
 */
public abstract class WaterloggableBlock extends Block implements SimpleWaterloggedBlock {
    protected WaterloggableBlock(Properties properties) {
        super(properties);
    }

    /** The state once a neighbor of {@code pos} has changed. */
    protected BlockState updateWithNeighbors(BlockState state, BlockGetter level, BlockPos pos) {
        return state;
    }

    //? if >=1.21.2 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return updateWithNeighbors(state, level, pos);
    }
    //?} else {
    /*@Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(BlockStateProperties.WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return updateWithNeighbors(state, level, pos);
    }
    *///?}

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }
}
