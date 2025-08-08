package wehavecookies56.bonfires.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;
import wehavecookies56.bonfires.packets.PacketHandler;
import wehavecookies56.bonfires.packets.client.LevelUpPacket;
import wehavecookies56.bonfires.packets.client.SyncRunesPacket;
import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.network.NetworkDispatcher;
import daripher.skilltree.network.message.SyncPlayerSkillsMessage;

public class SkillPointManager {
    private static final String PURCHASED_LEVELS_TAG = "BonfiresPurchasedLevels";
    private static final String EXPERIENCE_TAG = "PlayerExperience";
    private static int lastKnownPurchasedLevels = -1;

    public static void purchaseLevel(int levels) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) {
            System.out.println("purchaseLevel: Player is null");
            return;
        }

        int currentPurchased = getPurchasedLevels(player);
        int currentXP = LevelRuneManager.getPlayerExperience();
        int targetPurchased = currentPurchased + levels;
        int totalXP = LevelCostManager.calculateRequiredXP(currentPurchased, targetPurchased);

        System.out.println("purchaseLevel: Attempting to purchase " + levels + " levels. " +
                "Current purchased: " + currentPurchased + ", " +
                "Target purchased: " + targetPurchased + ", " +
                "Required XP: " + totalXP + ", " +
                "Available XP: " + currentXP);

        if (currentXP >= totalXP && targetPurchased <= 100) {
            PacketHandler.sendToServer(new LevelUpPacket(targetPurchased, levels, totalXP));
            System.out.println("purchaseLevel: Sent LevelUpPacket");
        } else {
            System.out.println("purchaseLevel: Cannot purchase levels");
        }
    }

    public static int getPurchasedLevels(Player player) {
        return player.getPersistentData().getInt(PURCHASED_LEVELS_TAG);
    }

    public static void setPurchasedLevels(Player player, int levels) {
        player.getPersistentData().putInt(PURCHASED_LEVELS_TAG, levels);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.player;
        int currentPurchased = getPurchasedLevels(player);

        if (currentPurchased != lastKnownPurchasedLevels) {
            System.out.println("onPlayerTick: Purchased levels changed from " + lastKnownPurchasedLevels + " to " + currentPurchased);
            lastKnownPurchasedLevels = currentPurchased;
            PacketHandler.sendTo(new SyncRunesPacket(player.totalExperience, currentPurchased), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        int currentPurchased = getPurchasedLevels(player);

        // Initialize if new player
        if (currentPurchased == 0 && !player.getPersistentData().contains(PURCHASED_LEVELS_TAG)) {
            currentPurchased = 1;
            setPurchasedLevels(player, currentPurchased);

            // Add starting skill point
            IPlayerSkills skills = PlayerSkillsProvider.get(player);
            if (skills != null) {
                skills.setSkillPoints(skills.getSkillPoints() + 1);
                System.out.println("onPlayerLoggedIn: Added starting skill point to new player");
            }
        }

        lastKnownPurchasedLevels = currentPurchased;
        System.out.println("onPlayerLoggedIn: Loaded purchased levels: " + currentPurchased + ", Experience: " + player.totalExperience);

        PacketHandler.sendTo(new SyncRunesPacket(player.totalExperience, currentPurchased), player);

        if (currentPurchased < 1 || currentPurchased > 100) {
            System.out.println("onPlayerLoggedIn: Invalid purchased levels detected: " + currentPurchased + ", resetting to 1");
            setPurchasedLevels(player, 1);
            currentPurchased = 1;
            PacketHandler.sendTo(new SyncRunesPacket(player.totalExperience, currentPurchased), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerPlayer player = (ServerPlayer) event.getEntity();
        int currentPurchased = getPurchasedLevels(player);
        player.getPersistentData().putInt(PURCHASED_LEVELS_TAG, currentPurchased);
        player.getPersistentData().putInt(EXPERIENCE_TAG, player.totalExperience);
        System.out.println("onPlayerLoggedOut: Saved purchased levels " + currentPurchased + ", Experience: " + player.totalExperience + " for player " + player.getName().getString());
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            int currentPurchased = getPurchasedLevels(player);
            int currentXP = player.totalExperience;
            CompoundTag playerData = player.getPersistentData();
            playerData.putInt(PURCHASED_LEVELS_TAG, currentPurchased);
            playerData.putInt(EXPERIENCE_TAG, currentXP);
            System.out.println("onPlayerDeath: Saved purchased levels " + currentPurchased + ", Experience: " + currentXP + " for player " + player.getName().getString());

            // Immediately sync with client to reset experience
            PacketHandler.sendTo(new SyncRunesPacket(0, currentPurchased), player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            Player original = event.getOriginal();
            Player newPlayer = event.getEntity();

            CompoundTag originalData = original.getPersistentData();
            if (originalData.contains(PURCHASED_LEVELS_TAG)) {
                newPlayer.getPersistentData().putInt(PURCHASED_LEVELS_TAG,
                        originalData.getInt(PURCHASED_LEVELS_TAG));
            }
            // Do not copy experience - it should be reset after death
            System.out.println("onPlayerClone: Copied data for " + newPlayer.getName().getString() +
                    " from original player. Purchased levels: " +
                    originalData.getInt(PURCHASED_LEVELS_TAG));
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            if (playerData.contains(PURCHASED_LEVELS_TAG)) {
                int savedPurchased = playerData.getInt(PURCHASED_LEVELS_TAG);
                int savedXP = playerData.contains(EXPERIENCE_TAG) ? playerData.getInt(EXPERIENCE_TAG) : 0;

                setPurchasedLevels(player, savedPurchased);
                player.totalExperience = 0; // Reset experience to 0 after death
                lastKnownPurchasedLevels = savedPurchased;

                System.out.println("onPlayerRespawn: Restored purchased levels " + savedPurchased +
                        ", Experience reset to 0 for player " + player.getName().getString());

                // Sync with client - experience should be 0
                PacketHandler.sendTo(new SyncRunesPacket(0, savedPurchased), player);
            }
        }
    }

    public static int getPlayerLevel(Player player) {
        return getPurchasedLevels(player);
    }
}