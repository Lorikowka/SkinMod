package com.example.skinmod;

import com.example.skinmod.network.ModNetwork;
import com.example.skinmod.network.SetTexturePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = "skinmod")
public class SkinEventHandler {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer joinedPlayer = (ServerPlayer) event.getEntity();

        // 1. Отправляем зашедшему игроку его собственный скин (вместе с данными)
        ServerSkinManager.SkinData myData = ServerSkinManager.SERVER_SKINS.get(joinedPlayer.getUUID());
        if (myData != null) {
            byte[] data = ServerSkinManager.readSkinBytes(myData.fileName);
            if (data != null) {
                ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> joinedPlayer),
                        new SetTexturePacket(joinedPlayer.getUUID(), myData.fileName, myData.modelType, false, data));
            }
        }

        // 2. Сообщаем зашедшему игроку о скинах всех остальных на сервере
        ServerSkinManager.SERVER_SKINS.forEach((uuid, skinData) -> {
            if (!uuid.equals(joinedPlayer.getUUID())) {
                byte[] data = ServerSkinManager.readSkinBytes(skinData.fileName);
                if (data != null) {
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> joinedPlayer),
                            new SetTexturePacket(uuid, skinData.fileName, skinData.modelType, false, data));
                }
            }
        });
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        // Когда игрок (event.getEntity()) начинает видеть другого игрока (event.getTarget())
        if (event.getTarget() instanceof ServerPlayer targetPlayer) {
            ServerSkinManager.SkinData data = ServerSkinManager.SERVER_SKINS.get(targetPlayer.getUUID());
            if (data != null) {
                byte[] imageData = ServerSkinManager.readSkinBytes(data.fileName);
                if (imageData != null) {
                    ServerPlayer trackingPlayer = (ServerPlayer) event.getEntity();
                    ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> trackingPlayer),
                            new SetTexturePacket(targetPlayer.getUUID(), data.fileName, data.modelType, false, imageData));
                }
            }
        }
    }
}
