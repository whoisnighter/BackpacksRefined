package com.backpacksrefined.dev.mixin;

import com.backpacksrefined.dev.agent.backpacks.Pointer;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ignores the real mouse while the agent's virtual pointer is shown, so a person's hand passing over
 * the window does not move the pointer a recording is following. The arguments are left out because
 * 26.3's move to SDL gave the method two more; the name alone matches it on every version.
 */
@Mixin(MouseHandler.class)
abstract class MouseMoveMixin {
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void backpacks$followVirtualPointer(CallbackInfo info) {
        if (Pointer.shown()) info.cancel();
    }
}
