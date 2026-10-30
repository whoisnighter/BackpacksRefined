package com.backpacksrefined.dev.mixin;

import com.backpacksrefined.dev.agent.backpacks.ClientWindow;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Leaves the mouse pointer alone on a client an agent drives, so the desktop around the window stays
 * usable. See {@link ClientWindow} for why, and for why it is off unless {@code -Pdriven} is passed.
 */
@Mixin(MouseHandler.class)
abstract class MouseGrabMixin {
    @Inject(method = "grabMouse", at = @At("HEAD"), cancellable = true)
    private void backpacks$keepPointerFree(CallbackInfo info) {
        if (ClientWindow.driven()) info.cancel();
    }
}
