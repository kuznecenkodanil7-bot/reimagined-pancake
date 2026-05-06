package ru.wqkcpf.moderationhelper.command;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.stats.SessionStats.PunishmentType;

public final class CommandSender {
    private CommandSender() {}

    public static void sendRawCommand(String slashCommand) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.player.networkHandler == null) return;

        String command = slashCommand == null ? "" : slashCommand.trim();
        if (command.startsWith("/")) command = command.substring(1);
        if (command.isBlank()) return;

        client.player.networkHandler.sendChatCommand(command);
    }

    public static void sendPunishment(PunishmentType type, String nick, String duration, String reason) {
        if (type == null || nick == null || nick.isBlank()) return;

        StringBuilder command = new StringBuilder(type.commandName()).append(' ').append(nick.trim());
        if (type.requiresDuration()) {
            if (duration == null || duration.isBlank()) {
                notifyLocal("§c[Moderation Helper] Не указано время наказания.");
                return;
            }
            command.append(' ').append(duration.trim());
        }
        if (reason != null && !reason.isBlank()) {
            command.append(' ').append(reason.trim());
        }
        sendRawCommand(command.toString());
    }

    public static void notifyLocal(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(Text.literal(text), false);
        }
    }
}
