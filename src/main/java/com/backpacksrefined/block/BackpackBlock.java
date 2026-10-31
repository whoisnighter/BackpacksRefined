package com.backpacksrefined.block;

import com.backpacksrefined.platform.WaterloggableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A backpack set down on the ground, facing the way the player who placed it was looking. It holds
 * nothing yet and does nothing when used; breaking it gives the backpack back.
 *
 * <p>It can stand in water, and it floats on still water: with a water source under it and none in it,
 * it sits lower, half sunk into the water (the {@code floating} models), and is shorter.
 *
 * <p>The overrides are public where Minecraft has them protected or public depending on the version,
 * which compiles against both.
 */
public class BackpackBlock extends WaterloggableBlock {
    public static final BooleanProperty FLOATING = BooleanProperty.create("floating");

    /** Facing north or south: wider than deep. */
    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(3.0, 0.0, 4.0, 13.0, 12.0, 12.0);
    /** Facing east or west. */
    private static final VoxelShape SHAPE_EAST_WEST = Block.box(4.0, 0.0, 3.0, 12.0, 12.0, 13.0);
    private static final VoxelShape FLOATING_SHAPE_NORTH_SOUTH = Block.box(3.0, 0.0, 4.0, 13.0, 8.0, 12.0);
    private static final VoxelShape FLOATING_SHAPE_EAST_WEST = Block.box(4.0, 0.0, 3.0, 12.0, 8.0, 13.0);

    public BackpackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH)
                .setValue(FLOATING, false)
                .setValue(BlockStateProperties.WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HorizontalDirectionalBlock.FACING, FLOATING, BlockStateProperties.WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean waterlogged = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        BlockState state = defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, context.getHorizontalDirection())
                .setValue(BlockStateProperties.WATERLOGGED, waterlogged);
        return updateWithNeighbors(state, context.getLevel(), context.getClickedPos());
    }

    /** Floats while there is a water source under it and it is not in water itself. */
    @Override
    protected BlockState updateWithNeighbors(BlockState state, BlockGetter level, BlockPos pos) {
        FluidState below = level.getFluidState(pos.below());
        boolean floating = below.getType() == Fluids.WATER && !state.getValue(BlockStateProperties.WATERLOGGED);
        return state.setValue(FLOATING, floating);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean eastWest = state.getValue(HorizontalDirectionalBlock.FACING).getAxis() == Direction.Axis.X;
        if (state.getValue(FLOATING)) return eastWest ? FLOATING_SHAPE_EAST_WEST : FLOATING_SHAPE_NORTH_SOUTH;
        return eastWest ? SHAPE_EAST_WEST : SHAPE_NORTH_SOUTH;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(HorizontalDirectionalBlock.FACING, rotation.rotate(state.getValue(HorizontalDirectionalBlock.FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(HorizontalDirectionalBlock.FACING)));
    }
}
