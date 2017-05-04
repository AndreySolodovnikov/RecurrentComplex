/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure.gentypes;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.RCGuiTables;
import ivorius.reccomplex.gui.editstructure.TableDataSourceBiomeGenList;
import ivorius.reccomplex.gui.editstructure.TableDataSourceDimensionGenList;
import ivorius.reccomplex.gui.editstructure.TableDataSourceNaturalGenLimitation;
import ivorius.reccomplex.gui.editstructure.placer.TableDataSourcePlacer;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.gui.table.cell.TableCell;
import ivorius.reccomplex.gui.table.cell.TableCellEnum;
import ivorius.reccomplex.gui.table.cell.TableCellMultiBuilder;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.world.gen.feature.structure.generic.gentypes.NaturalGeneration;
import ivorius.reccomplex.world.gen.feature.selector.NaturalStructureSelector;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 07.10.14.
 */
public class TableDataSourceNaturalGenerationInfo extends TableDataSourceSegmented
{
    private TableNavigator navigator;
    private TableDelegate tableDelegate;
    private NaturalGeneration generationInfo;

    public TableDataSourceNaturalGenerationInfo(TableNavigator navigator, TableDelegate delegate, NaturalGeneration generationInfo)
    {
        this.navigator = navigator;
        this.tableDelegate = delegate;
        this.generationInfo = generationInfo;

        addManagedSegment(0, new TableDataSourceGenerationInfo(generationInfo, navigator, delegate));

        addManagedSegment(3, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> new TableDataSourcePlacer(generationInfo.placer, delegate, navigator))
                .buildDataSource(IvTranslations.get("reccomplex.placer"), IvTranslations.getLines("reccomplex.placer.tooltip")));

        addManagedSegment(4, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> new TableDataSourceBiomeGenList(generationInfo.biomeWeights, delegate, navigator), () -> IvTranslations.get("reccomplex.gui.biomes"))
                .buildDataSource());

        addManagedSegment(5, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> new TableDataSourceDimensionGenList(generationInfo.dimensionWeights, delegate, navigator), () -> IvTranslations.get("reccomplex.gui.dimensions"))
                .buildDataSource());

        addManagedSegment(6, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> new TableDataSourceNaturalGenLimitation(generationInfo.spawnLimitation, delegate), () -> IvTranslations.get("reccomplex.generationInfo.natural.limitations"))
                .enabled(generationInfo::hasLimitations)
                .addAction(() -> generationInfo.spawnLimitation = generationInfo.hasLimitations() ? null : new NaturalGeneration.SpawnLimitation(), () -> generationInfo.hasLimitations() ? IvTranslations.get("reccomplex.gui.remove") : IvTranslations.get("reccomplex.gui.add"), null
                ).buildDataSource());
    }

    public static List<TableCellEnum.Option<String>> allGenerationCategories()
    {
        Set<String> categories = NaturalStructureSelector.CATEGORY_REGISTRY.activeIDs();
        return categories.stream()
                .map(category -> Pair.of(category, NaturalStructureSelector.CATEGORY_REGISTRY.getActive(category)))
                .filter(p -> p.getRight().selectableInGUI())
                .map(p -> new TableCellEnum.Option<>(p.getLeft(), p.getRight().title(), p.getRight().tooltip()))
                .sorted(Comparator.comparing(o -> o.title))
                .collect(Collectors.toList());
    }

    @Override
    public int numberOfSegments()
    {
        return 7;
    }

    @Override
    public int sizeOfSegment(int segment)
    {
        switch (segment)
        {
            case 1:
            case 2:
                return 1;
        }

        return super.sizeOfSegment(segment);
    }

    @Override
    public TableCell cellForIndexInSegment(GuiTable table, int index, int segment)
    {
        switch (segment)
        {
            case 1:
            {
                TableCellEnum<String> cell = new TableCellEnum<>("category", generationInfo.generationCategory, allGenerationCategories());
                cell.addPropertyConsumer(val -> generationInfo.generationCategory = val);
                return new TitledCell(IvTranslations.get("reccomplex.generationInfo.natural.category"), cell);
            }
            case 2:
                return RCGuiTables.defaultWeightElement(val -> generationInfo.setGenerationWeight(TableCells.toDouble(val)), generationInfo.getGenerationWeight());
        }

        return super.cellForIndexInSegment(table, index, segment);
    }
}
