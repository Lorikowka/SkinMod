package com.example.skinmod.client;

import com.example.skinmod.SkinMod;
import com.example.skinmod.ServerSkinManager;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomTextureManager {
    public static final Map<UUID, ResourceLocation> PLAYER_TEXTURES = new HashMap<>();
    public static final Map<UUID, String> PLAYER_MODELS = new HashMap<>();

    public static boolean hasCustomSkin(UUID playerUUID) {
        return PLAYER_TEXTURES.containsKey(playerUUID);
    }

    public static ResourceLocation getCustomSkin(UUID playerUUID) {
        return PLAYER_TEXTURES.get(playerUUID);
    }

    /**
     * Возвращает папку со скинами внутри каталога текущей версии игры:
     * {@code <gameDir>/versions/<versionId>/skins/}.
     * Папка создаётся, если её ещё нет.
     */
    public static File getSkinsDir() {
        Minecraft mc = Minecraft.getInstance();
        String version = mc.getLaunchedVersion();
        if (version == null || version.isEmpty()) {
            version = net.minecraft.SharedConstants.getCurrentVersion().getName();
        }
        File versionDir = new File(FMLPaths.GAMEDIR.get().toFile(), "versions" + File.separator + version);
        File skinsDir = new File(versionDir, "skins");
        if (!skinsDir.exists() && !skinsDir.mkdirs()) {
            SkinMod.LOGGER.warn("Could not create skins directory: {}", skinsDir);
        }
        return skinsDir;
    }

    public static void loadAndApplyTexture(UUID playerUUID, String fileName, String modelType) {
        // Защита от Path Traversal: имя файла должно быть безопасным, а сам файл —
        // находиться строго внутри папки со скинами версии. Не доверяем имени из пакета.
        if (!ServerSkinManager.isValidFilename(fileName)) {
            SkinMod.LOGGER.warn("Rejected unsafe skin filename from server: {}", fileName);
            return;
        }

        File skinsDir = getSkinsDir();
        File textureFile = new File(skinsDir, fileName);
        try {
            if (!textureFile.exists()) {
                SkinMod.LOGGER.warn("Skin texture not found in {}: {}", skinsDir, fileName);
                return;
            }
            // Канонический путь файла должен начинаться с канонического пути папки скинов.
            String canonicalFile = textureFile.getCanonicalPath();
            String canonicalDir = skinsDir.getCanonicalPath() + File.separator;
            if (!canonicalFile.startsWith(canonicalDir)) {
                SkinMod.LOGGER.warn("Rejected skin file outside skins directory: {}", fileName);
                return;
            }
        } catch (IOException e) {
            SkinMod.LOGGER.error("Failed to resolve skin file path: {}", fileName, e);
            return;
        }

        NativeImage nativeImage = null;
        try (FileInputStream is = new FileInputStream(textureFile)) {
            nativeImage = NativeImage.read(is);
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

            String safeName = fileName.toLowerCase().replaceAll("[^a-z0-9_.-]", "");

            // Генерируем уникальный ID для каждого применения, чтобы сбросить кэш игры
            ResourceLocation resourceLocation = new ResourceLocation("skinmod", "dynamic/" + safeName + "_" + System.currentTimeMillis());

            // Если у игрока уже была кастомная текстура, удаляем её из видеопамяти
            ResourceLocation oldTexture = PLAYER_TEXTURES.get(playerUUID);
            if (oldTexture != null) {
                Minecraft.getInstance().getTextureManager().release(oldTexture);
            }

            Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);

            PLAYER_TEXTURES.put(playerUUID, resourceLocation);
            PLAYER_MODELS.put(playerUUID, modelType.equals("slim") ? "slim" : "default");
        } catch (IOException e) {
            SkinMod.LOGGER.error("Failed to load skin texture: {}", fileName, e);
            // Освобождаем изображение, если текстура не была зарегистрирована
            if (nativeImage != null) {
                nativeImage.close();
            }
        }
    }

    public static void reset(UUID playerUUID) {
        ResourceLocation oldTexture = PLAYER_TEXTURES.remove(playerUUID);
        PLAYER_MODELS.remove(playerUUID);

        // Также очищаем старую текстуру при сбросе
        if (oldTexture != null) {
            Minecraft.getInstance().getTextureManager().release(oldTexture);
        }
    }

    /**
     * Полная очистка всех кастомных скинов (вызывается при выходе из мира/сервера,
     * чтобы не было утечки видеопамяти и "призрачных" скинов).
     */
    public static void clearAll() {
        Minecraft mc = Minecraft.getInstance();
        for (ResourceLocation oldTexture : PLAYER_TEXTURES.values()) {
            mc.getTextureManager().release(oldTexture);
        }
        PLAYER_TEXTURES.clear();
        PLAYER_MODELS.clear();
    }
}