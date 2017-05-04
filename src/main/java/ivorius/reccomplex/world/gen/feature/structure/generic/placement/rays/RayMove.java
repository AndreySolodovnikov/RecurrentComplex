/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.placement.rays;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.gui.table.cell.TableCellStringInt;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSource;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSupplied;
import ivorius.ivtoolkit.world.WorldCache;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.FactorLimit;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.StructurePlaceContext;

import java.util.OptionalInt;

import static ivorius.reccomplex.world.gen.feature.structure.generic.placement.FactorLimit.getRayRegistry;

/**
 * Created by lukas on 19.09.16.
 */
public class RayMove extends FactorLimit.Ray
{
    public int distance;

    public RayMove()
    {
        this(null, 0);
    }

    public RayMove(Float weight, int distance)
    {
        super(weight);
        this.distance = distance;
    }

    @Override
    public OptionalInt cast(WorldCache cache, StructurePlaceContext context, int y)
    {
        y += distance;
        return y >= 0 && y < cache.world.getHeight() ? OptionalInt.of(y) : OptionalInt.empty();
    }

    @Override
    public String displayString()
    {
        return IvTranslations.format(String.format("reccomplex.placer.factors.limit.rays.%s.title%s", getRayRegistry().iDForType(getClass()), weight != null ? ".mark" : ""), String.valueOf(distance));
    }

    @Override
    public TableDataSource tableDataSource(TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceSegmented(rayTableDataSource(navigator, delegate), new TableDataSourceSupplied(() ->
        {
            TableCellStringInt cell = new TableCellStringInt(null, distance);
            cell.addPropertyConsumer(v -> distance = v);
            return new TitledCell(IvTranslations.get("reccomplex.placer.factors.limit.rays.move.distance"), cell);
        }));
    }
}
