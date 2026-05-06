package ru.wqkcpf.moderationhelper;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.wqkcpf.moderationhelper.chat.ChatMessageStore;
import ru.wqkcpf.moderationhelper.config.ModConfig;
import ru.wqkcpf.moderationhelper.keybind.KeybindManager;
import ru.wqkcpf.moderationhelper.obs.ObsController;
import ru.wqkcpf.moderationhelper.obs.RecordingHud;
import ru.wqkcpf.moderationhelper.screenshot.ScreenshotManager;

public class ModerationHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "moderation-helper-gui";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        ModConfig.load();
        ScreenshotManager.initFolders();
        ScreenshotManager.cleanupOldScreenshots();
        ChatMessageStore.registerEvents();
        KeybindManager.register();
        RecordingHud.register();
        ObsController.init();

        LOGGER.info("Moderation Helper GUI loaded");
    }
}
