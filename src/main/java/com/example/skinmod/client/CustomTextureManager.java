package com.example.skinmod.client;

import com.example.skinmod.SkinMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayInputStream;
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
     * Применяет скин из полученных от сервера байтов (PNG).
     * Клиент больше не читает файлы с диска — сервер рассылает содержимое.
     */
    public static void loadAndApplyTexture(UUID playerUUID, String fileName, String modelType, byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            SkinMod.LOGGER.warn("[SkinMod] No skin data received for {}", playerUUID);
            return;
        }
        SkinMod.LOGGER.info("[SkinMod] Applying skin '{}' ({} B) to client player {}", fileName, imageData.length, playerUUID);

        NativeImage nativeImage = null;
        try (ByteArrayInputStream is = new ByteArrayInputStream(imageData)) {
            nativeImage = NativeImage.read(is);
            SkinMod.LOGGER.info("[SkinMod] Decoded PNG for {} ({}x{})", playerUUID, nativeImage.getWidth(), nativeImage.getHeight());
            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

            String safeName = (fileName != null ? fileName : playerUUID.toString())
                    .toLowerCase().replaceAll("[^a-z0-9_.-]", "");

            // Генерируем уникальный ID для каждого применения, чтобы сбросить кэш игры
            ResourceLocation resourceLocation = ResourceLocation.fromNamespaceAndPath("skinmod", "dynamic/" + safeName + "_" + System.currentTimeMillis());

            // Если у игрока уже была кастомная текстура, удаляем её из видеопамяти
            ResourceLocation oldTexture = PLAYER_TEXTURES.get(playerUUID);
            if (oldTexture != null) {
                Minecraft.getInstance().getTextureManager().release(oldTexture);
            }

            Minecraft.getInstance().getTextureManager().register(resourceLocation, dynamicTexture);
            SkinMod.LOGGER.info("[SkinMod] Registered texture {} for {}", resourceLocation, playerUUID);

            PLAYER_TEXTURES.put(playerUUID, resourceLocation);
            PLAYER_MODELS.put(playerUUID, modelType.equals("slim") ? "slim" : "default");
            SkinMod.LOGGER.info("[SkinMod] Custom skin active for {}: {}", playerUUID, resourceLocation);
        } catch (IOException e) {
            SkinMod.LOGGER.error("[SkinMod] Failed to decode skin for {}", playerUUID, e);
            // Освобождаем изображение, если текстура не была зарегистрирована
            if (nativeImage != null) {
                nativeImage.close();
            }
        }
    }

    public static void reset(UUID playerUUID) {
        SkinMod.LOGGER.info("[SkinMod] Resetting custom skin for {}", playerUUID);
        ResourceLocation oldTexture = PLAYER_TEXTURES.remove(playerUUID);
        PLAYER_MODELS.remove(playerUUID);
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
