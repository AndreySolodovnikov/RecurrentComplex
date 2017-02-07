/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature;

import ivorius.ivtoolkit.math.IvVecMathHelper;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.reccomplex.world.gen.feature.selector.MixingStructureSelector;
import ivorius.reccomplex.world.gen.feature.selector.NaturalStructureSelector;
import ivorius.reccomplex.world.gen.feature.structure.StructureInfo;
import ivorius.reccomplex.world.gen.feature.structure.StructureInfos;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext;
import ivorius.reccomplex.world.gen.feature.structure.generic.gentypes.NaturalGenerationInfo;
import ivorius.reccomplex.world.gen.feature.structure.generic.gentypes.StaticGenerationInfo;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Created by lukas on 24.05.14.
 */
public class WorldGenStructures
{
    public static void generateStaticStructuresInChunk(Random random, ChunkPos chunkPos, WorldServer world, BlockPos spawnPos, @Nullable Predicate<StructureInfo> structurePredicate)
    {
        StructureRegistry.INSTANCE.getStaticStructuresAt(chunkPos, world, spawnPos).forEach(triple ->
        {
            StaticGenerationInfo staticGenInfo = triple.getMiddle();
            StructureInfo<?> structureInfo = triple.getLeft();
            BlockSurfacePos pos = triple.getRight();

            if (structurePredicate != null && !structurePredicate.test(structureInfo))
                return;

            RecurrentComplex.logger.trace(String.format("Spawning static structure at %s", pos));

            new StructureGenerator<>(structureInfo).world(world).generationInfo(staticGenInfo)
                    .random(random).randomPosition(pos, staticGenInfo.placer.getContents()).fromCenter(true).generate();
        });
    }

    protected static float distance(ChunkPos left, ChunkPos right)
    {
        return MathHelper.sqrt_float(
                (left.chunkXPos - right.chunkXPos) * (left.chunkXPos - right.chunkXPos) +
                        (left.chunkZPos - right.chunkZPos) * (left.chunkZPos - right.chunkZPos));
    }

    public static void generateRandomStructuresInChunk(Random random, ChunkPos chunkPos, WorldServer world, Biome biomeGen, @Nullable Predicate<StructureInfo> structurePredicate)
    {
        MixingStructureSelector<NaturalGenerationInfo, NaturalStructureSelector.Category> structureSelector = StructureRegistry.INSTANCE.naturalStructureSelectors().get(biomeGen, world.provider);

        float distanceToSpawn = distance(new ChunkPos(world.getSpawnPoint()), chunkPos);
        List<Pair<StructureInfo<?>, NaturalGenerationInfo>> generated = structureSelector.generatedStructures(random, world.getBiome(chunkPos.getBlock(0, 0, 0)), world.provider, distanceToSpawn);

        generated.stream()
                .filter(pair -> structurePredicate == null || structurePredicate.test(pair.getLeft()))
                .forEach(pair -> generateStructureInChunk(random, chunkPos, world, pair.getLeft(), pair.getRight()));
    }

    public static boolean generateRandomStructureInChunk(Random random, ChunkPos chunkPos, WorldServer world, Biome biomeGen)
    {
        MixingStructureSelector<NaturalGenerationInfo, NaturalStructureSelector.Category> structureSelector = StructureRegistry.INSTANCE.naturalStructureSelectors().get(biomeGen, world.provider);

        float distanceToSpawn = distance(new ChunkPos(world.getSpawnPoint()), chunkPos);
        Pair<StructureInfo<?>, NaturalGenerationInfo> pair = structureSelector.selectOne(random, world.provider, world.getBiome(chunkPos.getBlock(0, 0, 0)), null, distanceToSpawn);

        if (pair != null)
        {
            generateStructureInChunk(random, chunkPos, world, pair.getLeft(), pair.getRight());
            return true;
        }

        return false;
    }

