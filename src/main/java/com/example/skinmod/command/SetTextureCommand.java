package com.example.skinmod.command;

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
                        // /settexture <target> <filename> — модель по умолчанию "default"
                        .executes(context -> executeSetTexture(context, "default"))
                        // /settexture <target> <filename> <model> — явный выбор модели
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

        String normalizedModel = "slim".equalsIgnoreCase(model) ? "slim" : "default";

        SetTexturePacket packet = new SetTexturePacket(target.getUUID(), filename, normalizedModel, false);
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);

        context.getSource().sendSuccess(
            Component.literal("Текстура '" + filename + "' (модель: " + normalizedModel
                    + ") отправлена для игрока " + target.getName().getString()),
            true
        );

        return 1;
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");

        SetTexturePacket packet = SetTexturePacket.createResetPacket(target.getUUID());
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);

        context.getSource().sendSuccess(
            Component.literal("Кастомная текстура сброшена для игрока " + target.getName().getString()),
            true
        );

        return 1;
    }
}
