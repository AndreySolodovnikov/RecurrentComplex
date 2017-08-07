/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.worldscripts;

import ivorius.reccomplex.block.TileEntityBlockScript;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.TableCellBoolean;
import ivorius.reccomplex.gui.table.cell.TableCellMulti;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSupplied;
import ivorius.reccomplex.gui.worldscripts.multi.TableDataSourceWorldScriptMulti;
import net.minecraft.util.text.TextFormatting;

import javax.annotation.Nonnull;

/**
 * Created by lukas on 06.09.16.
 */
public class TableDataSourceScriptBlock extends TableDataSourceSegmented
{
    public TileEntityBlockScript script;

    public TableDataSourceScriptBlock(TileEntityBlockScript script, TableDelegate delegate, TableNavigator navigator)
    {
        this.script = script;
        addManagedSegment(0, new TableDataSourceSupplied(() ->
        {
            TableCellBoolean spawn = new TableCellBoolean(null, script.spawnTriggerable);
            spawn.addPropertyConsumer(b -> script.spawnTriggerable = b);
            spawn.setTrueTitle(TextFormatting.GREEN + "Spawn");
            spawn.setFalseTitle(TextFormatting.GRAY + "Spawn");

            TableCellBoolean redstone = new TableCellBoolean(null, script.redstoneTriggerable);
            redstone.addPropertyConsumer(b -> script.redstoneTriggerable = b);
            redstone.setTrueTitle(TextFormatting.GREEN + "Redstone");
            redstone.setFalseTitle(TextFormatting.GRAY + "Redstone");
            return new TitledCell("Triggerable", new TableCellMulti(spawn, redstone));
        }));
        addManagedSegment(1, new TableDataSourceWorldScriptMulti(script.script, script.getPos(), delegate, navigator));
    }

    @Nonnull
    @Override
    public String title()
    {
        return "Script Block";
    }
}
