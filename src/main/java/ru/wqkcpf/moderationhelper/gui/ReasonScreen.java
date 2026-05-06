package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.command.CommandSender;
import ru.wqkcpf.moderationhelper.config.ModConfig;
import ru.wqkcpf.moderationhelper.obs.ObsController;
import ru.wqkcpf.moderationhelper.recent.RecentPlayersManager;
import ru.wqkcpf.moderationhelper.screenshot.ScreenshotManager;
import ru.wqkcpf.moderationhelper.stats.SessionStats;
import ru.wqkcpf.moderationhelper.stats.SessionStats.PunishmentType;

import java.util.ArrayList;
import java.util.List;

public class ReasonScreen extends Screen {
    private final String nick;
    private final PunishmentType type;
    private final String duration;
    private final ScreenshotManager.ScreenshotTicket screenshotTicket;
    private TextFieldWidget reasonField;

    public ReasonScreen(String nick, PunishmentType type, String duration, ScreenshotManager.ScreenshotTicket screenshotTicket) {
        super(Text.literal("Причина наказания"));
        this.nick = nick;
        this.type = type;
        this.duration = duration == null ? "" : duration;
        this.screenshotTicket = screenshotTicket;
    }

    @Override
    protected void init() {
        int panelW = 520;
        int panelH = 292;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        reasonField = new TextFieldWidget(textRenderer, x + 24, y + 72, 260, 22, Text.literal("Причина"));
        reasonField.setMaxLength(128);
        reasonField.setText(firstReason());
        addDrawableChild(reasonField);
        setInitialFocus(reasonField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Выдать наказание"), btn -> punish())
                .dimensions(x + 24, y + 106, 150, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), btn -> back())
                .dimensions(x + 184, y + 106, 100, 24).build());

        int bx = x + 24;
        int by = y + 154;
        List<ModConfig.QuickReasonConfig> reasons = reasonsForType();
        for (int i = 0; i < reasons.size() && i < 10; i++) {
            ModConfig.QuickReasonConfig reason = reasons.get(i);
            int px = bx + (i % 2) * 235;
            int py = by + (i / 2) * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(shorten(reason.displayName(), 32)), btn -> reasonField.setText(reason.reasonForCommand()))
                    .dimensions(px, py, 225, 20).build());
        }
    }

    private void punish() {
        String reason = reasonField.getText().trim();
        if (reason.isBlank()) {
            CommandSender.notifyLocal("§c[Moderation Helper] Укажи причину наказания.");
            return;
        }

        CommandSender.sendPunishment(type, nick, duration, reason);
        SessionStats.increment(type);
        RecentPlayersManager.add(nick);
        ScreenshotManager.finalizeScreenshot(screenshotTicket, type, duration, reason);

        if (type == PunishmentType.IPBAN && !"3.8".equals(reason.trim())) {
            ObsController.stopRecordingSilently();
        }

        CommandSender.notifyLocal("§a[Moderation Helper] Команда отправлена: /" + type.commandName() + " " + nick);
        if (client != null) client.setScreen(null);
    }

    private void back() {
        if (type.requiresDuration()) {
            client.setScreen(new DurationScreen(nick, type, screenshotTicket));
        } else {
            client.setScreen(new PunishmentScreen(nick, screenshotTicket));
        }
    }

    private String firstReason() {
        List<ModConfig.QuickReasonConfig> reasons = reasonsForType();
        return reasons.isEmpty() ? "" : reasons.get(0).reasonForCommand();
    }

    private List<ModConfig.QuickReasonConfig> reasonsForType() {
        List<ModConfig.QuickReasonConfig> result = new ArrayList<>();
        for (ModConfig.QuickReasonConfig reason : ModConfig.get().quickReasons) {
            if (reason.type != null && reason.type.equalsIgnoreCase(type.name())) {
                result.add(reason);
            }
        }
        return result;
    }

    private String shorten(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelW = 520;
        int panelH = 292;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        context.fill(x, y, x + panelW, y + panelH, 0xD9111117);
        context.fill(x, y, x + panelW, y + 34, 0xEE1D1D28);
        context.fill(x + 306, y + 48, x + 495, y + 132, 0xAA000000);

        context.drawTextWithShadow(textRenderer, Text.literal("Причина наказания"), x + 18, y + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Игрок: §b" + nick), x + 24, y + 48, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Категория: §e" + type.commandName()), x + 318, y + 58, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Время: §e" + (duration.isBlank() ? "без времени" : duration)), x + 318, y + 76, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("В команду уйдёт код/текст причины"), x + 318, y + 98, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal("Быстрые причины"), x + 24, y + 140, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
