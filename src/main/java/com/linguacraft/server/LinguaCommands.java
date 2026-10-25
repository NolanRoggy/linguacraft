package com.linguacraft.server;

import com.linguacraft.LinguaCraft;
import com.linguacraft.core.Language;
import com.linguacraft.core.Phrases;
import com.linguacraft.core.Vocab;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /lingua language <spanish|japanese>   switch the language you are learning
 * /lingua practice <true|false>         practice mode: nothing is ever deleted or penalized
 * /lingua test                          ask a random challenge right now
 * /lingua skip                          cancel the current challenge (no penalty)
 * /lingua status                        show language, mode, progress
 */
@EventBusSubscriber(modid = LinguaCraft.MODID)
public final class LinguaCommands {
    private LinguaCommands() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("lingua");

        root.then(Commands.literal("language")
                .then(Commands.argument("language", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            b.suggest("spanish");
                            b.suggest("japanese");
                            return b.buildFuture();
                        })
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            Language l = Language.parse(StringArgumentType.getString(ctx, "language"));
                            if (l == null) {
                                ctx.getSource().sendFailure(Component.literal("Unknown language. Use spanish or japanese."));
                                return 0;
                            }
                            ChallengeManager.cancel(p, true);
                            PlayerData.setLanguage(p, l);
                            ctx.getSource().sendSuccess(() -> Component.literal("Language set to " + l.displayName
                                    + ". First use downloads a speech model (~40-50 MB).").withStyle(ChatFormatting.GREEN), false);
                            return 1;
                        })));

        root.then(Commands.literal("practice")
                .then(Commands.argument("on", BoolArgumentType.bool())
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayerOrException();
                            boolean on = BoolArgumentType.getBool(ctx, "on");
                            PlayerData.setPractice(p, on);
                            ctx.getSource().sendSuccess(() -> Component.literal(on
                                    ? "Practice mode ON: nothing will be deleted."
                                    : "Practice mode OFF: failing deletes items!").withStyle(on ? ChatFormatting.AQUA : ChatFormatting.RED), false);
                            return 1;
                        })));

        root.then(Commands.literal("test").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!ChallengeManager.askRandom(p, true)) {
                ctx.getSource().sendFailure(Component.literal("Couldn't start a challenge (one is already running, or phrases are disabled in the config)."));
                return 0;
            }
            return 1;
        }));

        root.then(Commands.literal("skip").executes(ctx -> {
            ChallengeManager.cancel(ctx.getSource().getPlayerOrException(), true);
            return 1;
        }));

        root.then(Commands.literal("status").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Language l = PlayerData.language(p);
            int known = PlayerData.known(p, l).size();
            ctx.getSource().sendSuccess(() -> Component.literal("Language: " + l.displayName
                    + " | Practice mode: " + (PlayerData.practice(p) ? "on" : "off")
                    + " | Unlocked item words: " + known + "/" + Vocab.size(l)
                    + " | Phrases: " + Phrases.count(l)).withStyle(ChatFormatting.YELLOW), false);
            return 1;
        }));

        event.getDispatcher().register(root);
    }
}
