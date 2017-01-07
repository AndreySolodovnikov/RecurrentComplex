/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.TableDataSourceBlockState;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerPillar;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceBTPillar extends TableDataSourceSegmented
{
    private TransformerPillar transformer;

    public TableDataSourceBTPillar(TransformerPillar transformer, TableNavigator navigator, TableDelegate delegate)
    {
        this.transformer = transformer;

        addManagedSegment(0, new TableDataSourceTransformer(transformer, delegate, navigator));
        addManagedSegment(1, TableDataSourceExpression.constructDefault(IvTranslations.get("reccomplex.gui.sources"), IvTranslations.getLines("reccomplex.transformer.block.source.tooltip"), transformer.sourceMatcher, null));
        addManagedSegment(2, new TableDataSourceBlockState(transformer.destState, state -> transformer.destState = state, navigator, delegate, IvTranslations.get("reccomplex.transformer.pillar.dest.block"), IvTranslations.get("reccomplex.transformer.pillar.dest.metadata")));
    }

    public TransformerPillar getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerPillar transformer)
    {
        this.transformer = transformer;
    }
}
