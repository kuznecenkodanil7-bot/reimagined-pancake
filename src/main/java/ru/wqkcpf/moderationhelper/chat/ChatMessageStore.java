package ru.wqkcpf.moderationhelper.chat;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

public final class ChatMessageStore {
    private static final int LIMIT = 80;
    private static final Deque<String> RECENT_MESSAGES = new ArrayDeque<>();

    private ChatMessageStore() {}

    public static void registerEvents() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> add(message.getString()));
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) add(message.getString());
        });
    }

    public static void add(String raw) {
        if (raw == null || raw.isBlank()) return;
        synchronized (RECENT_MESSAGES) {
            RECENT_MESSAGES.addFirst(raw);
            while (RECENT_MESSAGES.size() > LIMIT) RECENT_MESSAGES.removeLast();
        }
    }

    public static String newest() {
        synchronized (RECENT_MESSAGES) {
            return RECENT_MESSAGES.peekFirst();
        }
    }

    /**
     * Best-effort fallback: Fabric/Yarn often changes ChatHud internals.
     * If exact text-under-mouse extraction fails, we still use the newest visible chat line.
     */
    public static String bestCandidate() {
        synchronized (RECENT_MESSAGES) {
            List<String> copy = RECENT_MESSAGES.stream().toList();
            for (String raw : copy) {
                if (ChatNicknameParser.parseNick(raw) != null) return raw;
            }
        }
        return newest();
    }
}
