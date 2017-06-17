/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.structure;

import ivorius.ivtoolkit.blocks.BlockSurfaceArea;
import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.commands.parameters.*;
import ivorius.reccomplex.world.gen.feature.WorldGenStructures;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandDecorate extends CommandExpecting
{
    @Nonnull
    protected static BlockSurfacePos getChunkPos(BlockSurfacePos point)
    {
        return new BlockSurfacePos(point.getX() >> 4, point.getZ() >> 4);
    }

    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "decorate";
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC()
                .xyz().required()
                .xyz().required()
                .named("exp").structurePredicate();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);

        BlockSurfaceArea area = new BlockSurfaceArea(
                parameters.get(0).surfacePos(commandSender.getPosition(), false).require(),
                parameters.get(2).surfacePos(commandSender.getPosition(), false).require()
        );
        BlockSurfaceArea chunkArea = new BlockSurfaceArea(getChunkPos(area.getPoint1()), getChunkPos(area.getPoint2()));
        Predicate<Structure> structurePredicate = parameters.get("exp").structurePredicate().optional().orElse(structureInfo -> true);

        WorldServer world = (WorldServer) commandSender.getEntityWorld();
        chunkArea.forEach(coord -> WorldGenStructures.decorate(world, world.rand, new ChunkPos(coord.x, coord.z), structurePredicate));
    }
}
