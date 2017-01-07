/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.transformers;

import com.google.gson.*;
import ivorius.ivtoolkit.blocks.*;
import ivorius.ivtoolkit.tools.*;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.gui.editstructure.transformers.TableDataSourceBTRuins;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.datasource.TableDataSource;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.random.BlurredValueField;
import ivorius.reccomplex.utils.NBTStorable;
import ivorius.reccomplex.utils.RCAxisAlignedTransform;
import ivorius.reccomplex.world.gen.feature.structure.context.*;
import net.minecraft.block.BlockSandStone;
import net.minecraft.block.BlockStoneBrick;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.05.14.
 */
public class TransformerRuins extends Transformer<TransformerRuins.InstanceData>
{
    public EnumFacing decayDirection;
    public float minDecay;
    public float maxDecay;
    public float decayChaos;
    public float decayValueDensity;

    public float blockErosion;
    public float vineGrowth;

    public TransformerRuins()
    {
        this(null, EnumFacing.DOWN, 0.0f, 0.9f, 0.3f, 1f / 25.0f, 0.3f, 0.1f);
    }

    public TransformerRuins(@Nullable String id, EnumFacing decayDirection, float minDecay, float maxDecay, float decayChaos, float decayValueDensity, float blockErosion, float vineGrowth)
    {
        super(id != null ? id : randomID(TransformerRuins.class));
        this.decayDirection = decayDirection;
        this.minDecay = minDecay;
        this.maxDecay = maxDecay;
        this.decayChaos = decayChaos;
        this.decayValueDensity = decayValueDensity;
        this.blockErosion = blockErosion;
        this.vineGrowth = vineGrowth;
    }

    private static int getPass(IBlockState state)
    {
        return (state.isNormalCube() || state.getMaterial() == Material.AIR) ? 0 : 1;
    }

    public static void setBlockToAirClean(World world, BlockPos pos)
    {
        TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof IInventory)
        {
            IInventory inventory = (IInventory) tileEntity;
            for (int i = 0; i < inventory.getSizeInventory(); i++)
                inventory.setInventorySlotContents(i, null);
        }

