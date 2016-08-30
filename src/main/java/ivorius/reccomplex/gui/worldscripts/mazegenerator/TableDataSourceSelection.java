/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.worldscripts.mazegenerator;

import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.generic.Selection;
import net.minecraft.util.text.TextFormatting;

import java.util.Arrays;

/**
 * Created by lukas on 04.06.14.
 */
public class TableDataSourceSelection extends TableDataSourceList<Selection.Area, Selection>
{
    private int[] dimensions;

    public TableDataSourceSelection(Selection list, int[] dimensions, TableDelegate tableDelegate, TableNavigator navigator)
    {
        super(list, tableDelegate, navigator);
        this.dimensions = dimensions;
    }

    @Override
    public String getDisplayString(Selection.Area area)
    {
        TextFormatting color = area.isAdditive() ? TextFormatting.GREEN : TextFormatting.RED;
        return String.format("%s%s%s - %s%s", color, Arrays.toString(area.getMinCoord()), TextFormatting.RESET, color, Arrays.toString(area.getMaxCoord()));
    }

    @Override
    public Selection.Area newEntry(String actionID)
    {
        return new Selection.Area(true, new int[dimensions.length], new int[dimensions.length]);
    }

    @Override
    public TableDataSource editEntryDataSource(Selection.Area entry)
    {
        return new TableDataSourceSelectionArea(entry, dimensions);
    }

}
