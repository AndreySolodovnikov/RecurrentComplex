/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.commands;

import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.capability.RCEntityInfo;
import ivorius.reccomplex.commands.parameters.*;
import ivorius.reccomplex.utils.ServerTranslations;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

/**
 * Created by lukas on 03.08.14.
 */
public class CommandVisual extends CommandExpecting
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "visual";
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC()
                .any("rulers").descriptionU("type").required()
                .any("true", "false").descriptionU("true|false").required();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);
        boolean enabled = parameters.get(1).asBoolean().require();

        EntityPlayer player = getCommandSenderAsPlayer(commandSender);
        RCEntityInfo RCEntityInfo = RCCommands.getStructureEntityInfo(player, null);

        String type = parameters.get(0).require();

        switch (type)
        {
            case "rulers":
                RCEntityInfo.showGrid = enabled;
                RCEntityInfo.sendOptionsToClients(player);
                break;
            default:
                throw ServerTranslations.wrongUsageException("commands.rcvisual.usage");
        }

        if (enabled)
            commandSender.sendMessage(ServerTranslations.format("commands.rcvisual.enabled", type));
        else
            commandSender.sendMessage(ServerTranslations.format("commands.rcvisual.disabled", type));
    }
}