        world.setBlockToAir(pos);
    }

    public static void shuffleArray(Object[] ar, Random rand)
    {
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rand.nextInt(i + 1);

            Object a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    @Override
    public boolean skipGeneration(InstanceData instanceData, StructureLiveContext context, BlockPos pos, IBlockState state, IvWorldData worldData, BlockPos sourcePos)
    {
        if (instanceData.fallingBlocks.contains(sourcePos))
            return true;

        BlurredValueField field = instanceData.blurredValueField;

        if (field == null)
            return false;

        IvBlockCollection blockCollection = worldData.blockCollection;

        double decay = field.getValue(Math.min(sourcePos.getX(), field.getSize()[0]), Math.min(sourcePos.getY(), field.getSize()[1]), Math.min(sourcePos.getZ(), field.getSize()[2]));

        double stability = decayDirection.getFrontOffsetX() * (sourcePos.getX() / (double) blockCollection.getWidth())
                + decayDirection.getFrontOffsetY() * (sourcePos.getY() / (double) blockCollection.getHeight())
                + decayDirection.getFrontOffsetZ() * (sourcePos.getZ() / (double) blockCollection.getLength());
        if (stability < 0) // Negative direction, not special case
            stability += 1;

        return stability < decay;
    }

    @Override
    public void transform(InstanceData instanceData, Phase phase, StructureSpawnContext context, IvWorldData worldData, RunTransformer transformer)
    {
        WorldServer world = context.environment.world;
        IvBlockCollection blockCollection = worldData.blockCollection;
        int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};

        BlockPos lowerCoord = StructureBoundingBoxes.min(context.boundingBox);

        BlockPos.MutableBlockPos dest = new BlockPos.MutableBlockPos(lowerCoord);
        for (BlockPos sourcePos : instanceData.fallingBlocks)
        {
            IvMutableBlockPos.add(RCAxisAlignedTransform.apply(sourcePos, dest, areaSize, context.transform), lowerCoord);

            IBlockState state;
            while (dest.getY() > 0
                    && (state = world.getBlockState(dest)).getBlock().isAir(state, world, dest))
            {
                IvMutableBlockPos.offset(dest, dest, EnumFacing.DOWN);
            }

            IvMutableBlockPos.offset(dest, dest, EnumFacing.UP);
            world.setBlockState(dest, blockCollection.getBlockState(sourcePos), 2);
        }

        if (phase == Phase.AFTER)
        {
            StructureBoundingBox dropAreaBB = context.boundingBox;
            RecurrentComplex.forgeEventHandler.disabledTileDropAreas.add(dropAreaBB);

            if (blockErosion > 0.0f || vineGrowth > 0.0f)
            {
                for (BlockPos sourceCoord : BlockAreas.mutablePositions(blockCollection.area()))
                {
                    BlockPos worldCoord = context.transform.apply(sourceCoord, areaSize).add(StructureBoundingBoxes.min(context.boundingBox));

                    if (context.includes(worldCoord))
                    {
                        IBlockState state = world.getBlockState(worldCoord);

                        if (!transformer.transformer.skipGeneration(transformer.instanceData, context, worldCoord, state, worldData, sourceCoord))
                            decayBlock(world, context.random, state, worldCoord);
                    }
                }
            }

            RecurrentComplex.forgeEventHandler.disabledTileDropAreas.remove(dropAreaBB);
        }
    }

    public void decayBlock(World world, Random random, IBlockState state, BlockPos coord)
    {
        IBlockState newState = state;

        if (random.nextFloat() < blockErosion)
        {
            if (newState.getBlock() == Blocks.STONEBRICK
                    && newState.getProperties().get(BlockStoneBrick.VARIANT) != BlockStoneBrick.EnumType.MOSSY)
                newState = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
            else if (newState.getBlock() == Blocks.SANDSTONE)
                newState = Blocks.SANDSTONE.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.DEFAULT);
        }

        if (random.nextFloat() < vineGrowth)
        {
            if (newState.getBlock() == Blocks.STONEBRICK)
                newState = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.MOSSY);
            else if (newState.getBlock() == Blocks.COBBLESTONE)
                newState = Blocks.MOSSY_COBBLESTONE.getDefaultState();
            else if (newState.getBlock() == Blocks.COBBLESTONE_WALL)
                newState = Blocks.COBBLESTONE_WALL.getDefaultState().withProperty(BlockWall.VARIANT, BlockWall.EnumType.MOSSY);
        }

        if (newState.getBlock() == Blocks.AIR)
        {
            newState = null;
            for (EnumFacing direction : EnumFacing.HORIZONTALS)
            {
                if (random.nextFloat() < vineGrowth && Blocks.VINE.canPlaceBlockOnSide(world, coord, direction))
                {
                    IBlockState downState = world.getBlockState(coord.offset(EnumFacing.DOWN));
                    downState = downState.getBlock() == Blocks.VINE ? downState : Blocks.VINE.getDefaultState();
                    downState = downState.withProperty(BlockVine.getPropertyFor(direction), true);

                    int length = 1 + random.nextInt(MathHelper.floor_float(vineGrowth * 10.0f + 3));
                    for (int y = 0; y < length; y++)
                    {
                        BlockPos downPos = coord.offset(EnumFacing.DOWN, y);
                        if (world.getBlockState(downPos) == Blocks.AIR)
                            world.setBlockState(downPos, downState, 3);
                        else
                            break;
                    }

                    break;
                }
            }
        }

        if (newState != null && state != newState)
            world.setBlockState(coord, newState, 3);
    }

    @Override
    public String getDisplayString()
    {
        return IvTranslations.get("reccomplex.transformer.ruins");
    }

    @Override
    public TableDataSource tableDataSource(TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceBTRuins(this, navigator, delegate);
    }

    @Override
    public InstanceData prepareInstanceData(StructurePrepareContext context, IvWorldData worldData)
    {
        InstanceData instanceData = new InstanceData();

        if (minDecay > 0.0f || maxDecay > 0.0f)
        {
            BlockArea sourceArea = BlockArea.areaFromSize(BlockPos.ORIGIN, StructureBoundingBoxes.size(context.boundingBox));

            float decayChaos = context.random.nextFloat() * this.decayChaos;
            if (this.maxDecay - this.minDecay > decayChaos)
                decayChaos = this.maxDecay - this.minDecay;

            float decayCenter = context.random.nextFloat() * (this.maxDecay - this.minDecay) + this.minDecay;

            int[] blurredFieldSize = BlockAreas.side(sourceArea, decayDirection).areaSize();
            instanceData.blurredValueField = new BlurredValueField(blurredFieldSize);

            int gridCoords = 1;
            for (int d : blurredFieldSize) gridCoords *= d;
            int values = MathHelper.floor_float(gridCoords * decayValueDensity + 0.5f);

            for (int i = 0; i < values; i++)
                instanceData.blurredValueField.addValue(decayCenter + (context.random.nextFloat() - context.random.nextFloat()) * decayChaos * 2.0f, context.random);
        }

        return instanceData;
    }

