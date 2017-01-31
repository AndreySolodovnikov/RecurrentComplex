/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.ivtoolkit.gui.FloatRange;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.TableDirections;
import ivorius.reccomplex.gui.table.GuiTable;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.*;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.utils.scale.Scales;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerRuins;
import net.minecraft.util.EnumFacing;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceBTRuins extends TableDataSourceSegmented
{
    private TransformerRuins transformer;

    public TableDataSourceBTRuins(TransformerRuins transformer, TableNavigator navigator, TableDelegate delegate)
    {
        this.transformer = transformer;

        addManagedSegment(0, new TableDataSourceTransformer(transformer, delegate, navigator));
    }

    public TransformerRuins getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerRuins transformer)
    {
        this.transformer = transformer;
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
            case 1:
                return 5;
            case 2:
                return 4;
            default:
                return super.sizeOfSegment(segment);
        }
    }

    @Override
    public TableCell cellForIndexInSegment(GuiTable table, int index, int segment)
    {
        switch (segment)
        {
            case 1:
                switch (index)
                {
                    case 0:
                        return new TitledCell(new TableCellTitle("decayTitle", IvTranslations.get("reccomplex.transformer.ruins.decay.title")));
                    case 1:
                    {
                        TableCellFloatRange cell = new TableCellFloatRange("decay", new FloatRange(transformer.minDecay, transformer.maxDecay), 0.0f, 1.0f, "%.4f");
                        cell.setScale(Scales.pow(5));
                        cell.addPropertyConsumer(val ->
                        {
                            transformer.minDecay = val.getMin();
                            transformer.maxDecay = val.getMax();
                        });
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.base"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.base.tooltip"));
                    }
                    case 2:
                    {
                        TableCellFloat cell = new TableCellFloat("decayChaos", transformer.decayChaos, 0.0f, 1.0f);
                        cell.setScale(Scales.pow(3));
                        cell.addPropertyConsumer(val -> transformer.decayChaos = val);
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.chaos"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.chaos.tooltip"));
                    }
                    case 3:
                    {
                        TableCellFloat cell = new TableCellFloat("decayValueDensity", transformer.decayValueDensity, 0.0f, 1.0f);
                        cell.setScale(Scales.pow(3));
                        cell.addPropertyConsumer(val -> transformer.decayValueDensity = val);
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.density"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.density.tooltip"));
                    }
                    case 4:
                    {
                        TableCellEnum<EnumFacing> cell = new TableCellEnum<>("decaySide", transformer.decayDirection, TableDirections.getDirectionOptions(EnumFacing.VALUES));
                        cell.addPropertyConsumer(val -> transformer.decayDirection = val);
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.direction"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.direction.tooltip"));
                    }
                }
                break;
            case 2:
                switch (index)
                {
                    case 0:
                        return new TitledCell(new TableCellTitle("otherTitle", IvTranslations.get("reccomplex.transformer.ruins.other.title")));
                    case 1:
                    {
                        TableCellFloat cell = new TableCellFloat("erosion", transformer.blockErosion, 0.0f, 1.0f);
                        cell.setScale(Scales.pow(3));
                        cell.addPropertyConsumer(val -> transformer.blockErosion = val);
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.erosion"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.erosion.tooltip"));
                    }
                    case 2:
                    {
                        TableCellFloat cell = new TableCellFloat("vines", transformer.vineGrowth, 0.0f, 1.0f);
                        cell.setScale(Scales.pow(3));
                        cell.addPropertyConsumer(val -> transformer.vineGrowth = val);
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.vines"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.vines.tooltip"));
                    }
                    case 3:
                    {
                        TableCellBoolean cell = new TableCellBoolean("gravity", transformer.gravity);
                        cell.addPropertyConsumer(val -> transformer.gravity = val);
                        return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.gravity"), cell)
                                .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.gravity.tooltip"));
                    }
                }
                break;
        }

        return super.cellForIndexInSegment(table, index, segment);
    }
}
