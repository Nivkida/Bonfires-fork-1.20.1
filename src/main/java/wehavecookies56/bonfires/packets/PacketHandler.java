package wehavecookies56.bonfires.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import wehavecookies56.bonfires.Bonfires;
import wehavecookies56.bonfires.packets.client.*;
import wehavecookies56.bonfires.packets.server.*;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class PacketHandler {
	private static byte packetId = 0;

	private static final String PROTOCOL_VERSION = Integer.toString(1);
	private static final SimpleChannel HANDLER = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(Bonfires.modid, "main_channel"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
	);

	public static void registerPackets() {
		// From Server to Client
		registerMessage(OpenBonfireGUI.class, OpenBonfireGUI::encode, OpenBonfireGUI::new, OpenBonfireGUI::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncBonfire.class, SyncBonfire::encode, SyncBonfire::new, SyncBonfire::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncSaveData.class, SyncSaveData::encode, SyncSaveData::new, SyncSaveData::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncDiscoveryData.class, SyncDiscoveryData::encode, SyncDiscoveryData::new, SyncDiscoveryData::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SendBonfiresToClient.class, SendBonfiresToClient::encode, SendBonfiresToClient::new, SendBonfiresToClient::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(OpenCreateScreen.class, OpenCreateScreen::encode, OpenCreateScreen::new, OpenCreateScreen::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(DisplayTitle.class, DisplayTitle::encode, DisplayTitle::new, DisplayTitle::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(QueueBonfireScreenshot.class, QueueBonfireScreenshot::encode, QueueBonfireScreenshot::new, QueueBonfireScreenshot::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(DeleteScreenshot.class, DeleteScreenshot::encode, DeleteScreenshot::new, DeleteScreenshot::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(SyncRunesPacket.class, SyncRunesPacket::encode, SyncRunesPacket::new, SyncRunesPacket::handle, NetworkDirection.PLAY_TO_CLIENT);


		// From Client to Server
		registerMessage(LightBonfire.class, LightBonfire::encode, LightBonfire::new, LightBonfire::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(Travel.class, Travel::encode, Travel::new, Travel::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(RequestDimensionsFromServer.class, RequestDimensionsFromServer::encode, RequestDimensionsFromServer::new, RequestDimensionsFromServer::handle, NetworkDirection.PLAY_TO_CLIENT);
		registerMessage(LevelUpPacket.class, LevelUpPacket::encode, LevelUpPacket::new, LevelUpPacket::handle, NetworkDirection.PLAY_TO_CLIENT);
	}

	private static <T extends Packet<T>> void registerMessage(Class<T> clazz, BiConsumer<T, FriendlyByteBuf> encode, Function<FriendlyByteBuf, T> decode, BiConsumer<T, Supplier<NetworkEvent.Context>> handler, NetworkDirection playToClient) {
		HANDLER.registerMessage(packetId++, clazz, encode, decode, handler);
	}

	public static void sendTo(Packet<?> packet, ServerPlayer player) {
		HANDLER.send(PacketDistributor.PLAYER.with(() -> player), packet);
	}

	public static void sendToAll(Packet<?> packet) {
		HANDLER.send(PacketDistributor.ALL.noArg(), packet);
	}

	public static void sendToServer(Packet<?> packet) {
		HANDLER.sendToServer(packet);
	}

	public static void sendToAllAround(Packet<?> packet, double x, double y, double z, double range, ResourceKey<Level> dimension) {
		HANDLER.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(x, y, z, range, dimension)), packet);
	}
}