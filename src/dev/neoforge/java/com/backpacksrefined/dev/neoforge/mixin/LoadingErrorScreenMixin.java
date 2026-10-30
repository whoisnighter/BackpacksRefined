package com.backpacksrefined.dev.neoforge.mixin;

import com.backpacksrefined.dev.neoforge.LoadingWarnings;
import net.neoforged.neoforge.client.gui.LoadingErrorScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ends an unattended run that failed to load, where {@link LoadingWarnings}' screen listener cannot:
 * NeoForge only starts posting events once loading has finished, so on a loading error no screen event
 * ever arrives. A mixin config is applied before any mod is constructed, so this runs even when this
 * mod's own constructor is the one that failed. It does not run when FML fails earlier, sorting mods
 * (a missing dependency): no mod mixin is applied then, and the {@code -javaagent} in src/watchdog
 * stops the run instead.
 */
@Mixin(LoadingErrorScreen.class)
abstract class LoadingErrorScreenMixin {
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void backpacks$stopUnattendedRun(CallbackInfo info) {
        LoadingWarnings.stopIfUnattendedError((LoadingErrorScreen) (Object) this);
    }
}
