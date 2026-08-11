package com.example.skinmod.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
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

    public static void loadAndApplyTexture(UUID playerUUID, String fileName, String modelType) {
        File textureFile = new File(FMLPaths.CONFIGDIR.get().toFile(), fileName);
        if (!textureFile.exists()) {
            System.err.println("Текстура не найдена в config: " + fileName);
            return;
        }

        try (FileInputStream is = new FileInputStream(textureFile)) {
            NativeImage nativeImage = NativeImage.read(is);
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
            e.printStackTrace();
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
}