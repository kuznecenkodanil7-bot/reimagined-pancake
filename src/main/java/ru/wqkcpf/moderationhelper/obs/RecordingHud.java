package ru.wqkcpf.moderationhelper.obs;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class RecordingHud {
    private RecordingHud() {}

    @SuppressWarnings("deprecation")
    public static void register() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!RecordingTimer.isActive()) return;
            MinecraftClient client = MinecraftClient.getInstance();
            if (client == null || client.textRenderer == null) return;

            String label = "Идёт запись: " + RecordingTimer.formatted();
            int x = drawContext.getScaledWindowWidth() / 2 - client.textRenderer.getWidth(label) / 2;
            int y = drawContext.getScaledWindowHeight() - 72;
            drawContext.fill(x - 6, y - 4, x + client.textRenderer.getWidth(label) + 6, y + 12, 0x99000000);
            drawContext.drawTextWithShadow(client.textRenderer, Text.literal(label), x, y, 0xFFFF5555);
        });
    }
}
