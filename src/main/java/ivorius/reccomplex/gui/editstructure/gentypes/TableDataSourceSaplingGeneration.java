/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.editstructure.gentypes;

import ivorius.ivtoolkit.maze.classic.MazeRoom;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.RCGuiTables;
import ivorius.reccomplex.gui.TableDataSourceBlockPos;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.editstructure.pattern.TableDataSourceBlockPattern;
import ivorius.reccomplex.gui.table.TableCells;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSupplied;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.SaplingGeneration;
import net.minecraft.util.math.BlockPos;

import java.util.function.Function;

/**
 * Created by lukas on 07.10.14.
 */
public class TableDataSourceSaplingGeneration extends TableDataSourceSegmented
{
    private TableNavigator navigator;
    private TableDelegate tableDelegate;

    private SaplingGeneration generationInfo;

    public TableDataSourceSaplingGeneration(SaplingGeneration generationInfo, Function<MazeRoom, BlockPos> realWorldMapper, TableNavigator navigator, TableDelegate tableDelegate)
    {
        this.navigator = navigator;
        this.tableDelegate = tableDelegate;
        this.generationInfo = generationInfo;

        addManagedSegment(0, new TableDataSourceGenerationType(generationInfo, navigator, tableDelegate));
        addManagedSegment(1, new TableDataSourceSupplied(() -> RCGuiTables.defaultWeightElement(val -> generationInfo.generationWeight = TableCells.toDouble(val), generationInfo.generationWeight)));
        addManagedSegment(2, new TableDataSourceBlockPattern(generationInfo.pattern, realWorldMapper, tableDelegate, navigator));
        addManagedSegment(3, TableDataSourceExpression.constructDefault(IvTranslations.get("reccomplex.gui.environment"), generationInfo.environmentExpression, null));
        addManagedSegment(4, new TableDataSourceBlockPos(generationInfo.spawnShift, generationInfo::setSpawnShift, null, null, null,
                IvTranslations.get("reccomplex.generationInfo.vanilla.shift.x"), IvTranslations.get("reccomplex.generationInfo.vanilla.shift.y"), IvTranslations.get("reccomplex.generationInfo.vanilla.shift.z")));
    }
}
