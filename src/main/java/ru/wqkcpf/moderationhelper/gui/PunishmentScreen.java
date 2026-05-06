package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.command.CommandSender;
import ru.wqkcpf.moderationhelper.config.ModConfig;
import ru.wqkcpf.moderationhelper.obs.ObsController;
import ru.wqkcpf.moderationhelper.recent.RecentPlayersManager;
import ru.wqkcpf.moderationhelper.screenshot.ScreenshotManager;
import ru.wqkcpf.moderationhelper.stats.SessionStats;
import ru.wqkcpf.moderationhelper.stats.SessionStats.PunishmentType;

public class PunishmentScreen extends Screen {
    private final String nick;
    private final ScreenshotManager.ScreenshotTicket screenshotTicket;

    public PunishmentScreen(String nick, ScreenshotManager.ScreenshotTicket screenshotTicket) {
        super(Text.literal("Moderation Helper GUI"));
        this.nick = nick;
        this.screenshotTicket = screenshotTicket;
    }

    @Override
    protected void init() {
        int panelW = 440;
        int panelH = 265;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        int leftX = x + 22;
        int btnW = 132;
        int btnH = 24;
        int gap = 7;

        addDrawableChild(ButtonWidget.builder(Text.literal("Warn"), btn -> openNext(PunishmentType.WARN))
                .dimensions(leftX, y + 54, btnW, btnH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Mute"), btn -> openNext(PunishmentType.MUTE))
                .dimensions(leftX, y + 54 + (btnH + gap), btnW, btnH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ban"), btn -> openNext(PunishmentType.BAN))
                .dimensions(leftX, y + 54 + (btnH + gap) * 2, btnW, btnH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("IPBan"), btn -> openNext(PunishmentType.IPBAN))
                .dimensions(leftX, y + 54 + (btnH + gap) * 3, btnW, btnH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Вызвать на проверку"), btn -> callCheck())
                .dimensions(leftX, y + 54 + (btnH + gap) * 4 + 8, 170, btnH).build());

        int recentX = x + 22;
        int recentY = y + 205;
        int i = 0;
        for (String player : RecentPlayersManager.list()) {
            if (i >= 5) break;
            int bx = recentX + (i % 5) * 78;
            int by = recentY;
            addDrawableChild(ButtonWidget.builder(Text.literal(player), btn -> openRecent(player))
                    .dimensions(bx, by, 74, 20).build());
            i++;
        }
    }

    private void openNext(PunishmentType type) {
        if (type.requiresDuration()) {
            client.setScreen(new DurationScreen(nick, type, screenshotTicket));
        } else {
            client.setScreen(new ReasonScreen(nick, type, "", screenshotTicket));
        }
    }

    private void callCheck() {
        ModConfig.ModConfigData cfg = ModConfig.get();
        CommandSender.sendRawCommand(cfg.checkCommandTemplate.replace("{nick}", nick));
        CommandSender.sendRawCommand(cfg.checkTellTemplate.replace("{nick}", nick));
        ObsController.startRecording();
        CommandSender.notifyLocal("§a[Moderation Helper] Проверка вызвана, таймер записи запущен.");
    }

    private void openRecent(String player) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.keyboard != null) {
            mc.keyboard.setClipboard(player);
        }
        RecentPlayersManager.add(player);
        if (client != null) client.setScreen(new PunishmentScreen(player, null));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelW = 440;
        int panelH = 265;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        context.fill(x, y, x + panelW, y + panelH, 0xD9111117);
        context.fill(x, y, x + panelW, y + 34, 0xEE1D1D28);
        context.fill(x + 285, y + 45, x + 420, y + 158, 0xAA000000);
        context.fill(x + 15, y + 195, x + 425, y + 238, 0x88000000);

        context.drawTextWithShadow(textRenderer, Text.literal("Moderation Helper GUI"), x + 18, y + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("Игрок: §b" + nick), x + 22, y + 38, 0xFFFFFFFF);

        context.drawTextWithShadow(textRenderer, Text.literal("Категории наказаний"), x + 22, y + 180, 0xFFAAAAAA);
        context.drawTextWithShadow(textRenderer, Text.literal("Недавние игроки"), x + 22, y + 196, 0xFFFFFFFF);

        int sx = x + 300;
        int sy = y + 55;
        context.drawTextWithShadow(textRenderer, Text.literal("Статистика"), sx, sy, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("warn: " + SessionStats.get(PunishmentType.WARN)), sx, sy + 20, 0xFFFFDD66);
        context.drawTextWithShadow(textRenderer, Text.literal("mute: " + SessionStats.get(PunishmentType.MUTE)), sx, sy + 36, 0xFF66CCFF);
        context.drawTextWithShadow(textRenderer, Text.literal("ban: " + SessionStats.get(PunishmentType.BAN)), sx, sy + 52, 0xFFFF7777);
        context.drawTextWithShadow(textRenderer, Text.literal("ipban: " + SessionStats.get(PunishmentType.IPBAN)), sx, sy + 68, 0xFFFF55FF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
