/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic;

import com.google.gson.*;
import ivorius.ivtoolkit.blocks.BlockAreas;
import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.ivtoolkit.blocks.IvMutableBlockPos;
import ivorius.ivtoolkit.blocks.IvTileEntityHelper;
import ivorius.ivtoolkit.tools.IvWorldData;
import ivorius.ivtoolkit.tools.NBTCompoundObjects;
import ivorius.ivtoolkit.transform.Mover;
import ivorius.ivtoolkit.transform.PosTransformer;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.block.GeneratingTileEntity;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.json.NBTToJson;
import ivorius.reccomplex.temp.RCMover;
import ivorius.reccomplex.temp.RCPosTransformer;
import ivorius.reccomplex.utils.*;
import ivorius.reccomplex.utils.expression.DependencyMatcher;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.VariableDomain;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureLoadContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructurePrepareContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.GenerationType;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.MazeGeneration;
import ivorius.reccomplex.world.gen.feature.structure.generic.generation.NaturalGeneration;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.RunTransformer;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.Transformer;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerMulti;
import ivorius.reccomplex.world.storage.loot.InventoryGenerationHandler;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Created by lukas on 24.05.14.
 */
public class GenericStructure implements Structure<GenericStructure.InstanceData>, Cloneable
{
    public static final int LATEST_VERSION = 3;
    public static final int MAX_GENERATING_LAYERS = 30;

    public final List<GenerationType> generationTypes = new ArrayList<>();
    public TransformerMulti transformer = new TransformerMulti();
    public final DependencyMatcher dependencies = new DependencyMatcher();

    public NBTTagCompound worldDataCompound;

    public boolean rotatable;
    public boolean mirrorable;
    public boolean blocking;

    public GenericVariableDomain variableDomain = new GenericVariableDomain();

    public Metadata metadata = new Metadata();

    public JsonObject customData;

    public static GenericStructure createDefaultStructure()
    {
        GenericStructure genericStructureInfo = new GenericStructure();
        genericStructureInfo.rotatable = true;
        genericStructureInfo.mirrorable = true;
        genericStructureInfo.blocking = true;

        genericStructureInfo.transformer.getData().setPreset("structure");
        genericStructureInfo.generationTypes.add(new NaturalGeneration());

        return genericStructureInfo;
    }

    private static double[] getEntityPos(NBTTagCompound compound)
    {
        NBTTagList pos = compound.getTagList("Pos", Constants.NBT.TAG_DOUBLE);
        return new double[]{pos.getDoubleAt(0), pos.getDoubleAt(1), pos.getDoubleAt(2)};
    }

    protected static void asGeneratingTileEntity(@Nonnull StructureContext context, World world, int[] areaSize, BlockPos origin, NBTTagCompound tileEntityCompound, BiConsumer<BlockPos, GeneratingTileEntity> consumer)
    {
        BlockPos src = RCMover.getTileEntityPos(tileEntityCompound);
        BlockPos dest = context.transform.apply(src, areaSize).add(origin);

        tileEntityCompound = RCMover.setTileEntityPos(tileEntityCompound, dest);

        TileEntity tileEntity = RecurrentComplex.specialRegistry.loadTileEntity(world, tileEntityCompound);
        if (tileEntity instanceof GeneratingTileEntity)
        {
            RCPosTransformer.transformAdditionalData(tileEntity, context.transform, areaSize);
            RCMover.moveAdditionalData(tileEntity, origin);

            consumer.accept(src, (GeneratingTileEntity) tileEntity);
        }
    }

