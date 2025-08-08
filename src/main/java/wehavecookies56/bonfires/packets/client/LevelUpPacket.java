package wehavecookies56.bonfires.packets.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import wehavecookies56.bonfires.client.gui.LevelCostManager;
import wehavecookies56.bonfires.client.gui.SkillPointManager;
import wehavecookies56.bonfires.packets.Packet;
import wehavecookies56.bonfires.packets.PacketHandler;
import wehavecookies56.bonfires.packets.client.SyncRunesPacket;
import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.network.NetworkDispatcher;
import daripher.skilltree.network.message.SyncPlayerSkillsMessage;

public class LevelUpPacket extends Packet<LevelUpPacket> {
    private final int targetPurchased;
    private final int levelsToAdd;
    private final int requiredXP;

    public LevelUpPacket(int targetPurchased, int levelsToAdd, int requiredXP) {
        this.targetPurchased = targetPurchased;
        this.levelsToAdd = levelsToAdd;
        this.requiredXP = requiredXP;
    }

    public LevelUpPacket(FriendlyByteBuf buf) {
        super(buf);
        this.targetPurchased = buf.readInt();
        this.levelsToAdd = buf.readInt();
        this.requiredXP = buf.readInt();
    }

    @Override
    public void decode(FriendlyByteBuf buf) {}

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(targetPurchased);
        buf.writeInt(levelsToAdd);
        buf.writeInt(requiredXP);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ServerPlayer player = ctx.getSender();
        if (player == null) {
            System.out.println("LevelUpPacket: Player is null");
            return;
        }

        int currentPurchased = SkillPointManager.getPurchasedLevels(player);
        int currentXP = player.totalExperience;
        int calculatedRequiredXP = LevelCostManager.calculateRequiredXP(currentPurchased, targetPurchased);

        System.out.println("LevelUpPacket: Received packet: targetPurchased=" + targetPurchased +
                ", levelsToAdd=" + levelsToAdd + ", requiredXP=" + requiredXP +
                ", calculatedRequiredXP=" + calculatedRequiredXP +
                ", currentPurchased=" + currentPurchased + ", currentXP=" + currentXP);

        if (currentXP >= requiredXP &&
                requiredXP == calculatedRequiredXP &&
                targetPurchased == currentPurchased + levelsToAdd &&
                targetPurchased <= 100) {

            // Deduct XP
            player.giveExperiencePoints(-requiredXP);

            // Update purchased levels
            SkillPointManager.setPurchasedLevels(player, targetPurchased);

            // Add skill points to SkillTree
            IPlayerSkills skills = PlayerSkillsProvider.get(player);
            if (skills != null) {
                skills.setSkillPoints(skills.getSkillPoints() + levelsToAdd);

                System.out.println("LevelUpPacket: Added " + levelsToAdd + " free skill points (now: " + skills.getSkillPoints() + ")");

                // Sync SkillTree
                NetworkDispatcher.network_channel.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new SyncPlayerSkillsMessage(player));
            }

            System.out.println("LevelUpPacket: Purchased levels updated to " + targetPurchased +
                    ", Added " + levelsToAdd + " free points, Spent " + requiredXP +
                    " XP, Remaining XP: " + player.totalExperience);

            // Sync our data
            PacketHandler.sendTo(new SyncRunesPacket(player.totalExperience, targetPurchased), player);
        } else {
            System.out.println("LevelUpPacket: Cannot level up. Required XP: " + requiredXP +
                    ", Available: " + currentXP + ", Calculated: " + calculatedRequiredXP +
                    ", Current purchased: " + currentPurchased + ", Target purchased: " + targetPurchased);
        }
        ctx.setPacketHandled(true);
    }
}