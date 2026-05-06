package ru.wqkcpf.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.wqkcpf.moderationhelper.recent.RecentPlayersManager;
import ru.wqkcpf.moderationhelper.stats.SessionStats;
import ru.wqkcpf.moderationhelper.stats.SessionStats.PunishmentType;

public class StatsScreen extends Screen {
    public StatsScreen() {
        super(Text.literal("Moderation Helper Stats"));
    }

    @Override
    protected void init() {
        int panelW = 420;
        int panelH = 255;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        int i = 0;
        for (String player : RecentPlayersManager.list()) {
            if (i >= 12) break;
            int px = x + 24 + (i % 3) * 120;
            int py = y + 116 + (i / 3) * 24;
            addDrawableChild(ButtonWidget.builder(Text.literal(player), btn -> openRecent(player))
                    .dimensions(px, py, 112, 20).build());
            i++;
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), btn -> close())
                .dimensions(x + panelW - 104, y + panelH - 34, 82, 22).build());
    }

    private void openRecent(String nick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.keyboard != null) {
            mc.keyboard.setClipboard(nick);
        }
        RecentPlayersManager.add(nick);
        if (client != null) client.setScreen(new PunishmentScreen(nick, null));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);

        int panelW = 420;
        int panelH = 255;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        context.fill(x, y, x + panelW, y + panelH, 0xD9111117);
        context.fill(x, y, x + panelW, y + 34, 0xEE1D1D28);
        context.fill(x + 24, y + 48, x + 194, y + 100, 0xAA000000);
        context.fill(x + 216, y + 48, x + 396, y + 100, 0xAA000000);

        context.drawTextWithShadow(textRenderer, Text.literal("Статистика и недавние игроки"), x + 18, y + 12, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Text.literal("warn: " + SessionStats.get(PunishmentType.WARN)), x + 36, y + 58, 0xFFFFDD66);
        context.drawTextWithShadow(textRenderer, Text.literal("mute: " + SessionStats.get(PunishmentType.MUTE)), x + 36, y + 76, 0xFF66CCFF);
        context.drawTextWithShadow(textRenderer, Text.literal("ban: " + SessionStats.get(PunishmentType.BAN)), x + 230, y + 58, 0xFFFF7777);
        context.drawTextWithShadow(textRenderer, Text.literal("ipban: " + SessionStats.get(PunishmentType.IPBAN)), x + 230, y + 76, 0xFFFF55FF);
        context.drawTextWithShadow(textRenderer, Text.literal("Недавние игроки — клик копирует ник и открывает меню"), x + 24, y + 106, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