    public static void setBlock(@Nonnull StructureSpawnContext context, int[] areaSize, @Nonnull BlockPos worldPos, @Nonnull IBlockState state, @Nonnull Supplier<NBTTagCompound> tileEntity)
    {
        WorldServer world = context.environment.world;
        if (context.setBlock(worldPos, state, 2))
        {
            NBTTagCompound tileEntityCompound = tileEntity.get(); // Wants to set

            if (tileEntityCompound != null && world.getBlockState(worldPos).getBlock() == state.getBlock())
            {
                TileEntity worldTileEntity = world.getTileEntity(worldPos);

                if (worldTileEntity != null) // Do set
                {
                    tileEntityCompound = RCMover.setTileEntityPos(tileEntityCompound, worldPos);
                    worldTileEntity.readFromNBT(tileEntityCompound);

                    RCPosTransformer.transformAdditionalData(worldTileEntity, context.transform, areaSize);
                    RCMover.setAdditionalDataPos(worldTileEntity, worldPos);

                    generateTileEntityContents(context, worldTileEntity);
                }
            }
        }
    }

    public static void generateEntityContents(@Nonnull StructureSpawnContext context, Entity entity)
    {
        if (!context.generateAsSource && entity instanceof IInventory)
            InventoryGenerationHandler.generateAllTags(context, (IInventory) entity);
    }

    public static void generateTileEntityContents(@Nonnull StructureSpawnContext context, TileEntity tileEntity)
    {
        if (!context.generateAsSource && tileEntity instanceof IInventory)
            InventoryGenerationHandler.generateAllTags(context, (IInventory) tileEntity);
    }

    @Nonnull
    @Override
    public int[] size()
    {
        if (worldDataCompound == null)
            return new int[]{0, 0, 0};

        NBTTagCompound compound = worldDataCompound.getCompoundTag("blockCollection");
        return new int[]{compound.getInteger("width"), compound.getInteger("height"), compound.getInteger("length")};
    }

    @Override
    public boolean isRotatable()
    {
        return rotatable;
    }

    @Override
    public boolean isMirrorable()
    {
        return mirrorable;
    }

    @Override
    public boolean isBlocking()
    {
        return blocking;
    }

