package wehavecookies56.bonfires.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class LevelRuneManager {
    private static int selectedLevel = 1;

    public static int getSelectedLevel() {
        return selectedLevel;
    }

    public static void setSelectedLevel(int level) {
        selectedLevel = level;
        System.out.println("LevelRuneManager: Set selected level to " + level);
    }

    public static int getPlayerLevel() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            return player.getPersistentData().getInt("BonfiresPurchasedLevels");
        }
        return 1;
    }

    public static int getPlayerExperience() {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null) {
            return player.totalExperience;
        }
        return 0;
    }
}