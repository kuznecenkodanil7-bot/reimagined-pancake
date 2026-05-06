package ru.wqkcpf.moderationhelper.chat;

import net.minecraft.client.MinecraftClient;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class ChatMessageExtractor {
    private ChatMessageExtractor() {}

    /**
     * Exact extraction from ChatHud is unstable across Yarn builds. This method first tries reflection
     * against common fields/methods and then falls back to the newest captured chat message.
     */
    public static String extractMessageUnderMouse(MinecraftClient client, double mouseX, double mouseY) {
        try {
            Object chatHud = client.inGameHud.getChatHud();

            // Some mappings expose something like getMessageAt / getTextStyleAt.
            for (String methodName : new String[]{"getMessageAt", "method_7191"}) {
                try {
                    Method method = chatHud.getClass().getDeclaredMethod(methodName, double.class, double.class);
                    method.setAccessible(true);
                    Object result = method.invoke(chatHud, mouseX, mouseY);
                    if (result != null) return result.toString();
                } catch (NoSuchMethodException ignored) {
                    // Try next name.
                }
            }

            // Reflection fallback for visible message lists. OrderedText does not always expose raw text,
            // so this branch is only a bonus and may still return a debug-looking string.
            for (String fieldName : new String[]{"visibleMessages", "field_2064"}) {
                try {
                    Field field = chatHud.getClass().getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(chatHud);
                    if (value instanceof List<?> list && !list.isEmpty()) {
                        Object first = list.get(0);
                        if (first != null) return first.toString();
                    }
                } catch (NoSuchFieldException ignored) {
                    // Try next name.
                }
            }
        } catch (Throwable t) {
            ModerationHelperClient.LOGGER.debug("Could not extract exact chat message under mouse, using fallback", t);
        }
        return ChatMessageStore.bestCandidate();
    }
}