    @Override
    public void generate(@Nonnull final StructureSpawnContext context, @Nonnull InstanceData instanceData, @Nonnull TransformerMulti foreignTransformer)
    {
        WorldServer world = context.environment.world;
        IvWorldData worldData = constructWorldData();
        boolean asSource = context.generateAsSource;

        RunTransformer transformer = getRunTransformer(instanceData, foreignTransformer, asSource);

        instanceData.variableDomain.fill(context.environment.variables);

        // The world initializes the block event array after it generates the world - in the constructor
        // This hackily sets the field to a temporary value. Yay.
        RCAccessorWorldServer.ensureBlockEventArray(world); // Hax

        IvBlockCollection blockCollection = worldData.blockCollection;
        int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
        BlockPos origin = StructureBoundingBoxes.min(context.boundingBox);

        Map<BlockPos, GeneratingTileEntity<?>> generatingTEs = new HashMap<>();
        Map<BlockPos, NBTTagCompound> tileEntityCompounds = new HashMap<>();
        for (NBTTagCompound tileEntityCompound : worldData.tileEntities)
        {
            BlockPos src = RCMover.getTileEntityPos(tileEntityCompound);
            tileEntityCompounds.put(src, tileEntityCompound);

            asGeneratingTileEntity(context, world, areaSize, origin, tileEntityCompound, generatingTEs::put);
        }

        if (transformer != null)
            transformer.transformer.transform(transformer.instanceData, Transformer.Phase.BEFORE, context, worldData, transformer);

        StructureBoundingBox relevantSourceArea = BlockAreas.toBoundingBox(blockCollection.area());
        if (context.generationBB != null)
            relevantSourceArea = RCStructureBoundingBoxes.intersection(relevantSourceArea, BlockAreas.toBoundingBox(
                    RCAxisAlignedTransform.apply(RCBlockAreas.sub(RCBlockAreas.from(context.generationBB), origin),
                            RCAxisAlignedTransform.applySize(context.transform, areaSize), RCAxisAlignedTransform.invert(context.transform))));

        if (relevantSourceArea != null) // Why did we get asked to generate again?
        {
            BlockPos.MutableBlockPos worldPos = new BlockPos.MutableBlockPos();
            for (int pass = 0; pass < 2; pass++)
            {
                for (BlockPos sourcePos : RCStructureBoundingBoxes.mutablePositions(relevantSourceArea))
                {
                    IvMutableBlockPos.add(context.transform.applyOn(sourcePos, worldPos, areaSize), origin);

                    // Don't need full context.includes since we already intersect
                    if (context.generationPredicate == null || context.generationPredicate.test(worldPos))
                    {
                        IBlockState state = PosTransformer.transformBlockState(blockCollection.getBlockState(sourcePos), context.transform);

                        if (RecurrentComplex.specialRegistry.isSafe(state.getBlock())
                                && pass == getPass(state) && (transformer == null || !transformer.transformer.skipGeneration(transformer.instanceData, context, worldPos, state, worldData, sourcePos)))
                        {
                            GeneratingTileEntity<?> generatingTE = generatingTEs.get(sourcePos);

                            //noinspection unchecked
                            if (asSource || generatingTE == null || ((GeneratingTileEntity) generatingTE).shouldPlaceInWorld(context, instanceData.tileEntities.get(sourcePos)))
                                setBlock(context, areaSize, worldPos, state, () -> tileEntityCompounds.get(sourcePos));
                            else
                                context.setBlock(worldPos, Blocks.AIR.getDefaultState(), 2); // Replace with air
                        }
                    }
                }
            }
        }

        if (transformer != null)
            transformer.transformer.transform(transformer.instanceData, Transformer.Phase.AFTER, context, worldData, transformer);

        for (NBTTagCompound entityCompound : worldData.entities)
        {
            double[] transformedEntityPos = context.transform.applyOn(getEntityPos(entityCompound), areaSize);
            if (context.includes(new Vec3i(transformedEntityPos[0] + origin.getX(), transformedEntityPos[1] + origin.getY(), transformedEntityPos[2] + origin.getZ())))
            {
                Entity entity = EntityList.createEntityFromNBT(entityCompound, world);

                if (entity != null)
                {
                    PosTransformer.transformEntityPos(entity, context.transform, areaSize);
                    Mover.moveEntity(entity, origin);

                    RCAccessorEntity.setEntityUniqueID(entity, UUID.randomUUID());
                    generateEntityContents(context, entity);
                    world.spawnEntityInWorld(entity);
                }
                else
                {
                    RecurrentComplex.logger.error("Error loading entity with ID '" + entityCompound.getString("id") + "'");
                }
            }
        }

        if (!asSource)
        {
            if (context.generationLayer < MAX_GENERATING_LAYERS)
            {
                generatingTEs.entrySet()
                        .forEach(entry ->
                        {
                            NBTStorable teData = instanceData.tileEntities.get(entry.getKey());
                            if (teData != null) // Otherwise it was added after prepare, or doesn't want to generate
                                //noinspection unchecked
                                ((GeneratingTileEntity) entry.getValue()).generate(context, teData);
                        });
            }
            else
            {
                RecurrentComplex.logger.warn("Structure generated with over " + MAX_GENERATING_LAYERS + " layers; most likely infinite loop!");
            }
        }
    }

    @Nullable
    public RunTransformer getRunTransformer(@Nonnull InstanceData instanceData, @Nonnull TransformerMulti foreignTransformer, boolean asSource)
    {
        if (asSource)
            return null;

        if (instanceData.transformerData == null || instanceData.foreignTransformerData == null)
            throw new IllegalStateException();

        if (!foreignTransformer.isEmpty(instanceData.foreignTransformerData))
        {
            TransformerMulti fused = TransformerMulti.fuse(Arrays.asList(this.transformer, foreignTransformer));
            return new RunTransformer(fused, fused.fuseDatas(Arrays.asList(instanceData.transformerData, instanceData.foreignTransformerData)));
        }

        return new RunTransformer(this.transformer, instanceData.transformerData);
    }

