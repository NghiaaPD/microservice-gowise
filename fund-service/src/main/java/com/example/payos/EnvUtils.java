package com.example.payos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnvUtils {

    private static final Map<String, String> ENV_MAP = new HashMap<>();

    static {
        Path envPath = Paths.get(".env");
        if (!Files.exists(envPath)) {
            envPath = Paths.get("..", ".env");
        }
        if (Files.exists(envPath)) {
            try {
                List<String> lines = Files.readAllLines(envPath);
                for (String rawLine : lines) {
                    if (rawLine == null) {
                        continue;
                    }
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx <= 0) {
                        continue;
                    }
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty() && !ENV_MAP.containsKey(key)) {
                        ENV_MAP.put(key, value);
                    }
                }
            } catch (IOException e) {
            }
        }
    }

    private EnvUtils() {
    }

    public static String get(String key) {
        if (key == null) {
            return null;
        }
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            value = ENV_MAP.get(key);
        }
        return value;
    }

    public static String getOrDefault(String key, String defaultValue) {
        String value = get(key);
        return (value == null || value.isEmpty()) ? defaultValue : value;
    }
}