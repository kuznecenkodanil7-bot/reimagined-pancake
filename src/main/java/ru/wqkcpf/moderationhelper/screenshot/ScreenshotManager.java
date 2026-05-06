package ru.wqkcpf.moderationhelper.screenshot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.config.ModConfig;
import ru.wqkcpf.moderationhelper.stats.SessionStats.PunishmentType;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ScreenshotManager {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private ScreenshotManager() {}

    public record ScreenshotTicket(Path tempPath, String nick, String datetime) {}

    public static void initFolders() {
        try {
            Files.createDirectories(baseDir());
            Files.createDirectories(folder("temp"));
            Files.createDirectories(folder("warn"));
            Files.createDirectories(folder("mute"));
            Files.createDirectories(folder("ban"));
            Files.createDirectories(folder("ipban"));
            Files.createDirectories(folder("archive"));
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Failed to create screenshot folders", e);
        }
    }

    public static ScreenshotTicket takeTempScreenshot(String nick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return null;

        try {
            initFolders();
            String datetime = nowForFile();
            String cleanNick = sanitize(nick);
            String relativeFile = ModConfig.get().screenshotDirectory + "/temp/" + cleanNick + "_" + datetime + ".png";
            Path expected = client.runDirectory.toPath().resolve(relativeFile).normalize();

            // Must be called before opening the GUI; this method is invoked directly from the chat click mixin.
            ScreenshotRecorder.saveScreenshot(client.runDirectory, relativeFile, client.getFramebuffer(), 1, text -> {
                if (client.player != null) {
                    client.player.sendMessage(Text.literal("§7[Moderation Helper] Скрин сохранён во временную папку."), false);
                }
            });
            return new ScreenshotTicket(expected, nick, datetime);
        } catch (Throwable t) {
            ModerationHelperClient.LOGGER.error("Failed to take temp screenshot", t);
            if (client.player != null) {
                client.player.sendMessage(Text.literal("§c[Moderation Helper] Не удалось сделать скриншот."), false);
            }
            return null;
        }
    }

    public static void finalizeScreenshot(ScreenshotTicket ticket, PunishmentType type, String duration, String reason) {
        if (ticket == null || ticket.tempPath() == null) return;
        try {
            initFolders();
            Path tempPath = ticket.tempPath();
            if (!Files.exists(tempPath)) {
                ModerationHelperClient.LOGGER.warn("Temp screenshot does not exist yet: {}", tempPath);
                return;
            }

            String dur = duration == null || duration.isBlank() ? "no-time" : duration;
            String rsn = reason == null || reason.isBlank() ? "no-reason" : reason;
            String filename = sanitize(ticket.nick()) + "_"
                    + sanitize(type.commandName()) + "_"
                    + sanitize(dur) + "_"
                    + sanitize(rsn) + "_"
                    + ticket.datetime() + ".png";

            Path target = folder(type.commandName()).resolve(filename);
            target = avoidOverwrite(target);
            Files.move(tempPath, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Failed to finalize screenshot", e);
        }
    }

    public static void cleanupOldScreenshots() {
        ModConfig.ModConfigData cfg = ModConfig.get();
        if (cfg.screenshotCleanupMode == ModConfig.CleanupMode.OFF) return;

        try {
            initFolders();
            Instant threshold = Instant.now().minusSeconds((long) cfg.screenshotRetentionDays * 24L * 60L * 60L);
            try (var stream = Files.walk(baseDir())) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .filter(path -> !path.startsWith(folder("archive")))
                        .forEach(path -> cleanupOne(path, threshold, cfg.screenshotCleanupMode));
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Screenshot cleanup failed", e);
        }
    }

    private static void cleanupOne(Path path, Instant threshold, ModConfig.CleanupMode mode) {
        try {
            Instant modified = Files.getLastModifiedTime(path).toInstant();
            if (modified.isAfter(threshold)) return;

            if (mode == ModConfig.CleanupMode.DELETE) {
                Files.deleteIfExists(path);
            } else if (mode == ModConfig.CleanupMode.ARCHIVE) {
                Path target = avoidOverwrite(folder("archive").resolve(path.getFileName()));
                Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.warn("Could not cleanup screenshot {}", path, e);
        }
    }

    private static Path baseDir() {
        File runDir = MinecraftClient.getInstance() == null ? new File(".") : MinecraftClient.getInstance().runDirectory;
        return runDir.toPath().resolve(ModConfig.get().screenshotDirectory).normalize();
    }

    private static Path folder(String name) {
        return baseDir().resolve(name).normalize();
    }

    public static String sanitize(String value) {
        if (value == null || value.isBlank()) return "empty";
        String sanitized = value.trim().replaceAll("[\\\\/:*?\"<>|\s]+", "_");
        sanitized = sanitized.replaceAll("_+", "_");
        if (sanitized.length() > 80) sanitized = sanitized.substring(0, 80);
        return sanitized;
    }

    private static String nowForFile() {
        return LocalDateTime.now(ZoneId.systemDefault()).format(FILE_TIME);
    }

    private static Path avoidOverwrite(Path target) throws IOException {
        if (!Files.exists(target)) return target;
        String fileName = target.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 1; i < 999; i++) {
            Path candidate = target.getParent().resolve(base + "_" + i + ext);
            if (!Files.exists(candidate)) return candidate;
        }
        throw new FileAlreadyExistsException(target.toString());
    }
}
