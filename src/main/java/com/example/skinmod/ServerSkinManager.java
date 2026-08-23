package com.example.skinmod;

import com.example.skinmod.SkinMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.SharedConstants;
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
    private static final long MAX_SKIN_SIZE = 1024L * 1024L; // 1 MB на файл — защита от огромных пакетов

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

    /**
     * Папка со скинами на стороне сервера:
     * {@code <gameDir>/versions/<versionId>/skins/}.
     * Сервер хранит здесь PNG-файлы и рассылает их содержимое клиентам,
     * поэтому мод работает в мультиплеере без локальных файлов у клиентов.
     */
    public static File getSkinsDir() {
        String version = SharedConstants.getCurrentVersion().getName();
        File dir = new File(FMLPaths.GAMEDIR.get().toFile(),
                "versions" + File.separator + version + File.separator + "skins");
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                SkinMod.LOGGER.info("Created skin storage directory: {}", dir.getAbsolutePath());
            } else {
                SkinMod.LOGGER.warn("Could not create skin storage directory: {}", dir.getAbsolutePath());
            }
        }
        return dir;
    }

    /**
     * Читает байты скина с сервера. Возвращает null при ошибке (нет файла,
     * выход за пределы папки, слишком большой размер, ошибка чтения).
     */
    public static byte[] readSkinBytes(String fileName) {
        if (!isValidFilename(fileName)) return null;
        File skinsDir = getSkinsDir();
        File file = new File(skinsDir, fileName);
        try {
            if (!file.exists()) {
                SkinMod.LOGGER.warn("Skin file not found on server: {}", file);
                return null;
            }
            // Канонический путь файла должен начинаться с канонического пути папки скинов.
            String canonicalFile = file.getCanonicalPath();
            String canonicalDir = skinsDir.getCanonicalPath() + File.separator;
            if (!canonicalFile.startsWith(canonicalDir)) {
                SkinMod.LOGGER.warn("Rejected skin file outside skins directory: {}", fileName);
                return null;
            }
            long size = file.length();
            if (size > MAX_SKIN_SIZE) {
                SkinMod.LOGGER.warn("Skin file too large ({} bytes), limit is {}: {}", size, MAX_SKIN_SIZE, fileName);
                return null;
            }
            try (InputStream in = new FileInputStream(file)) {
                return in.readAllBytes();
            }
        } catch (IOException e) {
            SkinMod.LOGGER.error("Failed to read skin file: {}", fileName, e);
            return null;
        }
    }
}