/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.GuiValidityStateIndicator;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.gui.table.cell.*;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.world.gen.feature.structure.Structures;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.Transformer;

import javax.annotation.Nonnull;

/**
 * Created by lukas on 29.08.16.
 */
public class TableDataSourceTransformer extends TableDataSourceSegmented
{
    public Transformer transformer;

    public TableDelegate delegate;

    public TableDataSourceTransformer(Transformer transformer, TableDelegate delegate, TableNavigator navigator)
    {
        this.transformer = transformer;

        this.delegate = delegate;
    }

    @Override
    public int numberOfSegments()
    {
        return 1;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        return segment == 0 ? 1 : super.sizeOfSegment(segment);
    }

    @Override
    public TableCell cellForIndexInSegment(GuiTable table, int index, int segment)
    {
        if (segment == 0)
        {
            TableCellString idCell = new TableCellString("transformerID", transformer.id());
            idCell.setShowsValidityState(true);
            idCell.setValidityState(currentIDState());
            idCell.addPropertyConsumer(val ->
            {
                transformer.setID(val);
                idCell.setValidityState(currentIDState());
            });

            TableCellButton randomizeCell = new TableCellButton(null, null, IvTranslations.get("reccomplex.gui.randomize.short"), IvTranslations.getLines("reccomplex.gui.randomize"));
            randomizeCell.addAction(() -> {
                transformer.setID(Transformer.randomID(transformer.getClass()));
                delegate.reloadData();
            });

            TableCellMulti cell = new TableCellMulti(idCell, randomizeCell);
            cell.setSize(1, 0.1f);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.id"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.id.tooltip"));
        }

        return super.cellForIndexInSegment(table, index, segment);
    }

    protected GuiValidityStateIndicator.State currentIDState()
    {
        return Structures.isSimpleIDState(transformer.id());
    }

    @Nonnull
    @Override
    public String title()
    {
        return transformer.getDisplayString();
    }
}
