package ru.wqkcpf.moderationhelper.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.gui.StatsScreen;
import ru.wqkcpf.moderationhelper.obs.ObsController;

public final class KeybindManager {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
            Identifier.of(ModerationHelperClient.MOD_ID, "main")
    );

    private static KeyBinding openStats;
    private static KeyBinding stopObs;

    private KeybindManager() {}

    public static void register() {
        openStats = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderation-helper-gui.open_stats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));

        stopObs = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderation-helper-gui.stop_obs",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openStats.wasPressed()) {
                // H opens only stats/recent players. It does not parse chat and does not take screenshots.
                client.setScreen(new StatsScreen());
            }

            while (stopObs.wasPressed()) {
                // Important: when chat is open, pressing G must not stop OBS recording.
                if (MinecraftClient.getInstance().currentScreen instanceof ChatScreen) {
                    continue;
                }
                ObsController.stopRecording();
            }
        });
    }
}
