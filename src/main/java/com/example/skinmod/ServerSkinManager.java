package com.example.skinmod;

import com.example.skinmod.SkinMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ServerSkinManager {
    public static final Map<UUID, SkinData> SERVER_SKINS = new ConcurrentHashMap<>();
    private static final File SAVE_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "skinmod_players.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type TYPE = new TypeToken<Map<UUID, SkinData>>(){}.getType();

    public static class SkinData {
        public String fileName;
        public String modelType;

        public SkinData(String fileName, String modelType) {
            this.fileName = fileName;
            this.modelType = modelType;
        }
    }

    /**
     * Проверяет безопасность имени файла (защита от Path Traversal).
     * Не допускает разделители путей, ".." и требует расширение .png.
     * Используется как на сервере (команда), так и на клиенте (обработка пакета).
     */
    public static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        if (filename.contains("..")) return false;
        if (filename.contains("/") || filename.contains("\\")) return false;
        if (filename.indexOf(':') >= 0) return false; // защита от Windows-путей вида C:...
        return filename.toLowerCase().endsWith(".png");
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(new HashMap<>(SERVER_SKINS), writer);
        } catch (IOException e) {
            SkinMod.LOGGER.error("Failed to save skin data to {}", SAVE_FILE, e);
        }
    }

    public static void load() {
        if (!SAVE_FILE.exists()) return;
        if (SAVE_FILE.length() == 0) return; // пустой файл — не пытаемся парсить
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            Map<UUID, SkinData> loaded = GSON.fromJson(reader, TYPE);
            if (loaded == null) return;
            SERVER_SKINS.clear();
            for (Map.Entry<UUID, SkinData> entry : loaded.entrySet()) {
                UUID uuid = entry.getKey();
                SkinData data = entry.getValue();
                if (uuid != null && data != null
                        && data.fileName != null && isValidFilename(data.fileName)
                        && data.modelType != null) {
                    SERVER_SKINS.put(uuid, data);
                } else {
                    SkinMod.LOGGER.warn("Skipping invalid skin entry for {}: {}", uuid, data);
                }
            }
            SkinMod.LOGGER.info("Loaded {} skin entries", SERVER_SKINS.size());
        } catch (Exception e) {
            // Gson бросает JsonSyntaxException (runtime) при повреждённом файле —
            // ловим широко, чтобы мод не падал при старте.
            SkinMod.LOGGER.error("Failed to load skin data from {}, starting with empty data", SAVE_FILE, e);
            SERVER_SKINS.clear();
        }
    }
}