    protected static void generateStructureInChunk(Random random, ChunkPos chunkPos, WorldServer world, StructureInfo<?> structureInfo, NaturalGenerationInfo naturalGenInfo)
    {
        String structureName = StructureRegistry.INSTANCE.id(structureInfo);

        BlockSurfacePos genPos = new BlockSurfacePos((chunkPos.chunkXPos << 4) + 8 + random.nextInt(16), (chunkPos.chunkZPos << 4) + 8 + random.nextInt(16));

        if (!naturalGenInfo.hasLimitations() || naturalGenInfo.getLimitations().areResolved(world, structureName))
        {
            StructureGenerator<?> generator = new StructureGenerator<>(structureInfo).world(world).generationInfo(naturalGenInfo)
                    .random(random).maturity(StructureSpawnContext.GenerateMaturity.SUGGEST)
                    .randomPosition(genPos, naturalGenInfo.placer.getContents()).fromCenter(true);

            if (naturalGenInfo.getGenerationWeight(world.provider, generator.environment().biome) <= 0)
            {
                RecurrentComplex.logger.trace(String.format("%s failed to spawn at %s (incompatible biome edge)", structureInfo, genPos));
                return;
            }

            boolean didSpawn = generator.generate().isPresent();

            if (!didSpawn)
            {
                if (generator.boundingBox().isPresent())
                    RecurrentComplex.logger.trace(String.format("%s failed to spawn at %s (unknown reason)", structureInfo, genPos));
                else
                    RecurrentComplex.logger.trace(String.format("%s couldn't find a place to spawn at %s (due to its Placer)", structureInfo, genPos));
            }
        }
    }

    public static void generatePartialStructuresInChunk(Random random, final ChunkPos chunkPos, final WorldServer world)
    {
        WorldStructureGenerationData data = WorldStructureGenerationData.get(world);

        data.structureEntriesAt(chunkPos).filter(e -> !e.hasBeenGenerated).forEach(entry -> {
            StructureInfo<?> structureInfo = StructureRegistry.INSTANCE.get(entry.getStructureID());

            if (structureInfo != null)
            {
                new StructureGenerator<>(structureInfo).world(world).generationInfo(entry.generationInfoID)
                        .random(random).boundingBox(entry.boundingBox).transform(entry.transform).generationBB(StructureInfos.chunkBoundingBox(chunkPos))
                        .structureID(entry.getStructureID()).instanceData(entry.instanceData).maturity(entry.firstTime ? StructureSpawnContext.GenerateMaturity.FIRST : StructureSpawnContext.GenerateMaturity.COMPLEMENT).generate();

                if (entry.firstTime)
                {
                    entry.firstTime = false;
                    data.markDirty();
                }
            }
        });
    }

    public static boolean decorate(WorldServer world, Random random, ChunkPos chunkPos, @Nullable Predicate<StructureInfo> structurePredicate)
    {
        boolean worldWantsStructures = world.getWorldInfo().isMapFeaturesEnabled();
        WorldStructureGenerationData data = WorldStructureGenerationData.get(world);

        if (structurePredicate == null)
            generatePartialStructuresInChunk(random, chunkPos, world);

        if ((!RCConfig.honorStructureGenerationOption || worldWantsStructures)
                && (structurePredicate != null || data.checkChunk(chunkPos)))
        {
            Biome biomeGen = world.getBiome(chunkPos.getBlock(8, 0, 8));
            BlockPos spawnPos = world.getSpawnPoint();

            generateStaticStructuresInChunk(random, chunkPos, world, spawnPos, structurePredicate);

            boolean mayGenerate = RCConfig.isGenerationEnabled(biomeGen) && RCConfig.isGenerationEnabled(world.provider);

            if (world.provider.getDimension() == 0)
            {
                double distToSpawn = IvVecMathHelper.distanceSQ(new double[]{chunkPos.chunkXPos * 16 + 8, chunkPos.chunkZPos * 16 + 8}, new double[]{spawnPos.getX(), spawnPos.getZ()});
                mayGenerate &= distToSpawn >= RCConfig.minDistToSpawnForGeneration * RCConfig.minDistToSpawnForGeneration;
            }

            if (mayGenerate)
                generateRandomStructuresInChunk(random, chunkPos, world, biomeGen, structurePredicate);

            return true;
        }

        return false;
    }
}
