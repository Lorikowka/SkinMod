package com.example.skinmod.command;

import com.example.skinmod.ServerSkinManager;
import com.example.skinmod.SkinMod;
import com.example.skinmod.network.ModNetwork;
import com.example.skinmod.network.SetTexturePacket;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SetTextureCommand {

    private static final SuggestionProvider<CommandSourceStack> MODEL_SUGGESTIONS = (context, builder) -> {
        builder.suggest("default");
        builder.suggest("slim");
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSourceStack> FILENAME_SUGGESTIONS = (context, builder) -> {
        File dir = ServerSkinManager.getSkinsDir();
        if (dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".png"));
            if (files != null) {
                for (File f : files) {
                    builder.suggest(f.getName());
                }
            }
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("skin")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("filename", StringArgumentType.string())
                                                .suggests(FILENAME_SUGGESTIONS)
                                                .executes(context -> executeSet(context, "default"))
                                                .then(Commands.argument("model", StringArgumentType.word())
                                                        .suggests(MODEL_SUGGESTIONS)
                                                        .executes(context -> executeSet(
                                                                context,
                                                                StringArgumentType.getString(context, "model")
                                                        ))
                                                )
                                        )
                                )
                        )
                        .then(Commands.literal("reset")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(SetTextureCommand::executeReset)
                                )
                        )
                        .then(Commands.literal("list")
                                .executes(context -> executeList(context, null))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> executeList(
                                                context,
                                                EntityArgument.getPlayer(context, "target")
                                        ))
                                )
                        )
        );
    }

    private static int executeSet(CommandContext<CommandSourceStack> context, String model) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");
        String rawFilename = StringArgumentType.getString(context, "filename");

        // Имя файла может быть указано как "steve" или "steve.png" —
        // автоматически добавляем расширение, если его забыли.
        String filename = rawFilename.toLowerCase().endsWith(".png") ? rawFilename : rawFilename + ".png";
        if (!ServerSkinManager.isValidFilename(filename)) {
            context.getSource().sendFailure(
                    Component.translatable("commands.skinmod.set.invalid").withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        String normalizedModel = "slim".equalsIgnoreCase(model) ? "slim" : "default";

        byte[] imageData = ServerSkinManager.readSkinBytes(filename);
        if (imageData == null) {
            context.getSource().sendFailure(
                    Component.translatable("commands.skinmod.set.filenotfound", filename).withStyle(ChatFormatting.RED)
            );
            return 0;
        }

        SkinMod.LOGGER.info("[SkinMod] Sending skin '{}' ({} B, model={}) to all clients for {}",
                filename, imageData.length, normalizedModel, target.getUUID());

        SetTexturePacket packet = new SetTexturePacket(target.getUUID(), filename, normalizedModel, false, imageData);
        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), packet);

        ServerSkinManager.SERVER_SKINS.put(target.getUUID(), new ServerSkinManager.SkinData(filename, normalizedModel));
        ServerSkinManager.save();

        context.getSource().sendSuccess(
                Component.translatable("commands.skinmod.set.success",
                        target.getName().getString(), filename, normalizedModel).withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static int executeReset(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "target");

        ServerSkinManager.SERVER_SKINS.remove(target.getUUID());
        ServerSkinManager.save();

        ModNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(),
                SetTexturePacket.createResetPacket(target.getUUID()));

        context.getSource().sendSuccess(
                Component.translatable("commands.skinmod.reset.success",
                        target.getName().getString()).withStyle(ChatFormatting.GREEN),
                true
        );
        return 1;
    }

    private static int executeList(CommandContext<CommandSourceStack> context, ServerPlayer specific) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (specific != null) {
            ServerSkinManager.SkinData data = ServerSkinManager.SERVER_SKINS.get(specific.getUUID());
            if (data == null) {
                source.sendSuccess(Component.translatable("commands.skinmod.list.player.none",
                        specific.getName().getString()).withStyle(ChatFormatting.YELLOW), false);
            } else {
                source.sendSuccess(Component.translatable("commands.skinmod.list.player",
                        specific.getName().getString(), data.fileName, data.modelType).withStyle(ChatFormatting.GREEN), false);
            }
            return 1;
        }

        if (ServerSkinManager.SERVER_SKINS.isEmpty()) {
            source.sendSuccess(Component.translatable("commands.skinmod.list.empty")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }

        source.sendSuccess(Component.translatable("commands.skinmod.list.header")
                .withStyle(ChatFormatting.GREEN), false);

        for (Map.Entry<UUID, ServerSkinManager.SkinData> entry : ServerSkinManager.SERVER_SKINS.entrySet()) {
            String name = resolveName(source, entry.getKey());
            ServerSkinManager.SkinData data = entry.getValue();
            source.sendSuccess(Component.translatable("commands.skinmod.list.entry",
                    name, data.fileName, data.modelType), false);
        }
        return 1;
    }

    private static String resolveName(CommandSourceStack source, UUID uuid) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(uuid);
        if (online != null) return online.getName().getString();
        return uuid.toString();
    }
}
