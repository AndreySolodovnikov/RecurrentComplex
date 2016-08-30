/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.structures.generic.matchers;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.primitives.Ints;
import ivorius.ivtoolkit.gui.IntegerRange;
import ivorius.ivtoolkit.tools.MCRegistry;
import ivorius.reccomplex.utils.*;
import ivorius.reccomplex.utils.algebra.RCBoolAlgebra;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * Created by lukas on 03.03.15.
 */
public class BlockMatcher extends PrefixedTypeExpressionCache<Boolean> implements Predicate<IBlockState>
{
    public static final String METADATA_PREFIX = "#";

    public BlockMatcher(MCRegistry registry, String expression)
    {
        super(RCBoolAlgebra.algebra(), true, TextFormatting.GREEN + "Any Block", expression);

        addType(new BlockVariableType("", registry));
        addType(new MetadataVariableType(METADATA_PREFIX));
    }

    public static String of(MCRegistry registry, Block block)
    {
        return registry.idFromBlock(block).toString();
    }

    public static String of(MCRegistry registry, Block block, Integer metadata)
    {
        return String.format("%s & %s%d", registry.idFromBlock(block), METADATA_PREFIX, metadata);
    }

    public static String of(MCRegistry registry, Block block, IntegerRange range)
    {
        return String.format("%s & %s%d-%d", registry.idFromBlock(block), METADATA_PREFIX, range.min, range.max);
    }

    @Override
    public boolean apply(final IBlockState input)
    {
        return evaluate(input);
    }

    public static class BlockVariableType extends ExpressionCaches.SimpleVariableType<Boolean>
    {
        public MCRegistry registry;

        public BlockVariableType(String prefix, MCRegistry registry)
        {
            super(prefix);
            this.registry = registry;
        }

        @Override
        public Boolean evaluate(String var, Object... args)
        {
            return ((IBlockState) args[0]).getBlock() == registry.blockFromID(new ResourceLocation(var));
        }

        @Override
        public boolean isKnown(String var, Object... args)
        {
            ResourceLocation location = new ResourceLocation(var); // Since MC defaults to air now
            return registry.blockFromID(location) != Blocks.AIR || location.equals(Block.REGISTRY.getNameForObject(Blocks.AIR));
        }
    }

    public static class MetadataVariableType extends ExpressionCaches.SimpleVariableType<Boolean>
    {
        public MetadataVariableType(String prefix)
        {
            super(prefix);
        }

        public static IntegerRange parseMetadataExp(String var)
        {
            if (var.contains("-"))
            {
                List<String> split = Splitter.on('-').splitToList(var);

                if (split.size() != 2)
                    return null;

                Integer left = parseMetadata(split.get(0));
                Integer right = parseMetadata(split.get(1));

                return left != null && right != null ? new IntegerRange(Math.min(left, right), Math.max(left, right)) : null;
            }

            Integer meta = parseMetadata(var);
            return meta != null ? new IntegerRange(meta, meta) : null;
        }

        public static Integer parseMetadata(String var)
        {
            Integer integer = Ints.tryParse(var);
            return integer != null && integer >= 0 && integer < 16 ? integer : null;
        }

        @Override
        public Boolean evaluate(String var, Object... args)
        {
            IntegerRange range = parseMetadataExp(var);
            int metadata = BlockStates.toMetadata((IBlockState) args[0]);

            return range != null && metadata >= range.min && metadata <= range.max;
        }

        @Override
        public boolean isKnown(String var, Object... args)
        {
            return parseMetadataExp(var) != null;
        }
    }
}
