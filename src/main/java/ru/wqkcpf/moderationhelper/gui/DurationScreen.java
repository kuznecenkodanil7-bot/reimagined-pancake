package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.command.CommandSender;
import ru.wqkcpf.moderationhelper.config.ModConfig;
import ru.wqkcpf.moderationhelper.screenshot.ScreenshotManager;
import ru.wqkcpf.moderationhelper.stats.SessionStats.PunishmentType;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class DurationScreen extends Screen {
    private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+)([dh])$|^(permanent|perm|forever)$", Pattern.CASE_INSENSITIVE);

    private final String nick;
    private final PunishmentType type;
    private final ScreenshotManager.ScreenshotTicket screenshotTicket;
    private TextFieldWidget durationField;

    public DurationScreen(String nick, PunishmentType type, ScreenshotManager.ScreenshotTicket screenshotTicket) {
        super(Text.literal("Выбор времени"));
        this.nick = nick;
        this.type = type;
        this.screenshotTicket = screenshotTicket;
    }

    @Override
    protected void init() {
        int panelW = 410;
        int panelH = 230;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        durationField = new TextFieldWidget(textRenderer, x + 24, y + 72, 160, 22, Text.literal("Время"));
        durationField.setMaxLength(24);
        durationField.setText(firstDefaultDuration());
        addDrawableChild(durationField);
        setInitialFocus(durationField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Далее"), btn -> next())
                .dimensions(x + 24, y + 108, 100, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), btn -> client.setScreen(new PunishmentScreen(nick, screenshotTicket)))
                .dimensions(x + 132, y + 108, 100, 24).build());

        int bx = x + 24;
        int by = y + 152;
        int i = 0;
        for (String dur : defaultDurations()) {
            if (i >= 8) break;
            int px = bx + (i % 4) * 82;
            int py = by + (i / 4) * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(dur), btn -> durationField.setText(dur))
                    .dimensions(px, py, 76, 20).build());
            i++;
        }
    }

    private void next() {
        String duration = durationField.getText().trim().toLowerCase(Locale.ROOT);
        if (!isValidDuration(duration)) {
            CommandSender.notifyLocal("§c[Moderation Helper] Время должно быть формата 7d или 12h. Для вечного бана можно permanent.");
            return;
        }
        client.setScreen(new ReasonScreen(nick, type, duration, screenshotTicket));
    }

    private boolean isValidDuration(String duration) {
        return duration != null && DURATION_PATTERN.matcher(duration).matches();
    }

    private String firstDefaultDuration() {
        return defaultDurations().stream().findFirst().orElse("1d");
    }

    private Set<String> defaultDurations() {
        Set<String> result = new LinkedHashSet<>();
        for (ModConfig.QuickReasonConfig reason : ModConfig.get().quickReasons) {
            if (reason.type != null && reason.type.equalsIgnoreCase(type.name())
                    && reason.defaultDuration != null && !reason.defaultDuration.isBlank()) {
                result.add(reason.defaultDuration.trim());
            }
        }
        if (result.isEmpty()) {
            result.add("1h");
            result.add("12h");
            result.add("1d");
            result.add("7d");
        }
        return result;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelW = 410;
        int panelH = 230;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        context.fill(x, y, x + panelW, y + panelH, 0xD9111117);
        context.fill(x, y, x + panelW, y + 34, 0xEE1D1D28);

        context.drawTextWithShadow(textRenderer, Text.literal("Выбор времени: §b" + nick), x + 18, y + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Наказание: §e" + type.commandName()), x + 24, y + 48, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Формат: 7d = 7 дней, 12h = 12 часов"), x + 24, y + 138, 0xFFAAAAAA);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
