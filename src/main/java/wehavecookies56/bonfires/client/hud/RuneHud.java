package wehavecookies56.bonfires.client.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import wehavecookies56.bonfires.client.gui.LevelRuneManager;

public class RuneHud implements IGuiOverlay {
    private static final ResourceLocation XP_ICON = new ResourceLocation("minecraft", "textures/item/experience_bottle.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.isSpectator()) {
            System.out.println("RuneHud: Skipping render, player is null or spectator");
            return;
        }
        int xp = minecraft.player.totalExperience;
        int level = LevelRuneManager.getSelectedLevel();
        if (xp < 0) {
            System.out.println("RuneHud: Warning, experience negative: " + xp);
            xp = 0;
        }
        String xpText = String.valueOf(xp);
        int x = 10;
        int y = screenHeight - 30;
        try {
            guiGraphics.blit(XP_ICON, x, y, 0, 0, 16, 16, 16, 16);
            System.out.println("RuneHud: Successfully rendered experience_bottle.png");
        } catch (Exception e) {
            System.out.println("RuneHud: Failed to render experience_bottle.png: " + e.getMessage());
        }
        guiGraphics.drawString(minecraft.font, xpText + " (Level " + level + ")", x + 20, y + 4, 0xFFFFFF, true);
        System.out.println("RuneHud: Rendered experience: " + xp + ", Level: " + level);
    }
}