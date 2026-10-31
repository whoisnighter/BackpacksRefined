package com.backpacksrefined.client.mixin;

import com.backpacksrefined.client.panel.BackpackPanel;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * Hangs the worn backpack's panel on every container screen (see {@code BackpackPanel}): set up with
 * the screen, drawn after its contents and before the carried stack, and given the mouse first, so a
 * press on the panel never counts as a click outside the screen. One mixin serves every loader.
 *
 * <p>Where versions differ: the contents are drawn by {@code extractContents} from 26.1 and by
 * {@code renderContents} from 1.21.6; before, the whole screen is one {@code render}, and the panel is
 * drawn at its end, under the carried stack, which is drawn further forward. The mouse methods take a
 * {@code MouseButtonEvent} from 1.21.9, and the modifier keys come with it.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    @Shadow protected int leftPos;
    @Shadow protected int topPos;
    @Shadow protected int imageWidth;
    @Shadow protected Slot hoveredSlot;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    private AbstractContainerScreen<?> backpacksrefined$self() {
        return (AbstractContainerScreen<?>) (Object) this;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void backpacksrefined$attach(CallbackInfo ci) {
        BackpackPanel.attach(backpacksrefined$self());
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void backpacksrefined$detach(CallbackInfo ci) {
        BackpackPanel.detach(backpacksrefined$self());
    }

    //? if >=26.1 {
    @Inject(method = "extractContents", at = @At("TAIL"))
    //?} else if >=1.21.6 {
    /*@Inject(method = "renderContents", at = @At("TAIL"))
    *///?} else {
    /*@Inject(method = "render", at = @At("TAIL"))
    *///?}
    private void backpacksrefined$draw(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        BackpackPanel.render(backpacksrefined$self(), graphics, mouseX, mouseY, leftPos, topPos, imageWidth);
    }

    //? if >=1.21.9 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$click(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackPanel.mouseClicked(backpacksrefined$self(), event.x(), event.y(), event.button(),
                event.hasShiftDown(), event.hasAltDown(), hoveredSlot)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$release(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackPanel.mouseOther(backpacksrefined$self(), event.x(), event.y(), true)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$drag(MouseButtonEvent event, double dragX, double dragY, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackPanel.mouseOther(backpacksrefined$self(), event.x(), event.y(), false)) cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$click(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackPanel.mouseClicked(backpacksrefined$self(), mouseX, mouseY, button,
                Screen.hasShiftDown(), Screen.hasAltDown(), hoveredSlot)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$release(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (BackpackPanel.mouseOther(backpacksrefined$self(), mouseX, mouseY, true)) cir.setReturnValue(true);
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true)
    private void backpacksrefined$drag(double mouseX, double mouseY, int button, double dragX, double dragY,
            CallbackInfoReturnable<Boolean> cir) {
        if (BackpackPanel.mouseOther(backpacksrefined$self(), mouseX, mouseY, false)) cir.setReturnValue(true);
    }
    *///?}
}
