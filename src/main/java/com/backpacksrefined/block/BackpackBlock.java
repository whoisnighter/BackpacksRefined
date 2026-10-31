package com.backpacksrefined.block;

import com.backpacksrefined.item.BackpacksItems;
import com.backpacksrefined.menu.BackpackMenu;
import com.backpacksrefined.platform.BackpackIds;
import com.backpacksrefined.platform.OpenableBlock;
import com.backpacksrefined.sound.BackpacksSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.UUID;

/**
 * A backpack set down on the ground, facing the way the player who placed it was looking. Used, it opens
 * as a three-row chest over the backpack's items; broken, it gives the backpack back with the same items.
 * The items never move: the block entity takes the backpack's id when it is placed, and the dropped
 * backpack takes it back (see {@link BackpackBlockEntity}).
 *
 * <p>It can stand in water, and it floats on still water: with a water source under it and none in it,
 * it sits lower, half sunk into the water (the {@code floating} models), and is shorter.
 *
 * <p>The overrides are public where Minecraft has them protected or public depending on the version,
 * which compiles against both.
 */
public class BackpackBlock extends OpenableBlock implements EntityBlock {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BackpackBlockEntity(pos, state);
    }

    /** The placed backpack takes the id of the backpack it was placed from, and with it its items. */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        UUID id = BackpackIds.get(stack);
        if (id != null && level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack) backpack.setId(id);
    }

    @Override
    protected void open(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity backpack)) return;
        player.openMenu(new SimpleMenuProvider((containerId, inventory, opener) ->
                new BackpackMenu(containerId, inventory, backpack), getName()));
        level.playSound(null, pos, BackpacksSounds.EQUIP, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** The dropped backpack takes the placed one's id, and with it its items. */
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof BackpackBlockEntity backpack
                && backpack.id() != null) {
            for (ItemStack drop : drops) {
                if (drop.is(BackpacksItems.backpack())) BackpackIds.set(drop, backpack.id());
            }
        }
        return drops;
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
