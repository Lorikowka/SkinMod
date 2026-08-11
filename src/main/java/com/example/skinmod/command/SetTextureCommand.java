package com.example.skinmod.command;

import com.example.skinmod.ServerSkinManager;
import com.example.skinmod.network.ModNetwork;
import com.example.skinmod.network.SetTexturePacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class SetTextureCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("settexture")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("filename", StringArgumentType.string())
                                        .executes(context -> executeSetTexture(context, "default"))
                                        .then(Commands.argument("model", StringArgumentType.word())
                                                .executes(context -> executeSetTexture(
                                                        context,
                                                        StringArgumentType.getString(context, "model")
                                                ))
                                        )
                                )
                        )
        );

        dispatcher.register(
                Commands.literal("resettexture")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(SetTextureCommand::executeReset)
                        )
        );
    }

        private static int executeSetTexture(CommandContext<CommandSourceStack> context, String model) throws CommandSyntaxException {
                ServerPlayer target = EntityArgument.getPlayer(context, "target");
                String filename = StringArgumentType.getString(context, "filename");

                // Валидация имени файла — защита от Path Traversal
                if (!isValidFilename(filename)) {
                context.getSource().sendFailure(
                        Component.literal("Некорректное имя файла. Используйте только имя файла с расширением .png")
                );
            return 0;
        }

        String normalizedModel = "slim".equalsIgnoreCase(model) ? "slim" : "default";

        // Отправляем пакет клиентам
        SetTexturePacket packet = new SetTexturePacket(target.getUUID(), filename, normalizedModel, false);
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);

        // Сохраняем новую текстуру в базу сервера, используя правильные переменные
        ServerSkinManager.SERVER_SKINS.put(target.getUUID(), new ServerSkinManager.SkinData(filename, normalizedModel));
        ServerSkinManager.save();

        context.getSource().sendSuccess(Component.literal("Текстура для " + target.getName().getString() + " обновлена (модель: " + normalizedModel + ")."), true);
        return 1;
    }
    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");

        // Удаляем текстуру из базы
        com.example.skinmod.ServerSkinManager.SERVER_SKINS.remove(target.getUUID());
        com.example.skinmod.ServerSkinManager.save();

        // Отправляем пакет на сброс
        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.ALL.noArg(), 
            new SetTexturePacket(target.getUUID(), "", "default", true));

        context.getSource().sendSuccess(Component.literal("Текстура для " + target.getName().getString() + " сброшена на стандартную."), true);
        return 1;
    }

    /**
     * Проверяет, что имя файла безопасно:
     * - без .. (path traversal)
     * - без / и \\ (разделители путей)
     * - заканчивается на .png
     */
    private static boolean isValidFilename(String filename) {
        if (filename == null || filename.isEmpty()) return false;
        if (filename.contains("..")) return false;
        if (filename.contains("/") || filename.contains("\\")) return false;
        return filename.toLowerCase().endsWith(".png");
    }
}
