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
    private final byte[] imageData;

    public SetTexturePacket(UUID targetUUID, String fileName, String modelType, boolean reset, byte[] imageData) {
        this.targetUUID = targetUUID;
        this.fileName = fileName;
        this.modelType = modelType;
        this.reset = reset;
        this.imageData = imageData;
    }

    public static SetTexturePacket createResetPacket(UUID targetUUID) {
        return new SetTexturePacket(targetUUID, "", "default", true, new byte[0]);
    }

    public static void encode(SetTexturePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUUID(packet.targetUUID);
        buffer.writeUtf(packet.fileName);
        buffer.writeUtf(packet.modelType);
        buffer.writeBoolean(packet.reset);
        buffer.writeByteArray(packet.imageData);
    }

    public static SetTexturePacket decode(FriendlyByteBuf buffer) {
        UUID uuid = buffer.readUUID();
        String fileName = buffer.readUtf();
        String modelType = buffer.readUtf();
        boolean reset = buffer.readBoolean();
        byte[] imageData = buffer.readByteArray(2 * 1024 * 1024);
        return new SetTexturePacket(uuid, fileName, modelType, reset, imageData);
    }

    public static void handle(SetTexturePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                if (packet.reset) {
                    CustomTextureManager.reset(packet.targetUUID);
                } else {
                    CustomTextureManager.loadAndApplyTexture(packet.targetUUID, packet.fileName, packet.modelType, packet.imageData);
                }
            });
        });
        context.setPacketHandled(true);
    }
}
