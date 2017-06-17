/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.structure;

import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.parameters.RCExpect;
import ivorius.reccomplex.commands.parameters.RCParameters;
import ivorius.reccomplex.operation.OperationRegistry;
import ivorius.reccomplex.utils.RCBlockAreas;
import ivorius.reccomplex.utils.RCStrings;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.world.gen.feature.StructureGenerator;
import ivorius.reccomplex.world.gen.feature.structure.OperationGenerateStructure;
import ivorius.reccomplex.world.gen.feature.structure.Placer;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.generic.GenericStructure;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.GenerationType;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandGenerateStructure extends CommandBase
{
    @Nonnull
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "gen";
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Nonnull
    @Override
    @ParametersAreNonnullByDefault
    public String getCommandUsage(ICommandSender var1)
    {
        return ServerTranslations.usage("commands.strucGen.usage");
    }

    @Override
    @ParametersAreNonnullByDefault
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, "mirror", "select");

        String structureID = parameters.get().first().require();
        Structure<?> structure = parameters.rc().structure().require();
        WorldServer world = parameters.mc("dimension").dimension(server, sender).require();
        AxisAlignedTransform2D transform = parameters.transform("rotation", "mirror").optional().orElse(null);
        GenerationType generationType = parameters.rc("gen").generationType(structure).require();
        BlockSurfacePos pos = parameters.surfacePos("x", "z", sender.getPosition(), false).require();
        String seed = parameters.get("seed").first().optional().orElse(null);
        boolean select = parameters.has("select");

        Placer placer = generationType.placer();

        StructureGenerator<?> generator = new StructureGenerator<>(structure).world(world).generationInfo(generationType)
                .seed(RCStrings.seed(seed))
                .structureID(structureID).randomPosition(pos, placer).fromCenter(true)
                .transform(transform);

        Optional<StructureBoundingBox> boundingBox = generator.boundingBox();
        if (!boundingBox.isPresent())
            throw ServerTranslations.commandException("commands.strucGen.noPlace");

        if (structure instanceof GenericStructure && world == sender.getEntityWorld())
        {
            GenericStructure genericStructureInfo = (GenericStructure) structure;

            BlockPos lowerCoord = StructureBoundingBoxes.min(boundingBox.get());

            OperationRegistry.queueOperation(new OperationGenerateStructure(genericStructureInfo, generationType.id(), generator.transform(), lowerCoord, false)
                    .withSeed(seed)
                    .withStructureID(structureID).prepare(world), sender);
        }
        else
        {
            if (generator.generate() == null)
                throw ServerTranslations.commandException("commands.strucGen.noPlace");
        }

        if (select)
        {
            SelectionOwner owner = RCCommands.getSelectionOwner(sender, null, false);
            owner.setSelection(RCBlockAreas.from(boundingBox.get()));
        }
    }

    @Nonnull
    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        RCParameters parameters = RCParameters.of(args);

        return RCExpect.expectRC()
                .structure()
                .surfacePos("x", "z")
                .named("dimension").dimension()
                .named("gen")
                .next((String[] args1) -> parameters.rc().genericStructure().tryGet()
                        .map(structure -> structure.generationTypes(GenerationType.class).stream().map(GenerationType::id).collect(Collectors.toList()))
                        .orElse(Collections.emptyList()))
                .named("rotation").rotation()
                .named("seed").randomString()
                .flag("mirror")
                .flag("select")
                .get(server, sender, args, pos);
    }
}
