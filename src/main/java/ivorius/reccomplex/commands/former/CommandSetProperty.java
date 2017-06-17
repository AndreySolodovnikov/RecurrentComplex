/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.former;

import ivorius.ivtoolkit.blocks.BlockAreas;
import ivorius.ivtoolkit.world.MockWorld;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.CommandVirtual;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.parameters.CommandExpecting;
import ivorius.reccomplex.commands.parameters.Expect;
import ivorius.reccomplex.commands.parameters.RCExpect;
import ivorius.reccomplex.commands.parameters.RCParameters;
import ivorius.reccomplex.utils.expression.PositionedBlockExpression;
import ivorius.reccomplex.utils.optional.IvOptional;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerProperty;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;

import java.util.stream.Collectors;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandSetProperty extends CommandExpecting implements CommandVirtual
{
    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "property";
    }

    @Override
    public Expect<?> expect()
    {
        return RCExpect.expectRC()
                .next(TransformerProperty.propertyNameStream().collect(Collectors.toSet())).requiredU("key")
                .next(params -> params.get().first().tryGet().map(TransformerProperty::propertyValueStream)).requiredU("value")
                .named("exp").block().optionalU("positioned block expression");
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MockWorld world, ICommandSender commandSender, String[] args) throws CommandException
    {
        RCParameters parameters = RCParameters.of(args, expect()::declare);

        PositionedBlockExpression matcher = new PositionedBlockExpression(RecurrentComplex.specialRegistry);
        IvOptional.ifAbsent(parameters.rc("exp").expression(matcher).optional(), () -> matcher.setExpression(""));

        String propertyName = parameters.get().first().require();
        String propertyValue = parameters.get().at(1).require();

        SelectionOwner selectionOwner = RCCommands.getSelectionOwner(commandSender, null, true);
        RCCommands.assertSize(commandSender, selectionOwner);
        for (BlockPos pos : BlockAreas.mutablePositions(selectionOwner.getSelection()))
        {
            PositionedBlockExpression.Argument at = PositionedBlockExpression.Argument.at(world, pos);
            if (matcher.test(at))
                TransformerProperty.withProperty(at.state, propertyName, propertyValue).ifPresent(state -> world.setBlockState(pos, state, 3));
        }
    }
}
