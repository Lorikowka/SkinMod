package com.example.skinmod;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ServerSkinManager {
    public static final Map<UUID, SkinData> SERVER_SKINS = new HashMap<>();
    private static final File SAVE_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "skinmod_players.json");
    private static final Gson GSON = new Gson();

    public static class SkinData {
        public String fileName;
        public String modelType;

        public SkinData(String fileName, String modelType) {
            this.fileName = fileName;
            this.modelType = modelType;
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            GSON.toJson(SERVER_SKINS, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void load() {
        if (!SAVE_FILE.exists()) return;
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            Type type = new TypeToken<Map<UUID, SkinData>>(){}.getType();
            Map<UUID, SkinData> loaded = GSON.fromJson(reader, type);
            if (loaded != null) SERVER_SKINS.putAll(loaded);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}