//    @Override
//    public void configureInstanceData(InstanceData instanceData, StructurePrepareContext context, IvWorldData worldData, RunTransformer transformer)
//    {
//        WorldServer world = context.environment.world;
//        IvBlockCollection blockCollection = worldData.blockCollection;
//        int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
//
//        BlockPos lowerCoord = StructureBoundingBoxes.min(context.boundingBox);
//
//        BlockPos.MutableBlockPos dest = new BlockPos.MutableBlockPos(lowerCoord);
//        for (BlockPos sourcePos : BlockAreas.mutablePositions(blockCollection.area()))
//        {
//            IvMutableBlockPos.add(RCAxisAlignedTransform.apply(sourcePos, dest, areaSize, context.transform), lowerCoord);
//
//            IBlockState state = blockCollection.getBlockState(sourcePos);
//
//            if (!transformer.transformer.skipGeneration(transformer.instanceData, context, dest, state, worldData, sourcePos))
//                instanceData.fallingBlocks.add(sourcePos.toImmutable());
//        }
//    }

    @Override
    public InstanceData loadInstanceData(StructureLoadContext context, NBTBase nbt)
    {
        return new InstanceData(nbt instanceof NBTTagCompound ? (NBTTagCompound) nbt : new NBTTagCompound());
    }

    public static class InstanceData implements NBTStorable
    {
        public BlurredValueField blurredValueField;
        public final Set<BlockPos> fallingBlocks = new HashSet<>();

        public InstanceData()
        {
        }

        public InstanceData(NBTTagCompound compound)
        {
            blurredValueField = compound.hasKey("field", Constants.NBT.TAG_COMPOUND)
                    ? NBTCompoundObjects.read(compound.getCompoundTag("field"), BlurredValueField::new)
                    : null;
            fallingBlocks.addAll(NBTTagLists.intArraysFrom(compound, "fallingBlocks").stream().map(BlockPositions::fromIntArray).collect(Collectors.toList()));
        }

        @Override
        public NBTBase writeToNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();
            if (blurredValueField != null)
                compound.setTag("field", NBTCompoundObjects.write(blurredValueField));
            NBTTagLists.writeIntArraysTo(compound, "fallingBlocks", fallingBlocks.stream().map(BlockPositions::toIntArray).collect(Collectors.toList()));
            return compound;
        }
    }

    public static class Serializer implements JsonDeserializer<TransformerRuins>, JsonSerializer<TransformerRuins>
    {
        private MCRegistry registry;

        public Serializer(MCRegistry registry)
        {
            this.registry = registry;
        }

        @Override
        public TransformerRuins deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonObject = JsonUtils.asJsonObject(jsonElement, "transformerRuins");

            String id = JsonUtils.getString(jsonObject, "id", null);

            EnumFacing decayDirection = Directions.deserialize(JsonUtils.getString(jsonObject, "decayDirection", "DOWN"));
            float minDecay = JsonUtils.getFloat(jsonObject, "minDecay", 0.0f);
            float maxDecay = JsonUtils.getFloat(jsonObject, "maxDecay", 0.9f);
            float decayChaos = JsonUtils.getFloat(jsonObject, "decayChaos", 0.3f);
            float decayValueDensity = JsonUtils.getFloat(jsonObject, "decayValueDensity", 1.0f / 25.0f);

            float blockErosion = JsonUtils.getFloat(jsonObject, "blockErosion", 0.0f);
            float vineGrowth = JsonUtils.getFloat(jsonObject, "vineGrowth", 0.0f);

            return new TransformerRuins(id, decayDirection, minDecay, maxDecay, decayChaos, decayValueDensity, blockErosion, vineGrowth);
        }

        @Override
        public JsonElement serialize(TransformerRuins transformer, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("id", transformer.id());

            jsonObject.addProperty("decayDirection", Directions.serialize(transformer.decayDirection));
            jsonObject.addProperty("minDecay", transformer.minDecay);
            jsonObject.addProperty("maxDecay", transformer.maxDecay);
            jsonObject.addProperty("decayChaos", transformer.decayChaos);
            jsonObject.addProperty("decayValueDensity", transformer.decayValueDensity);

            jsonObject.addProperty("blockErosion", transformer.blockErosion);
            jsonObject.addProperty("vineGrowth", transformer.vineGrowth);

            return jsonObject;
        }
    }
}
