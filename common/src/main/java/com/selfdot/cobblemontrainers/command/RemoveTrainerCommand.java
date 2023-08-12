package com.selfdot.cobblemontrainers.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.selfdot.cobblemontrainers.trainer.TrainerRegistry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static com.mojang.brigadier.arguments.StringArgumentType.string;

public class RemoveTrainerCommand implements Command<ServerCommandSource> {

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>
            literal("trainers")
            .then(LiteralArgumentBuilder.<ServerCommandSource>
                literal("remove")
                .then(RequiredArgumentBuilder.<ServerCommandSource, String>
                    argument("name", string())
                        .suggests(new TrainerNameSuggestionProvider())
                        .executes(this)
                )
            )
        );
    }

    @Override
    public int run(CommandContext<ServerCommandSource> ctx) {
        String name = ctx.getArgument("name", String.class);

        if (!TrainerRegistry.getInstance().removeTrainer(name)) {
            ctx.getSource().sendError(Text.literal("Trainer " + name + " does not exist"));
            return -1;
        }
        ctx.getSource().sendMessage(Text.literal("Removed trainer " + name));
        return 1;
    }

}
