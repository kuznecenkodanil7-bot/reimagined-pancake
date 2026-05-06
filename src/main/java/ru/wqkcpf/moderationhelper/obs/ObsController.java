package ru.wqkcpf.moderationhelper.obs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ru.wqkcpf.moderationhelper.ModerationHelperClient;
import ru.wqkcpf.moderationhelper.command.CommandSender;
import ru.wqkcpf.moderationhelper.config.ModConfig;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class ObsController {
    private static final AtomicReference<WebSocket> SOCKET = new AtomicReference<>();
    private static final AtomicBoolean IDENTIFIED = new AtomicBoolean(false);
    private static final AtomicBoolean CONNECTING = new AtomicBoolean(false);

    private ObsController() {}

    public static void init() {
        if (ModConfig.get().obsEnabled) {
            ModerationHelperClient.LOGGER.info("OBS integration enabled: {}:{}", ModConfig.get().obsHost, ModConfig.get().obsPort);
        }
    }

    public static void startRecording() {
        RecordingTimer.start();
        if (!ModConfig.get().obsEnabled) {
            CommandSender.notifyLocal("§e[Moderation Helper] OBS выключен в конфиге, таймер запущен локально.");
            return;
        }
        sendRequest("StartRecord");
    }

    public static void stopRecording() {
        RecordingTimer.stop();
        if (!ModConfig.get().obsEnabled) {
            CommandSender.notifyLocal("§e[Moderation Helper] OBS выключен в конфиге, таймер остановлен локально.");
            return;
        }
        sendRequest("StopRecord");
    }

    public static void stopRecordingSilently() {
        RecordingTimer.stop();
        if (ModConfig.get().obsEnabled) sendRequest("StopRecord");
    }

    private static void sendRequest(String requestType) {
        try {
            WebSocket socket = ensureSocket();
            if (socket == null) return;

            JsonObject d = new JsonObject();
            d.addProperty("requestType", requestType);
            d.addProperty("requestId", UUID.randomUUID().toString());

            JsonObject root = new JsonObject();
            root.addProperty("op", 6);
            root.add("d", d);

            socket.sendText(root.toString(), true);
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.warn("OBS request failed: {}", requestType, e);
            CommandSender.notifyLocal("§c[Moderation Helper] OBS недоступен: " + e.getMessage());
        }
    }

    private static WebSocket ensureSocket() throws Exception {
        WebSocket existing = SOCKET.get();
        if (existing != null) return existing;
        if (!CONNECTING.compareAndSet(false, true)) return null;

        try {
            ModConfig.ModConfigData cfg = ModConfig.get();
            URI uri = URI.create("ws://" + cfg.obsHost + ":" + cfg.obsPort);
            WebSocket socket = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .build()
                    .newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(2))
                    .buildAsync(uri, new ObsListener())
                    .get(3, TimeUnit.SECONDS);
            SOCKET.set(socket);
            return socket;
        } finally {
            CONNECTING.set(false);
        }
    }

    private static final class ObsListener implements WebSocket.Listener {
        private final StringBuilder partial = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (!last) return WebSocket.Listener.super.onText(webSocket, data, false);

            String text = partial.toString();
            partial.setLength(0);
            handleMessage(webSocket, text);
            return WebSocket.Listener.super.onText(webSocket, data, true);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            ModerationHelperClient.LOGGER.warn("OBS websocket error", error);
            SOCKET.compareAndSet(webSocket, null);
            IDENTIFIED.set(false);
        }

        private void handleMessage(WebSocket webSocket, String text) {
            try {
                JsonObject root = JsonParser.parseString(text).getAsJsonObject();
                int op = root.has("op") ? root.get("op").getAsInt() : -1;
                if (op == 0) {
                    identify(webSocket, root.getAsJsonObject("d"));
                } else if (op == 2) {
                    IDENTIFIED.set(true);
                    CommandSender.notifyLocal("§a[Moderation Helper] OBS подключён.");
                } else if (op == 7) {
                    // RequestResponse. Kept quiet unless OBS returns an error.
                    JsonObject d = root.getAsJsonObject("d");
                    if (d != null && d.has("requestStatus")) {
                        JsonObject status = d.getAsJsonObject("requestStatus");
                        if (status != null && status.has("result") && !status.get("result").getAsBoolean()) {
                            String comment = status.has("comment") ? status.get("comment").getAsString() : "unknown error";
                            CommandSender.notifyLocal("§c[Moderation Helper] OBS: " + comment);
                        }
                    }
                }
            } catch (Exception e) {
                ModerationHelperClient.LOGGER.debug("Could not parse OBS message: {}", text, e);
            }
        }

        private void identify(WebSocket webSocket, JsonObject helloData) {
            try {
                JsonObject identify = new JsonObject();
                identify.addProperty("rpcVersion", helloData.has("rpcVersion") ? helloData.get("rpcVersion").getAsInt() : 1);

                if (helloData.has("authentication")) {
                    JsonObject auth = helloData.getAsJsonObject("authentication");
                    String salt = auth.get("salt").getAsString();
                    String challenge = auth.get("challenge").getAsString();
                    identify.addProperty("authentication", makeAuth(ModConfig.get().obsPassword, salt, challenge));
                }

                JsonObject root = new JsonObject();
                root.addProperty("op", 1);
                root.add("d", identify);
                webSocket.sendText(root.toString(), true);
            } catch (Exception e) {
                ModerationHelperClient.LOGGER.warn("OBS identification failed", e);
                CommandSender.notifyLocal("§c[Moderation Helper] Ошибка авторизации OBS.");
            }
        }
    }

    private static String makeAuth(String password, String salt, String challenge) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        String secret = Base64.getEncoder().encodeToString(sha256.digest((password + salt).getBytes(StandardCharsets.UTF_8)));
        return Base64.getEncoder().encodeToString(sha256.digest((secret + challenge).getBytes(StandardCharsets.UTF_8)));
    }
}
