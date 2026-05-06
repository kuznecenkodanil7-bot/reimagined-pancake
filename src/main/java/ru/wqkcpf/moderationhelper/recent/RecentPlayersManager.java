package ru.wqkcpf.moderationhelper.recent;

import ru.wqkcpf.moderationhelper.config.ModConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecentPlayersManager {
    private static final List<String> RECENT = new ArrayList<>();

    private RecentPlayersManager() {}

    public static void add(String nick) {
        if (nick == null || nick.isBlank()) return;
        RECENT.removeIf(existing -> existing.equalsIgnoreCase(nick));
        RECENT.add(0, nick);
        int limit = Math.max(1, ModConfig.get().recentPlayersLimit);
        while (RECENT.size() > limit) {
            RECENT.remove(RECENT.size() - 1);
        }
    }

    public static List<String> list() {
        return Collections.unmodifiableList(RECENT);
    }
}
