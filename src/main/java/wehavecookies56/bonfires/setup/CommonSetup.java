package wehavecookies56.bonfires.setup;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import wehavecookies56.bonfires.advancements.BonfireLitTrigger;
import wehavecookies56.bonfires.data.BonfireHandler;
import wehavecookies56.bonfires.data.DiscoveryHandler;
import wehavecookies56.bonfires.packets.PacketHandler;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CommonSetup {

    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.registerPackets();
        BonfireHandler.init();
        DiscoveryHandler.init();
        BonfireLitTrigger.TRIGGER_BONFIRE_LIT = CriteriaTriggers.register(new BonfireLitTrigger());
    }

}
