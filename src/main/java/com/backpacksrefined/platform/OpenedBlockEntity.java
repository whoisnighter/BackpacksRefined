package com.backpacksrefined.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
//? if >=1.21.9 {
import net.minecraft.world.entity.ContainerUser;
//?} else {
/*import net.minecraft.world.entity.player.Player;
*///?}

/**
 * A block entity that is a container and counts who has it open, on the server. A menu tells its
 * container when someone opens and closes it: a player before 1.21.9, any {@code ContainerUser} from
 * then. Spectators are not counted, as vanilla's chests do not count them.
 */
public abstract class OpenedBlockEntity extends IdBlockEntity implements Container {
    private int openers;

    protected OpenedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Someone opened it or closed it, and {@code openers} have it open now. */
    protected abstract void openersChanged(int openers);

    //? if >=1.21.9 {
    @Override
    public void startOpen(ContainerUser user) {
        if (!user.getLivingEntity().isSpectator()) changeOpeners(1);
    }

    @Override
    public void stopOpen(ContainerUser user) {
        if (!user.getLivingEntity().isSpectator()) changeOpeners(-1);
    }
    //?} else {
    /*@Override
    public void startOpen(Player player) {
        if (!player.isSpectator()) changeOpeners(1);
    }

    @Override
    public void stopOpen(Player player) {
        if (!player.isSpectator()) changeOpeners(-1);
    }
    *///?}

    private void changeOpeners(int change) {
        openers = Math.max(0, openers + change);
        openersChanged(openers);
    }
}
