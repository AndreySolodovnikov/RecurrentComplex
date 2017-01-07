/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerEnsureBlocks;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceBTEnsureSpace extends TableDataSourceSegmented
{
    private TransformerEnsureBlocks transformer;

    public TableDataSourceBTEnsureSpace(TransformerEnsureBlocks transformer, TableNavigator navigator, TableDelegate delegate)
    {
        this.transformer = transformer;

        addManagedSegment(0, new TableDataSourceTransformer(transformer, delegate, navigator));

        addManagedSegment(1, TableDataSourceExpression.constructDefault(IvTranslations.get("reccomplex.gui.sources"), IvTranslations.getLines("reccomplex.transformer.block.source.tooltip"), transformer.sourceMatcher, null));
        addManagedSegment(2, TableDataSourceExpression.constructDefault(IvTranslations.get("reccomplex.transformer.ensureBlocks.dest"), IvTranslations.getLines("reccomplex.transformer.block.dest.tooltip"), transformer.destMatcher, null));
    }

    public TransformerEnsureBlocks getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerEnsureBlocks transformer)
    {
        this.transformer = transformer;
    }
}
