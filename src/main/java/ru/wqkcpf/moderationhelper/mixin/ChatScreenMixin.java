package ru.wqkcpf.moderationhelper.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.wqkcpf.moderationhelper.chat.ChatClickHandler;
import ru.wqkcpf.moderationhelper.chat.ChatMessageExtractor;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    protected ChatScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void mhg$onMiddleClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;

        String rawMessage = ChatMessageExtractor.extractMessageUnderMouse(MinecraftClient.getInstance(), mouseX, mouseY);
        ChatClickHandler.handleMiddleClick(rawMessage);
        cir.setReturnValue(true);
    }
}
