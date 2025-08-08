package wehavecookies56.bonfires;

import com.mojang.brigadier.CommandDispatcher;
import daripher.skilltree.capability.skill.IPlayerSkills;
import daripher.skilltree.capability.skill.PlayerSkillsProvider;
import daripher.skilltree.network.NetworkDispatcher;
import daripher.skilltree.network.message.SyncPlayerSkillsMessage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import wehavecookies56.bonfires.bonfire.Bonfire;
import wehavecookies56.bonfires.bonfire.BonfireRegistry;
import wehavecookies56.bonfires.client.gui.SkillPointManager;
import wehavecookies56.bonfires.client.hud.RuneHud;
import wehavecookies56.bonfires.data.BonfireHandler;
import wehavecookies56.bonfires.data.DiscoveryHandler;
import wehavecookies56.bonfires.packets.PacketHandler;
import wehavecookies56.bonfires.packets.client.SyncDiscoveryData;
import wehavecookies56.bonfires.packets.client.SyncRunesPacket;
import wehavecookies56.bonfires.packets.client.SyncSaveData;
import wehavecookies56.bonfires.setup.*;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Mod("bonfires")
public class Bonfires {
    public static Logger LOGGER = LogManager.getLogger();
    public static final String modid = "bonfires";

    public static final UUID reinforceDamageModifier = UUID.fromString("117e876c-c9bd-4898-985a-2ecb24198350");

    public Bonfires() {
        final ModLoadingContext modLoadingContext = ModLoadingContext.get();
        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        BlockSetup.BLOCKS.register(modEventBus);
        ItemSetup.ITEMS.register(modEventBus);
        EntitySetup.TILE_ENTITIES.register(modEventBus);
        CreativeTabSetup.TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(new RuneHud());
        MinecraftForge.EVENT_BUS.register(SkillPointManager.class);
        PacketHandler.registerPackets();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, BonfiresConfig.CLIENT_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BonfiresConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, BonfiresConfig.SERVER_SPEC);

        MinecraftForge.EVENT_BUS.register(new CommonSetup());
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void entityDeath(LivingDropsEvent event) {
        if (event.getSource().is(DamageTypes.IN_FIRE) || event.getEntity().isOnFire() || (event.getSource().getEntity() instanceof Player && ((Player) event.getSource().getEntity()).getMainHandItem().getItem() == ItemSetup.coiled_sword.get())) {
            Random r = new Random();
            double percent = r.nextDouble() * 100;
            if (percent > 65) {
                event.getDrops().add(new ItemEntity(event.getEntity().level(), event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(), new ItemStack(ItemSetup.ash_pile.get())));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getLevel().isClientSide) {
            System.out.println("onPlayerJoin: Не серверный игрок или клиентская сторона, пропускаем");
            return;
        }

        // Получаем текущий уровень игрока напрямую из его данных
        int currentLevel = SkillPointManager.getPurchasedLevels(player);
        int experience = player.totalExperience;

        // Синхронизируем с клиентом
        PacketHandler.sendTo(new SyncRunesPacket(experience, currentLevel), player);
        System.out.println("onPlayerJoin: Синхронизирован уровень: " + currentLevel + ", опыт: " + experience);

        // Дополнительная синхронизация с SkillTree
        IPlayerSkills skills = PlayerSkillsProvider.get(player);
        if (skills != null) {
            NetworkDispatcher.network_channel.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new SyncPlayerSkillsMessage(player));
        }
    }

    @SubscribeEvent
    public void entityJoinWorld(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PacketHandler.sendTo(new SyncSaveData(BonfireHandler.getServerHandler(event.getLevel().getServer()).getRegistry().getBonfires()), player);
                PacketHandler.sendTo(new SyncDiscoveryData(DiscoveryHandler.getHandler(player)), player);
                if (DiscoveryHandler.getHandler(player).getDiscovered().isEmpty()) {
                    BonfireRegistry registry = BonfireHandler.getServerHandler(event.getLevel().getServer()).getRegistry();
                    DiscoveryHandler.IDiscoveryHandler discoveryHandler = DiscoveryHandler.getHandler(player);
                    List<Bonfire> bonfires = registry.getBonfiresByOwner(player.getUUID());
                    bonfires.forEach(bonfire -> discoveryHandler.setDiscovered(bonfire.getId(), bonfire.getTimeCreated()));
                }
            }
        }
    }

    @SubscribeEvent
    public void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!event.getEntity().level().isClientSide) {
            PacketHandler.sendTo(new SyncSaveData(BonfireHandler.getServerHandler(event.getEntity().getServer()).getRegistry().getBonfires()), (ServerPlayer) event.getEntity());
        }
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        // Обработка удалена
    }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        BonfiresCommand.register(dispatcher);
        TravelCommand.register(dispatcher);
    }
}