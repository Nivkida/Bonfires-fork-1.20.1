package wehavecookies56.bonfires.packets.client;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import wehavecookies56.bonfires.packets.Packet;

public class SyncRunesPacket extends Packet<SyncRunesPacket> {
    private final int experience;
    private final int purchasedLevels;

    public SyncRunesPacket(int experience, int purchasedLevels) {
        this.experience = experience;
        this.purchasedLevels = purchasedLevels;
    }

    public SyncRunesPacket(FriendlyByteBuf buf) {
        super(buf);
        this.experience = buf.readInt();
        this.purchasedLevels = buf.readInt();
    }

    @Override
    public void decode(FriendlyByteBuf buf) {}

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(experience);
        buf.writeInt(purchasedLevels);
    }

    @Override
    public void handle(NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            Player player = minecraft.player;
            if (player != null) {
                // Always set experience to the value from server
                player.totalExperience = experience;
                player.getPersistentData().putInt("BonfiresPurchasedLevels", purchasedLevels);
                System.out.println("SyncRunesPacket: Synced client experience to " + experience + ", purchased levels to " + purchasedLevels);
            } else {
                System.out.println("SyncRunesPacket: Player is null");
            }
        });
        ctx.setPacketHandled(true);
    }
}