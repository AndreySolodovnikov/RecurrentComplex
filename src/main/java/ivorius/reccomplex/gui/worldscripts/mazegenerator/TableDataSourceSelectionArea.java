/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.worldscripts.mazegenerator;

import ivorius.ivtoolkit.gui.IntegerRange;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.generic.Selection;
import net.minecraft.util.text.TextFormatting;

/**
* Created by lukas on 08.10.14.
*/
public class TableDataSourceSelectionArea extends TableDataSourceSegmented
{
    private Selection.Area area;

    private int[] dimensions;

    public TableDataSourceSelectionArea(Selection.Area area, int[] dimensions)
    {
        this.area = area;
        this.dimensions = dimensions;
    }

    @Override
    public int numberOfSegments()
    {
        return 2;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        return segment == 1 ? 3 : 1;
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (segment == 0)
        {
            TableCellBoolean cell = new TableCellBoolean("additive", area.isAdditive(),
                    TextFormatting.GREEN + IvTranslations.get("reccomplex.selection.area.additive"),
                    TextFormatting.GOLD + IvTranslations.get("reccomplex.selection.area.subtractive"));
            cell.addPropertyConsumer(area::setAdditive);
            return new TableElementCell(cell);
        }
        else if (segment == 1)
        {
            String title = IvTranslations.get("reccomplex.selection.area.range." + new String[]{"x", "y", "z"}[index]);
            IntegerRange intRange = new IntegerRange(area.getMinCoord()[index], area.getMaxCoord()[index]);
            TableCellIntegerRange cell = new TableCellIntegerRange("area" + index, intRange, 0, dimensions[index] - 1);
            cell.addPropertyConsumer(val -> area.setCoord(index, val.getMin(), val.getMax()));
            return new TableElementCell(title, cell);
        }

        return null;
    }
}