    @Nullable
    @Override
    public InstanceData prepareInstanceData(@Nonnull StructurePrepareContext context, @Nonnull TransformerMulti foreignTransformer)
    {
        InstanceData instanceData = new InstanceData();

        if (!context.generateAsSource)
        {
            IvWorldData worldData = constructWorldData();
            IvBlockCollection blockCollection = worldData.blockCollection;

            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockPos origin = StructureBoundingBoxes.min(context.boundingBox);

            variableDomain.fill(instanceData.variableDomain, context.environment, context.random);
            instanceData.variableDomain.fill(context.environment.variables);

            instanceData.transformerData = this.transformer.prepareInstanceData(context, worldData);
            instanceData.foreignTransformerData = foreignTransformer.prepareInstanceData(context, worldData);

            TransformerMulti transformer = TransformerMulti.fuse(Arrays.asList(this.transformer, foreignTransformer));
            TransformerMulti.InstanceData cInstanceData = transformer.fuseDatas(Arrays.asList(instanceData.transformerData, instanceData.foreignTransformerData));

            RunTransformer runTransformer = new RunTransformer(transformer, cInstanceData);
            transformer.configureInstanceData(cInstanceData, context, worldData, runTransformer);

            if (context.generateMaturity.isSuggest() && !transformer.mayGenerate(cInstanceData, context, worldData, runTransformer))
                return null;

            worldData.tileEntities.forEach(tileEntityCompound ->
            {
                asGeneratingTileEntity(context, IvTileEntityHelper.getAnyWorld(), areaSize, origin, tileEntityCompound, (src, tileEntity) ->
                {
                    NBTStorable tileEntityInstanceData = (NBTStorable) tileEntity.prepareInstanceData(context);
                    if (tileEntityInstanceData != null) // Otherwise, don't generate
                        instanceData.tileEntities.put(src, tileEntityInstanceData);
                });
            });
        }

        return instanceData;
    }

    @Nonnull
    @Override
    public InstanceData loadInstanceData(@Nonnull StructureLoadContext context, @Nonnull final NBTBase nbt, @Nonnull TransformerMulti transformer)
    {
        InstanceData instanceData = new InstanceData();
        instanceData.readFromNBT(context, nbt, this.transformer, transformer, constructWorldData());
        return instanceData;
    }

    public IvWorldData constructWorldData()
    {
        return new IvWorldData(worldDataCompound, RecurrentComplex.specialRegistry.itemHidingMode());
    }

    @Nonnull
    @Override
    public <I extends GenerationType> List<I> generationInfos(@Nonnull Class<? extends I> clazz)
    {
        //noinspection unchecked
        return generationTypes.stream().filter(info -> clazz.isAssignableFrom(info.getClass())).map(info -> (I) info).collect(Collectors.toList());
    }

    @Override
    public GenerationType generationInfo(@Nonnull String id)
    {
        for (GenerationType info : generationTypes)
        {
            if (Objects.equals(info.id(), id))
                return info;
        }

        return null;
    }

    private int getPass(IBlockState state)
    {
        return (state.isNormalCube() || state.getMaterial() == Material.AIR) ? 0 : 1;
    }

    @Override
    @Nonnull
    public GenericStructure copyAsGenericStructureInfo()
    {
        return copy();
    }

    @Override
    public boolean areDependenciesResolved()
    {
        return dependencies.test(RecurrentComplex.saver);
    }

    @Nullable
    @Override
    public IvBlockCollection blockCollection()
    {
        return constructWorldData().blockCollection;
    }

    @Nonnull
    @Override
    public GenericVariableDomain declaredVariables()
    {
        return variableDomain;
    }

    @Override
    public String toString()
    {
        String s = StructureRegistry.INSTANCE.id(this);
        return s != null ? s : "Generic Structure";
    }

    public GenericStructure copy()
    {
        GenericStructure genericStructureInfo = StructureSaveHandler.INSTANCE.fromJSON(StructureSaveHandler.INSTANCE.toJSON(this));
        genericStructureInfo.worldDataCompound = worldDataCompound.copy();
        return genericStructureInfo;
    }

