/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.commands;

import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.commands.parameters.CommandExpecting;
import ivorius.reccomplex.commands.parameters.Expect;
import ivorius.reccomplex.commands.parameters.RCExpect;
import ivorius.reccomplex.commands.parameters.RCParameters;
import ivorius.reccomplex.files.loading.ResourceDirectory;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

/**
 * Created by lukas on 03.08.14.
 */
public class CommandVisitFiles extends CommandExpecting
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "files";
    }

    public int getRequiredPermissionLevel()
    {
        return 0;
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC()
                .named("directory", "d").resourceDirectory();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);

        ResourceDirectory directory = parameters.rc("directory").resourceDirectory().optional().orElse(ResourceDirectory.ACTIVE);

        OpenGlHelper.openFile(directory.toFile());
    }
}
