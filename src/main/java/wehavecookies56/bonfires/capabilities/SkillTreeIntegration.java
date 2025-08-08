package wehavecookies56.bonfires.capabilities;

import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.capability.skill.IPlayerSkills;
import net.minecraft.world.entity.player.Player;

public class SkillTreeIntegration {
    public static void addSkillPoint(Player player) {
        IPlayerSkills skillTreeCap = PlayerSkillsProvider.get(player);
        if (skillTreeCap != null) {
            skillTreeCap.grantSkillPoints(1); // Добавляем 1 очко навыков
        }
    }
}