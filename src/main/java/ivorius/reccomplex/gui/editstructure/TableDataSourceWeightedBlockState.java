/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.GuiValidityStateIndicator;
import ivorius.reccomplex.gui.RCGuiTables;
import ivorius.reccomplex.gui.TableDataSourceBlockState;
import ivorius.reccomplex.gui.nbt.TableDataSourceNBTTagCompound;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.gui.table.cell.TableCell;
import ivorius.reccomplex.gui.table.cell.TableCellMultiBuilder;
import ivorius.reccomplex.gui.table.cell.TableCellString;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSource;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.world.gen.feature.structure.generic.WeightedBlockState;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceWeightedBlockState extends TableDataSourceSegmented
{
    private WeightedBlockState weightedBlockState;

    public TableDataSourceWeightedBlockState(WeightedBlockState weightedBlockState, TableNavigator navigator, TableDelegate delegate)
    {
        this.weightedBlockState = weightedBlockState;

        addManagedSegment(1, new TableDataSourceBlockState(weightedBlockState.state, state -> weightedBlockState.state = state, navigator, delegate, "Block", "Metadata"));
        addManagedSegment(2, tileEntitySegment(navigator, delegate, () -> weightedBlockState.tileEntityInfo, val -> weightedBlockState.tileEntityInfo = val));
    }

    @Nonnull
    public static TableDataSource tileEntitySegment(final TableNavigator navigator, final TableDelegate delegate, Supplier<NBTTagCompound> supplier, Consumer<NBTTagCompound> consumer)
    {
        return TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> new TableDataSourceNBTTagCompound(delegate, navigator, supplier.get())
                {
                    @Nonnull
                    @Override
                    public String title()
                    {
                        return "Tile Entity";
                    }
                })
                .enabled(() -> supplier.get() != null)
                .addAction(() -> supplier.get() != null ? "Remove" : "Add", null, () ->
                {
                    consumer.accept(supplier.get() != null ? null : new NBTTagCompound());
                    delegate.reloadData();
                })
                .buildDataSource("Tile Entity");
    }

    @Nonnull
    @Override
    public String title()
    {
        return weightedBlockState.state.getBlock().getLocalizedName();
    }

    @Override
    public int numberOfSegments()
    {
        return 3;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        switch (segment)
        {
            case 0:
            default:
                return super.sizeOfSegment(segment);
        }
    }

    @Override
    public TableCell cellForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (segment == 0)
        {
            return RCGuiTables.defaultWeightElement(val -> weightedBlockState.weight = TableCells.toDouble(val), weightedBlockState.weight);
        }

        return super.cellForIndexInSegment(table, index, segment);
    }
}
