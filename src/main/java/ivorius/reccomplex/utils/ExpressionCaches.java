/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.utils;

import net.minecraft.util.text.TextFormatting;

/**
 * Created by lukas on 24.03.15.
 */
public class ExpressionCaches
{
    public static abstract class SimpleVariableType<T> extends PrefixedTypeExpressionCache.VariableType<T>
    {
        public SimpleVariableType(String prefix)
        {
            super(prefix);
        }

        @Override
        public String getRepresentation(String var, Object... args)
        {
            TextFormatting variableColor = isKnown(var, args) ? TextFormatting.GREEN : TextFormatting.YELLOW;
            return TextFormatting.BLUE + prefix + variableColor + var + TextFormatting.RESET;
        }
    }
}
