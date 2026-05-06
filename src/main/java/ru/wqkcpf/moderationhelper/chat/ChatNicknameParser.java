package ru.wqkcpf.moderationhelper.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class ChatNicknameParser {
    private static final Pattern COLOR_CODES = Pattern.compile("(?i)(§[0-9A-FK-OR]|&[0-9A-FK-OR])");
    private static final Pattern NICK_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final Set<String> RANKS = Set.of(
            "HT5", "LT5", "HT4", "LT4", "HT3", "LT3", "HT2", "LT2", "HT1", "LT1",
            "RHT3", "RLT3", "RHT2", "RLT2", "RHT1", "RLT1",
            "XHT5", "XLT5", "XHT4", "XLT4", "XHT3", "XLT3", "XHT2", "XLT2", "XHT1", "XLT1",
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    );

    private static final Set<String> SERVER_MARKERS = Set.of(
            "anarchy-alpha",
            "anarchy-beta",
            "anarchy-gamma",
            "anarchy-new",
            "duels"
    );

    private ChatNicknameParser() {}

    public static String parseNick(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) return null;

        List<String> tokens = tokenize(rawMessage);
        if (tokens.isEmpty()) return null;

        for (int i = 0; i < tokens.size(); i++) {
            String token = cleanToken(tokens.get(i));
            String upper = token.toUpperCase(Locale.ROOT);
            String lower = token.toLowerCase(Locale.ROOT);

            // If a server marker stands before the nickname, pick the next valid token.
            if (SERVER_MARKERS.contains(lower)) {
                String next = findNextValidNick(tokens, i + 1);
                if (next != null) return next;
            }

            // If the line starts with a rank, the nickname is the next suitable word.
            if (i == 0 && RANKS.contains(upper)) {
                String next = findNextValidNick(tokens, i + 1);
                if (next != null) return next;
            }

            // Ignore ranks anywhere in the message.
            if (RANKS.contains(upper)) continue;

            if (isValidNick(token) && !looksLikeTechnicalWord(token)) {
                return token;
            }
        }
        return null;
    }

    public static boolean shouldSkipScreenshot(String rawMessage) {
        if (rawMessage == null) return false;
        String lower = rawMessage.toLowerCase(Locale.ROOT);
        return lower.contains("tick speed".toLowerCase(Locale.ROOT))
                || lower.contains("reach")
                || lower.contains("fighting suspiciously".toLowerCase(Locale.ROOT))
                || lower.contains("block interaction".toLowerCase(Locale.ROOT));
    }

    public static boolean isValidNick(String token) {
        return token != null && NICK_PATTERN.matcher(token).matches();
    }

    private static List<String> tokenize(String rawMessage) {
        String noColors = COLOR_CODES.matcher(rawMessage).replaceAll("");
        String normalized = noColors
                .replace('→', ' ')
                .replace('»', ' ')
                .replace('«', ' ')
                .replace(':', ' ')
                .replace(',', ' ')
                .replace(';', ' ')
                .replace('|', ' ')
                .replace('(', ' ')
                .replace(')', ' ')
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('{', ' ')
                .replace('}', ' ')
                .replace('<', ' ')
                .replace('>', ' ');

        List<String> result = new ArrayList<>();
        for (String part : normalized.split("\\s+")) {
            String cleaned = cleanToken(part);
            if (!cleaned.isBlank()) result.add(cleaned);
        }
        return result;
    }

    private static String cleanToken(String token) {
        if (token == null) return "";
        return token.replaceAll("^[^A-Za-z0-9_\\-]+", "")
                .replaceAll("[^A-Za-z0-9_\\-]+$", "")
                .trim();
    }

    private static String findNextValidNick(List<String> tokens, int start) {
        for (int i = start; i < tokens.size(); i++) {
            String candidate = cleanToken(tokens.get(i));
            if (candidate.isBlank()) continue;
            if (RANKS.contains(candidate.toUpperCase(Locale.ROOT))) continue;
            if (SERVER_MARKERS.contains(candidate.toLowerCase(Locale.ROOT))) continue;
            if (isValidNick(candidate)) return candidate;
        }
        return null;
    }

    private static boolean looksLikeTechnicalWord(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        return lower.equals("tick") || lower.equals("speed") || lower.equals("reach") || lower.equals("block") || lower.equals("interaction");
    }
}
