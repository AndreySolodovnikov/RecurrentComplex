/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure;

import ivorius.reccomplex.gui.TableElementSaveDirectory;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.GuiValidityStateIndicator;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.table.*;
import ivorius.reccomplex.structures.StructureInfos;
import ivorius.reccomplex.structures.StructureRegistry;
import ivorius.reccomplex.structures.generic.GenericStructureInfo;
import ivorius.reccomplex.utils.SaveDirectoryData;

import java.util.Set;

/**
 * Created by lukas on 05.06.14.
 */
public class TableDataSourceGenericStructure extends TableDataSourceSegmented
{
    private GenericStructureInfo structureInfo;
    private String structureKey;

    private SaveDirectoryData saveDirectoryData;

    private TableDelegate tableDelegate;
    private TableNavigator navigator;

    public TableDataSourceGenericStructure(GenericStructureInfo structureInfo, String structureKey, SaveDirectoryData saveDirectoryData, TableDelegate delegate, TableNavigator navigator)
    {
        this.structureInfo = structureInfo;
        this.structureKey = structureKey;
        this.saveDirectoryData = saveDirectoryData;
        this.tableDelegate = delegate;
        this.navigator = navigator;

        addManagedSection(1, new TableDataSourceSupplied(() -> TableElementSaveDirectory.create(saveDirectoryData, () -> structureKey, delegate)));
        addManagedSection(2, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> IvTranslations.get("reccomplex.gui.edit"), () -> IvTranslations.getLines("reccomplex.structure.metadata.tooltip"),
                        () -> new GuiTable(delegate, new TableDataSourceMetadata(structureInfo.metadata))
                ).buildDataSource(IvTranslations.get("reccomplex.structure.metadata")));
        addManagedSection(4, TableDataSourceExpression.constructDefault(IvTranslations.get("reccomplex.structure.dependencies"), structureInfo.dependencies));
        addManagedSection(5, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> IvTranslations.get("reccomplex.gui.edit"), () -> IvTranslations.getLines("reccomplex.structure.generation.tooltip"),
                        () -> new GuiTable(delegate, new TableDataSourceStructureGenerationInfoList(structureInfo.generationInfos, delegate, navigator))
                ).buildDataSource(IvTranslations.get("reccomplex.structure.generation")));
        addManagedSection(6, TableCellMultiBuilder.create(navigator, delegate)
                .addNavigation(() -> IvTranslations.get("reccomplex.gui.edit"), () -> IvTranslations.getLines("reccomplex.structure.transformers.tooltip"),
                        () -> new GuiTable(delegate, new TableDataSourceTransformerList(structureInfo.transformers, delegate, navigator))
                ).buildDataSource(IvTranslations.get("reccomplex.structure.transformers")));
    }

    public GenericStructureInfo getStructureInfo()
    {
        return structureInfo;
    }

    public void setStructureInfo(GenericStructureInfo structureInfo)
    {
        this.structureInfo = structureInfo;
    }

    public String getStructureKey()
    {
        return structureKey;
    }

    public void setStructureKey(String structureKey)
    {
        this.structureKey = structureKey;
    }

    public SaveDirectoryData getSaveDirectoryData()
    {
        return saveDirectoryData;
    }

    public void setSaveDirectoryData(SaveDirectoryData saveDirectoryData)
    {
        this.saveDirectoryData = saveDirectoryData;
    }

    public TableDelegate getTableDelegate()
    {
        return tableDelegate;
    }

    public void setTableDelegate(TableDelegate tableDelegate)
    {
        this.tableDelegate = tableDelegate;
    }

    public TableNavigator getNavigator()
    {
        return navigator;
    }

    public void setNavigator(TableNavigator navigator)
    {
        this.navigator = navigator;
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
            case 0:
                return 1;
            case 3:
                return 1;
        }

        return super.sizeOfSegment(segment);
    }

    @Override
    public TableElement elementForIndexInSegment(GuiTable table, int index, int segment)
    {
        switch (segment)
        {
            case 0:
                if (index == 0)
                {
                    TableCellString cell = new TableCellString(null, structureKey);
                    cell.setTooltip(IvTranslations.formatLines("reccomplex.structure.id.tooltip"));
                    cell.addPropertyConsumer(cell1 -> {
                        structureKey = cell.getPropertyValue();
                        cell.setValidityState(currentNameState());
                        TableElements.reloadExcept(tableDelegate, "structureID");
                    });
                    cell.setShowsValidityState(true);
                    cell.setValidityState(currentNameState());
                    return new TableElementCell("structureID", IvTranslations.get("reccomplex.structure.id"), cell);
                }
            case 3:
            {
                TableCellBoolean cellRotatable = new TableCellBoolean("rotatable", structureInfo.rotatable,
                        IvTranslations.get("reccomplex.structure.rotatable.true"),
                        IvTranslations.get("reccomplex.structure.rotatable.false"));
                cellRotatable.setTooltip(IvTranslations.formatLines("reccomplex.structure.rotatable.tooltip"));
                cellRotatable.addPropertyConsumer(cell -> structureInfo.rotatable = cellRotatable.getPropertyValue());

                TableCellBoolean cellMirrorable = new TableCellBoolean("mirrorable", structureInfo.mirrorable,
                        IvTranslations.format("reccomplex.structure.mirrorable.true"),
                        IvTranslations.format("reccomplex.structure.mirrorable.false"));
                cellMirrorable.setTooltip(IvTranslations.formatLines("reccomplex.structure.mirrorable.tooltip"));
                cellMirrorable.addPropertyConsumer(cell -> structureInfo.mirrorable = cellMirrorable.getPropertyValue());

                return new TableElementCell(new TableCellMulti(cellRotatable, cellMirrorable));
            }
        }

        return super.elementForIndexInSegment(table, index, segment);
    }

    private GuiValidityStateIndicator.State currentNameState()
    {
        return StructureInfos.isSimpleID(structureKey)
                ? StructureRegistry.INSTANCE.allStructureIDs().contains(structureKey)
                ? GuiValidityStateIndicator.State.SEMI_VALID
                : GuiValidityStateIndicator.State.VALID
                : GuiValidityStateIndicator.State.INVALID;
    }
}
