package com.example.skinmod.client;

import com.example.skinmod.SkinMod;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CustomTextureManager {

    // UUID игрока -> ResourceLocation зарегистрированной текстуры
    private static final Map<UUID, ResourceLocation> CUSTOM_SKINS = new HashMap<>();

    // UUID игрока -> тип модели ("default" или "slim")
    public static final Map<UUID, String> PLAYER_MODELS = new HashMap<>();

    /**
     * Загружает .png из папки config, регистрирует как DynamicTexture,
     * привязывает результат к UUID игрока и сохраняет тип модели.
     * Должно вызываться ТОЛЬКО в главном потоке клиента.
     */
    public static void loadAndApplyTexture(UUID playerUUID, String filename, String modelType) {
        Path configDir = FMLPaths.CONFIGDIR.get();
        Path texturePath = configDir.resolve(filename);

        if (!Files.exists(texturePath)) {
            System.err.println("[SkinMod] Файл текстуры не найден: " + texturePath);
            return;
        }

        try (InputStream stream = Files.newInputStream(texturePath)) {
            NativeImage nativeImage = NativeImage.read(stream);

            DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);

            ResourceLocation textureLocation = new ResourceLocation(
                    SkinMod.MODID,
                    "custom_skin_" + playerUUID.toString().replace("-", "")
            );

            // Регистрируем (или заменяем) текстуру в TextureManager
            Minecraft.getInstance().getTextureManager().register(textureLocation, dynamicTexture);

            CUSTOM_SKINS.put(playerUUID, textureLocation);

            // Нормализуем и сохраняем тип модели (по умолчанию "default")
            String normalizedModel = ("slim".equalsIgnoreCase(modelType)) ? "slim" : "default";
            PLAYER_MODELS.put(playerUUID, normalizedModel);

            System.out.println("[SkinMod] Текстура для " + playerUUID + " загружена: "
                    + textureLocation + " (модель: " + normalizedModel + ")");
        } catch (IOException e) {
            System.err.println("[SkinMod] Ошибка чтения текстуры: " + e.getMessage());
        }
    }

    /**
     * Сбрасывает кастомную текстуру и модель игрока, возвращая ванильный скин.
     */
    public static void reset(UUID playerUUID) {
        CUSTOM_SKINS.remove(playerUUID);
        PLAYER_MODELS.remove(playerUUID);
        System.out.println("[SkinMod] Кастомная текстура для " + playerUUID + " сброшена.");
    }

    public static ResourceLocation getCustomSkin(UUID playerUUID) {
        return CUSTOM_SKINS.get(playerUUID);
    }

    public static boolean hasCustomSkin(UUID playerUUID) {
        return CUSTOM_SKINS.containsKey(playerUUID);
    }

    public static void removeCustomSkin(UUID playerUUID) {
        CUSTOM_SKINS.remove(playerUUID);
    }
}
