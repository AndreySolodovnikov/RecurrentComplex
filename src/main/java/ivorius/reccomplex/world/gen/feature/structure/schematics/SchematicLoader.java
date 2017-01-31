/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.schematics;

import ivorius.ivtoolkit.tools.IvFileHelper;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.files.loading.ResourceDirectory;
import ivorius.reccomplex.utils.RCAccessorNBT;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Created by lukas on 29.09.14.
 */
public class SchematicLoader
{
    public static void initializeFolder()
    {
        getValidatedSchematicsFile();
    }

    public static SchematicFile loadSchematicByName(String name) throws SchematicFile.UnsupportedSchematicFormatException
    {
        if (FilenameUtils.getExtension(name).length() == 0)
            name = name + ".schematic";

        for (File file : currentSchematicFiles())
        {
            if (file.getPath().endsWith(name) && name.endsWith(file.getName()))
                return loadSchematicFromFile(file);
        }

        return null;
    }

    public static SchematicFile loadSchematicFromFile(File file) throws SchematicFile.UnsupportedSchematicFormatException
    {
        NBTTagCompound compound = null;

        try (FileInputStream fileInputStream = new FileInputStream(file))
        {
            compound = CompressedStreamTools.readCompressed(fileInputStream);
        }
        catch (Exception e)
        {
            throw new SchematicFile.UnsupportedSchematicFormatException("Not a compress NBT file!");
        }

        if (compound != null)
            return new SchematicFile(compound);

        return null;
    }

    public static void writeSchematicByName(SchematicFile schematic, String name)
    {
        writeSchematicToFile(schematic, new File(getValidatedSchematicsFile(), name + ".schematic"));
    }

    public static void writeSchematicToFile(SchematicFile schematic, File file)
    {
        NBTTagCompound compound = new NBTTagCompound();
        schematic.writeToNBT(compound);

        try (DataOutputStream dataOutputStream = new DataOutputStream(new GZIPOutputStream(new FileOutputStream(file))))
        {
            RCAccessorNBT.writeEntry("Schematic", compound, dataOutputStream);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static List<String> currentSchematicFileNames()
    {
        return currentSchematicFiles().stream().map(file -> FilenameUtils.getBaseName(file.getName())).collect(Collectors.toList());
    }

    public static Collection<File> currentSchematicFiles()
    {
        File schematicsFile = getValidatedSchematicsFile();
        return schematicsFile != null ? FileUtils.listFiles(schematicsFile, new String[]{"schematic"}, true) : Collections.emptyList();
    }

    public static String getLookupFolderName()
    {
        return "structures/schematics";
    }

    public static File getValidatedSchematicsFile()
    {
        File structuresFile = IvFileHelper.getValidatedFolder(new File(RecurrentComplex.proxy.getDataDirectory(), ResourceDirectory.RESOURCES_FILE_NAME));
        return structuresFile != null ? IvFileHelper.getValidatedFolder(structuresFile, "schematics") : null;
    }
}
