/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockAreas;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.utils.ServerTranslations;
import ivorius.reccomplex.utils.algebra.ExpressionCache;
import ivorius.reccomplex.utils.expression.PositionedBlockMatcher;
import ivorius.ivtoolkit.world.MockWorld;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandSelectWand extends CommandVirtual
{
    @Nonnull
    protected static Stream<BlockPos> sideStream(BlockArea area, EnumFacing direction)
    {
        return StreamSupport.stream(BlockAreas.side(area, direction).spliterator(), false);
    }

    @Override
    public String getCommandName()
    {
        return RCConfig.commandPrefix + "wand";
    }

    @Override
    public String getCommandUsage(ICommandSender var1)
    {
        return ServerTranslations.usage("commands.selectWand.usage");
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MockWorld world, ICommandSender commandSender, String[] args) throws CommandException
    {
        SelectionOwner selectionOwner = RCCommands.getSelectionOwner(commandSender, null, true);
        BlockArea area = selectionOwner.getSelection();

        boolean changed = true;
        int total = 0;

        while (changed)
        {
            changed = false;

            String exp = args.length > 0 ? buildString(args, 0) : "!is:air";
            PositionedBlockMatcher matcher = ExpressionCache.of(new PositionedBlockMatcher(RecurrentComplex.specialRegistry), exp);
            RCCommands.ensureValid(matcher, 0);

            for (EnumFacing direction : EnumFacing.VALUES)
            {
                BlockArea expand;

                while (sideStream((expand = BlockAreas.expand(area, direction, 1)), direction).anyMatch(p -> matcher.test(PositionedBlockMatcher.Argument.at(world, p))) && (total++) < 300)
                {
                    area = expand;
                    changed = true;
                }
            }
        }

        selectionOwner.setSelection(area);
    }
}
