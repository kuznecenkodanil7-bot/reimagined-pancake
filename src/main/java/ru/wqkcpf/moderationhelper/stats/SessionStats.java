package ru.wqkcpf.moderationhelper.stats;

import java.util.EnumMap;
import java.util.Map;

public final class SessionStats {
    public enum PunishmentType {
        WARN("warn", false),
        MUTE("mute", true),
        BAN("ban", true),
        IPBAN("ipban", true);

        private final String commandName;
        private final boolean requiresDuration;

        PunishmentType(String commandName, boolean requiresDuration) {
            this.commandName = commandName;
            this.requiresDuration = requiresDuration;
        }

        public String commandName() {
            return commandName;
        }

        public boolean requiresDuration() {
            return requiresDuration;
        }
    }

    private static final Map<PunishmentType, Integer> COUNTERS = new EnumMap<>(PunishmentType.class);

    static {
        for (PunishmentType type : PunishmentType.values()) {
            COUNTERS.put(type, 0);
        }
    }

    private SessionStats() {}

    public static void increment(PunishmentType type) {
        COUNTERS.put(type, get(type) + 1);
    }

    public static int get(PunishmentType type) {
        return COUNTERS.getOrDefault(type, 0);
    }
}
