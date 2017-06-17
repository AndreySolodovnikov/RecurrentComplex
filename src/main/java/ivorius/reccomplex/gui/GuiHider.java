/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui;

import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.commands.RCCommands;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by lukas on 10.10.16.
 */
@SideOnly(Side.CLIENT)
public class GuiHider
{
    protected static GuiScreen hiddenGUI;

    public static boolean canHide()
    {
        return hiddenGUI == null;
    }

    public static boolean hideGUI()
    {
        if (!canHide())
            return false;

        Minecraft mc = Minecraft.getMinecraft();
        hiddenGUI = mc.currentScreen;

        if (hiddenGUI == null)
            return false;

        mc.displayGuiScreen(null);

        ITextComponent reopen = new TextComponentString("/" + RCCommands.reopen.getCommandName());
        reopen.getStyle().setColor(TextFormatting.GREEN);
        reopen.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + RCCommands.reopen.getCommandName()));
        reopen.getStyle().setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, RecurrentComplex.translations.get("commands.rcreopen.run")));

        mc.thePlayer.addChatMessage(RecurrentComplex.translations.format("commands.rc.didhide", reopen));

        return true;
    }

    public static boolean canReopen()
    {
        return hiddenGUI != null;
    }

    public static boolean reopenGUI()
    {
        if (!canReopen())
            return false;

        GuiScreen hiddenGUI = GuiHider.hiddenGUI;
        GuiHider.hiddenGUI = null;

        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(hiddenGUI);

        if (mc.currentScreen == null)
        {
            GuiHider.hiddenGUI = hiddenGUI;
            return false;
        }

        return true;
    }

    public static void tryReopenGUI()
    {
        Minecraft mc = Minecraft.getMinecraft();

        if (!canReopen())
        {
            mc.thePlayer.addChatMessage(RecurrentComplex.translations.get("commands.rcreopen.nogui"));
            return;
        }

        if (!reopenGUI())
            mc.thePlayer.addChatMessage(RecurrentComplex.translations.get("commands.rcreopen.fail"));
    }
}
