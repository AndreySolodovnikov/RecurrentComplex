/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.block;

import ivorius.reccomplex.world.gen.feature.structure.context.StructureLoadContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructurePrepareContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext;
import net.minecraft.nbt.NBTBase;

/**
 * Created by lukas on 06.06.14.
 */
public interface GeneratingTileEntity<S>
{
    S prepareInstanceData(StructurePrepareContext context);

    S loadInstanceData(StructureLoadContext context, NBTBase nbt);

    void generate(StructureSpawnContext context, S instanceData);

    boolean shouldPlaceInWorld(StructureSpawnContext context, S instanceData);
}
