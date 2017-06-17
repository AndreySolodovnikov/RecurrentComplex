/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.structure;

import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.ivtoolkit.world.MockWorld;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.commands.CommandSelecting;
import ivorius.reccomplex.commands.CommandVirtual;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.RCTextStyle;
import ivorius.reccomplex.commands.parameters.*;
import ivorius.reccomplex.files.loading.LeveledRegistry;
import ivorius.reccomplex.files.loading.ResourceDirectory;
import ivorius.reccomplex.network.PacketSaveStructureHandler;
import ivorius.reccomplex.utils.RawResourceLocation;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.utils.expression.ResourceExpression;
import ivorius.reccomplex.utils.optional.IvOptional;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.generic.GenericStructure;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

/**
 * Created by lukas on 03.08.14.
 */
public class CommandMapAllStructure extends CommandExpecting
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "mapall";
    }

    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC()
                .virtualCommand()
                .commandArguments(Parameters::get)
                .named("exp").structure()
                .named("directory", "d").resourceDirectory();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);

        ResourceExpression expression = new ResourceExpression(StructureRegistry.INSTANCE::has);
        IvOptional.ifAbsent(parameters.rc("exp").expression(expression).optional(), () -> expression.setExpression(""));

        ResourceDirectory directory = parameters.rc("directory").resourceDirectory().optional().orElse(ResourceDirectory.ACTIVE);

        CommandVirtual virtual = parameters.rc().virtualCommand(server).require();

        int saved = 0, failed = 0, skipped = 0;
        for (String id : StructureRegistry.INSTANCE.ids())
        {
            if (!expression.test(new RawResourceLocation(StructureRegistry.INSTANCE.status(id).getDomain(), id)))
                continue;

            Structure<?> info = StructureRegistry.INSTANCE.get(id);

            if (!(info instanceof GenericStructure))
            {
                skipped++;
                continue;
            }

            GenericStructure structure = (GenericStructure) info;

            IvWorldData worldData = structure.constructWorldData();
            MockWorld world = new MockWorld.WorldData(worldData);

            try
            {
                virtual.execute(world, new CommandSelecting.SelectingSender(commandSender, BlockPos.ORIGIN, worldData.blockCollection.area().getHigherCorner()),
                        parameters.get().move(1).varargs());
            }
            catch (MockWorld.VirtualWorldException ex)
            {
                throw ServerTranslations.commandException("commands.rcmap.nonvirtual.arguments");
            }

            structure.worldDataCompound = worldData.createTagCompound();

            if (PacketSaveStructureHandler.write(commandSender, structure, id, directory, true, false))
                saved++;
            else
                failed++;
        }

        commandSender.sendMessage(ServerTranslations.format("commands.rcmapall.result", saved, RCTextStyle.path(directory), failed, skipped));

        RCCommands.tryReload(RecurrentComplex.loader, LeveledRegistry.Level.CUSTOM);
        RCCommands.tryReload(RecurrentComplex.loader, LeveledRegistry.Level.SERVER);
    }
}
