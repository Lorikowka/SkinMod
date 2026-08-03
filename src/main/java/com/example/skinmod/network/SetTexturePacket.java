package com.example.skinmod.network;

import com.example.skinmod.client.CustomTextureManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class SetTexturePacket {

    private final UUID targetUUID;
    private final String fileName;
    private final String modelType;
    private final boolean reset;

    public SetTexturePacket(UUID targetUUID, String fileName, String modelType, boolean reset) {
        this.targetUUID = targetUUID;
        this.fileName = fileName;
        this.modelType = modelType;
        this.reset = reset;
    }

    // Удобный конструктор для команды сброса
    public static SetTexturePacket createResetPacket(UUID targetUUID) {
        return new SetTexturePacket(targetUUID, "", "default", true);
    }

    // Кодирование пакета (сервер -> байты)
    public static void encode(SetTexturePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetUUID);
        buffer.writeUtf(packet.fileName);
        buffer.writeUtf(packet.modelType);
        buffer.writeBoolean(packet.reset);
    }

    // Декодирование пакета (байты -> клиент)
    public static SetTexturePacket decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        String fileName = buffer.readUtf();
        String modelType = buffer.readUtf();
        boolean reset = buffer.readBoolean();
        return new SetTexturePacket(uuid, fileName, modelType, reset);
    }

    // Обработка пакета
    public static void handle(SetTexturePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            // Выполняется в главном потоке клиента
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (packet.reset) {
                    CustomTextureManager.reset(packet.targetUUID);
                } else {
                    CustomTextureManager.loadAndApplyTexture(packet.targetUUID, packet.fileName, packet.modelType);
                }
            });
        });

        context.setPacketHandled(true);
    }
}