    public static class Serializer implements JsonDeserializer<GenericStructure>, JsonSerializer<GenericStructure>
    {
        public GenericStructure deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonObject = JsonUtils.asJsonObject(jsonElement, "status");
            GenericStructure structureInfo = new GenericStructure();

            Integer version;
            if (jsonObject.has("version"))
            {
                version = JsonUtils.getInt(jsonObject, "version");
            }
            else
            {
                version = LATEST_VERSION;
                RecurrentComplex.logger.warn("Structure JSON missing 'version', using latest (" + LATEST_VERSION + ")");
            }

            if (jsonObject.has("generationInfos"))
                Collections.addAll(structureInfo.generationTypes, context.<GenerationType[]>deserialize(jsonObject.get("generationInfos"), GenerationType[].class));

            if (version == 1)
                structureInfo.generationTypes.add(NaturalGeneration.deserializeFromVersion1(jsonObject, context));

            {
                // Legacy version 2
                if (jsonObject.has("naturalGenerationInfo"))
                    structureInfo.generationTypes.add(NaturalGeneration.getGson().fromJson(jsonObject.get("naturalGenerationInfo"), NaturalGeneration.class));

                if (jsonObject.has("mazeGenerationInfo"))
                    structureInfo.generationTypes.add(MazeGeneration.getGson().fromJson(jsonObject.get("mazeGenerationInfo"), MazeGeneration.class));
            }

            if (jsonObject.has("transformer"))
                structureInfo.transformer = context.deserialize(jsonObject.get("transformer"), TransformerMulti.class);
            else if (jsonObject.has("transformers")) // Legacy
                Collections.addAll(structureInfo.transformer.getTransformers(), context.<Transformer[]>deserialize(jsonObject.get("transformers"), Transformer[].class));
            else if (jsonObject.has("blockTransformers")) // Legacy
                Collections.addAll(structureInfo.transformer.getTransformers(), context.<Transformer[]>deserialize(jsonObject.get("blockTransformers"), Transformer[].class));

            structureInfo.rotatable = JsonUtils.getBoolean(jsonObject, "rotatable", false);
            structureInfo.mirrorable = JsonUtils.getBoolean(jsonObject, "mirrorable", false);
            structureInfo.blocking = JsonUtils.getBoolean(jsonObject, "blocking", true);

            structureInfo.variableDomain = context.deserialize(JsonUtils.getJsonObject(jsonObject, "variableDomain", new JsonObject()), GenericVariableDomain.class);

            if (jsonObject.has("dependencyExpression"))
                structureInfo.dependencies.setExpression(JsonUtils.getString(jsonObject, "dependencyExpression"));
            else if (jsonObject.has("dependencies")) // Legacy
                structureInfo.dependencies.setExpression(DependencyMatcher.ofMods(context.<String[]>deserialize(jsonObject.get("dependencies"), String[].class)));

            if (jsonObject.has("worldData"))
                structureInfo.worldDataCompound = context.deserialize(jsonObject.get("worldData"), NBTTagCompound.class);
            else if (jsonObject.has("worldDataBase64"))
                structureInfo.worldDataCompound = NBTToJson.getNBTFromBase64(JsonUtils.getString(jsonObject, "worldDataBase64"));
            // And else it is taken out for packet size, or stored in the zip

            if (jsonObject.has("metadata")) // Else, use default
                structureInfo.metadata = context.deserialize(jsonObject.get("metadata"), Metadata.class);

            structureInfo.customData = JsonUtils.getJsonObject(jsonObject, "customData", new JsonObject());

            return structureInfo;
        }

