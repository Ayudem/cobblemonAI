package com.selfdot.cobblemontrainers.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SetEndCommandCommand extends TrainerCommand {

    @Override
    protected int runSubCommand(CommandContext<ServerCommandSource> context) {
        String endCommand = StringArgumentType.getString(context, "endCommand");
        trainer.setEndCommand(endCommand);
        context.getSource().sendMessage(Text.literal(
            "Set end command for trainer " + trainer.getName() + " to '" + endCommand + "'"
        ));
        return SINGLE_SUCCESS;
    }

}
