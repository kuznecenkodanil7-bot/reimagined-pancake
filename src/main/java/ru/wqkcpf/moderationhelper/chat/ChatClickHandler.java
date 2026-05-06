package ru.wqkcpf.moderationhelper.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.gui.PunishmentScreen;
import ru.wqkcpf.moderationhelper.recent.RecentPlayersManager;
import ru.wqkcpf.moderationhelper.screenshot.ScreenshotManager;

public final class ChatClickHandler {
    private ChatClickHandler() {}

    public static void handleMiddleClick(String rawMessage) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        String raw = rawMessage == null || rawMessage.isBlank() ? ChatMessageStore.bestCandidate() : rawMessage;
        String nick = ChatNicknameParser.parseNick(raw);
        if (nick == null) {
            client.player.sendMessage(Text.literal("§c[Moderation Helper] Ник не найден в сообщении."), false);
            return;
        }

        RecentPlayersManager.add(nick);

        ScreenshotManager.ScreenshotTicket ticket = null;
        if (!ChatNicknameParser.shouldSkipScreenshot(raw)) {
            ticket = ScreenshotManager.takeTempScreenshot(nick);
        }

        client.setScreen(new PunishmentScreen(nick, ticket));
    }
}