        public JsonElement serialize(GenericStructure structureInfo, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("version", LATEST_VERSION);

            jsonObject.add("generationInfos", context.serialize(structureInfo.generationTypes));
            jsonObject.add("transformer", context.serialize(structureInfo.transformer));

            jsonObject.addProperty("rotatable", structureInfo.rotatable);
            jsonObject.addProperty("mirrorable", structureInfo.mirrorable);
            jsonObject.addProperty("blocking", structureInfo.blocking);

            jsonObject.add("variableDomain", context.serialize(structureInfo.variableDomain));

            jsonObject.add("dependencyExpression", context.serialize(structureInfo.dependencies.getExpression()));

            if (!RecurrentComplex.USE_ZIP_FOR_STRUCTURE_FILES && structureInfo.worldDataCompound != null)
            {
                if (RecurrentComplex.USE_JSON_FOR_NBT)
                    jsonObject.add("worldData", context.serialize(structureInfo.worldDataCompound));
                else
                    jsonObject.addProperty("worldDataBase64", NBTToJson.getBase64FromNBT(structureInfo.worldDataCompound));
            }

            jsonObject.add("metadata", context.serialize(structureInfo.metadata));
            jsonObject.add("customData", structureInfo.customData);

            return jsonObject;
        }
    }

    public static class InstanceData implements NBTStorable
    {
        public static final String KEY_TRANSFORMER = "transformer";
        public static final String KEY_FOREIGN_TRANSFORMER = "foreignTransformer";
        public static final String KEY_TILE_ENTITIES = "tileEntities";

        public final VariableDomain variableDomain = new VariableDomain();

        public TransformerMulti.InstanceData transformerData;
        public TransformerMulti.InstanceData foreignTransformerData;
        public final Map<BlockPos, NBTStorable> tileEntities = new HashMap<>();

        protected static NBTBase getTileEntityTag(NBTTagCompound tileEntityCompound, BlockPos coord)
        {
            return tileEntityCompound.getTag(getTileEntityKey(coord));
        }

        private static String getTileEntityKey(BlockPos coord)
        {
            return String.format("%d,%d,%d", coord.getX(), coord.getY(), coord.getZ());
        }

        public void readFromNBT(StructureLoadContext context, NBTBase nbt, TransformerMulti transformer, @Nonnull TransformerMulti foreignTransformer, IvWorldData worldData)
        {
            IvBlockCollection blockCollection = worldData.blockCollection;
            NBTTagCompound compound = nbt instanceof NBTTagCompound ? (NBTTagCompound) nbt : new NBTTagCompound();

            variableDomain.readFromNBT(compound.getCompoundTag("variables"));

            if (compound.hasKey(KEY_TRANSFORMER))
                transformerData = transformer.loadInstanceData(context, compound.getTag(KEY_TRANSFORMER));
            if (compound.hasKey(KEY_FOREIGN_TRANSFORMER))
                foreignTransformerData = foreignTransformer.loadInstanceData(context, compound.getTag(KEY_FOREIGN_TRANSFORMER));

            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockPos origin = StructureBoundingBoxes.min(context.boundingBox);

            NBTTagCompound tileEntitiesCompound = compound.getCompoundTag(InstanceData.KEY_TILE_ENTITIES);
            worldData.tileEntities.forEach(tileEntityCompound ->
            {
                asGeneratingTileEntity(context, IvTileEntityHelper.getAnyWorld(), areaSize, origin, tileEntityCompound, (src, tileEntity) ->
                {
                    tileEntities.put(src, (NBTStorable) (tileEntity.loadInstanceData(context, getTileEntityTag(tileEntitiesCompound, src))));
                });
            });
        }

        @Override
        public NBTBase writeToNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();

            NBTCompoundObjects.writeTo(compound, "variables", variableDomain);

            if (transformerData != null)
                compound.setTag(KEY_TRANSFORMER, transformerData.writeToNBT());
            if (foreignTransformerData != null)
                compound.setTag(KEY_FOREIGN_TRANSFORMER, foreignTransformerData.writeToNBT());

            NBTTagCompound tileEntityCompound = new NBTTagCompound();
            for (Map.Entry<BlockPos, NBTStorable> entry : tileEntities.entrySet())
                tileEntityCompound.setTag(getTileEntityKey(entry.getKey()), entry.getValue().writeToNBT());
            compound.setTag(KEY_TILE_ENTITIES, tileEntityCompound);

            return compound;
        }
    }
}
