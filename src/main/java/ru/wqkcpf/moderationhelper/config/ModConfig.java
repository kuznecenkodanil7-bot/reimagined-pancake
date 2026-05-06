package ru.wqkcpf.moderationhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("moderation-helper-gui.json");

    private static ModConfigData data;

    private ModConfig() {}

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                    data = GSON.fromJson(reader, ModConfigData.class);
                }
                if (data == null) data = ModConfigData.defaults();
                data.normalize();
            } else {
                data = ModConfigData.defaults();
                save();
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Failed to load config, using defaults", e);
            data = ModConfigData.defaults();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Failed to save config", e);
        }
    }

    public static ModConfigData get() {
        if (data == null) load();
        return data;
    }

    public enum CleanupMode {
        DELETE,
        ARCHIVE,
        OFF
    }

    public static final class ModConfigData {
        public boolean obsEnabled = false;
        public String obsHost = "localhost";
        public int obsPort = 4455;
        public String obsPassword = "";

        public int recentPlayersLimit = 15;
        public CleanupMode screenshotCleanupMode = CleanupMode.ARCHIVE;
        public int screenshotRetentionDays = 30;
        public String screenshotDirectory = "moderation_screenshots";

        public String checkCommandTemplate = "/check {nick}";
        public String checkTellTemplate = "/tell {nick} Здравствуйте, проверка на читы. В течении 5 минут жду ваш Anydesk(наилучший вариант, скачать можно в любом браузере)/Discord. Также сообщаю, что в случае признания на наличие чит-клиентов срок бана составит 20 дней, вместо 30.";

        public List<QuickReasonConfig> quickReasons = new ArrayList<>();

        public static ModConfigData defaults() {
            ModConfigData config = new ModConfigData();
            config.quickReasons = defaultReasons();
            return config;
        }

        public void normalize() {
            if (obsHost == null || obsHost.isBlank()) obsHost = "localhost";
            if (obsPort <= 0) obsPort = 4455;
            if (recentPlayersLimit <= 0) recentPlayersLimit = 15;
            if (screenshotRetentionDays <= 0) screenshotRetentionDays = 30;
            if (screenshotDirectory == null || screenshotDirectory.isBlank()) screenshotDirectory = "moderation_screenshots";
            if (checkCommandTemplate == null || checkCommandTemplate.isBlank()) checkCommandTemplate = "/check {nick}";
            if (checkTellTemplate == null || checkTellTemplate.isBlank()) {
                checkTellTemplate = "/tell {nick} Здравствуйте, проверка на читы. В течении 5 минут жду ваш Anydesk(наилучший вариант, скачать можно в любом браузере)/Discord. Также сообщаю, что в случае признания на наличие чит-клиентов срок бана составит 20 дней, вместо 30.";
            }
            if (quickReasons == null || quickReasons.isEmpty()) quickReasons = defaultReasons();
        }
    }

    public static final class QuickReasonConfig {
        /** WARN, MUTE, BAN, IPBAN */
        public String type;
        /** Rule code, for example 2.2 or 3.8. */
        public String code;
        /** Text shown in GUI. */
        public String title;
        /** Optional full text shown in tooltip/README and used only if commandReason is empty. */
        public String description;
        /** Recommended duration. Empty for WARN. */
        public String defaultDuration;
        /** What goes into command after duration. Usually the rule code. */
        public String commandReason;

        public QuickReasonConfig() {}

        public QuickReasonConfig(String type, String code, String title, String description, String defaultDuration) {
            this.type = type;
            this.code = code;
            this.title = title;
            this.description = description;
            this.defaultDuration = defaultDuration;
            this.commandReason = code;
        }

        public String displayName() {
            String dur = defaultDuration == null || defaultDuration.isBlank() ? "" : " · " + defaultDuration;
            return code + " — " + title + dur;
        }

        public String reasonForCommand() {
            if (commandReason != null && !commandReason.isBlank()) return commandReason.trim();
            if (code != null && !code.isBlank()) return code.trim();
            return title == null ? "" : title.trim();
        }
    }

    private static List<QuickReasonConfig> defaultReasons() {
        List<QuickReasonConfig> list = new ArrayList<>();

        list.add(new QuickReasonConfig("WARN", "2.1", "Предупреждение", "предупреждение", ""));

        list.add(new QuickReasonConfig("MUTE", "2.2", "Оскорбления", "Запрещено оскорблять кого-либо/что-либо.", "12h"));
        list.add(new QuickReasonConfig("BAN", "2.2", "Оскорбления", "Бан по пункту 2.2.", "2d"));
        list.add(new QuickReasonConfig("MUTE", "2.3", "Оскорбление родных", "Запрещено оскорблять родных.", "5d"));
        list.add(new QuickReasonConfig("BAN", "2.3", "Оскорбление родных", "Бан по пункту 2.3.", "7d"));
        list.add(new QuickReasonConfig("MUTE", "2.4", "Сексуальный характер", "Запрещены сообщения сексуального характера.", "2h"));
        list.add(new QuickReasonConfig("MUTE", "2.5", "Неадекватное поведение", "Запрещено неадекватное поведение.", "2h"));
        list.add(new QuickReasonConfig("MUTE", "2.6", "Реклама", "Запрещена реклама серверов и сторонних ресурсов.", "1d"));
        list.add(new QuickReasonConfig("BAN", "2.6", "Реклама", "Бан за рекламу серверов/ресурсов.", "14d"));
        list.add(new QuickReasonConfig("MUTE", "2.7", "Ненависть/вражда", "Запрещена пропаганда или агитация ненависти и вражды.", "9h"));
        list.add(new QuickReasonConfig("BAN", "2.7", "Ненависть/вражда", "Бан по пункту 2.7.", "3d"));
        list.add(new QuickReasonConfig("MUTE", "2.8", "Ссылки/стримы/видео", "Запрещены сторонние ссылки/реклама стримов и видео.", "8h"));
        list.add(new QuickReasonConfig("MUTE", "2.9", "Выдача себя за администратора", "Запрещена выдача себя за администратора.", "12h"));
        list.add(new QuickReasonConfig("MUTE", "2.10", "Угрозы вне игры", "Запрещены угрозы, не относящиеся к игровому процессу.", "12h"));
        list.add(new QuickReasonConfig("MUTE", "2.11", "Угроза наказанием без причины", "Запрещено угрожать наказанием без реальной причины.", "6h"));
        list.add(new QuickReasonConfig("MUTE", "2.12", "Политика", "Запрещено обсуждать политику.", "12h"));
        list.add(new QuickReasonConfig("MUTE", "2.13", "Введение в заблуждение", "Запрещено вводить игроков в заблуждение.", "2h"));
        list.add(new QuickReasonConfig("MUTE", "2.14", "Попрошайничество", "Запрещено попрошайничество ресурсов/привилегий у администрации.", "6h"));
        list.add(new QuickReasonConfig("MUTE", "2.15", "Помехи в голосовом чате", "Запрещены помехи в голосовом чате.", "4h"));

        list.add(new QuickReasonConfig("BAN", "3.1", "Недопустимый никнейм", "Ник нарушает правила/содержит рекламу/читы/оскорбления.", "permanent"));
        list.add(new QuickReasonConfig("IPBAN", "3.3", "Торговля за реальные деньги", "Запрещена любая торговля за реальные деньги.", "30d"));
        list.add(new QuickReasonConfig("IPBAN", "3.6", "Баги/лаг-машины", "Использование багов сервера или постройка лаг-машин.", "15d"));
        list.add(new QuickReasonConfig("IPBAN", "3.7", "Стороннее ПО", "Использование/хранение стороннего ПО.", "30d"));
        list.add(new QuickReasonConfig("IPBAN", "3.8", "Обход бана за читы", "Обход бана за читы с другого аккаунта.", "30d"));
        list.add(new QuickReasonConfig("IPBAN", "3.9", "Подстрекательство", "Подстрекательство игроков на нарушение правил.", "3d"));
        list.add(new QuickReasonConfig("IPBAN", "3.10", "Тим с читером", "Тим с игроком, использующим стороннее ПО.", "15d"));
        list.add(new QuickReasonConfig("BAN", "4.1", "Нет доказательств", "Донатеры обязаны иметь доказательства при выдаче наказания.", "20d"));

        return list;
    }
}
