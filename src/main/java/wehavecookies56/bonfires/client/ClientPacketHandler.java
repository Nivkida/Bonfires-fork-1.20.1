package wehavecookies56.bonfires.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.lang3.text.WordUtils;
import wehavecookies56.bonfires.Bonfires;
import wehavecookies56.bonfires.BonfiresConfig;
import wehavecookies56.bonfires.LocalStrings;
import wehavecookies56.bonfires.bonfire.Bonfire;
import wehavecookies56.bonfires.client.gui.BonfireScreen;
import wehavecookies56.bonfires.client.gui.CreateBonfireScreen;
import wehavecookies56.bonfires.data.BonfireHandler;
import wehavecookies56.bonfires.data.DiscoveryHandler;
import wehavecookies56.bonfires.packets.client.*;
import wehavecookies56.bonfires.tiles.BonfireTileEntity;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Toby on 07/11/2016.
 */
public class ClientPacketHandler {

    public static DistExecutor.SafeRunnable openBonfire(OpenBonfireGUI packet) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                Minecraft.getInstance().setScreen(new BonfireScreen(
                        (BonfireTileEntity) Minecraft.getInstance().level.getBlockEntity(packet.tileEntity),
                        packet.ownerNames,
                        packet.dimensions.stream()
                                .filter(dim -> !BonfiresConfig.Client.hiddenDimensions.contains(dim.location().toString()))
                                .toList(),
                        packet.registry
                ));
            }
        };
    }

    public static DistExecutor.SafeRunnable setBonfiresFromServer(SendBonfiresToClient packet) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                if (Minecraft.getInstance().screen instanceof BonfireScreen gui) {
                    gui.updateDimensionsFromServer(
                            packet.registry,
                            packet.dimensions.stream()
                                    .filter(dim -> !BonfiresConfig.Client.hiddenDimensions.contains(dim.location().toString()))
                                    .toList(),
                            packet.ownerNames
                    );
                }
            }
        };
    }

    public static DistExecutor.SafeRunnable openCreateScreen(BonfireTileEntity te) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                Minecraft.getInstance().setScreen(new CreateBonfireScreen(te));
            }
        };
    }

    public static DistExecutor.SafeRunnable displayTitle(DisplayTitle packet) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                Gui gui = Minecraft.getInstance().gui;
                gui.setTitle(Component.translatable(packet.title));
                gui.setSubtitle(Component.translatable(packet.subtitle));
                gui.setTimes(packet.fadein, packet.stay, packet.fadeout);
            }
        };
    }

    public static DistExecutor.SafeRunnable syncBonfire(SyncBonfire packet) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                BlockPos pos = new BlockPos(packet.x, packet.y, packet.z);
                Level level = Minecraft.getInstance().level;
                if (level.getBlockEntity(pos) instanceof BonfireTileEntity te) {
                    te.setBonfire(packet.bonfire);
                    te.setBonfireType(packet.type);
                    te.setLit(packet.lit);
                    if (packet.lit) {
                        te.setID(packet.id);
                    }
                }
            }
        };
    }

    public static DistExecutor.SafeRunnable syncSaveData(SyncSaveData packet) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                BonfireHandler.getHandler(Minecraft.getInstance().level).getRegistry().setBonfires(packet.bonfires);
            }
        };
    }

    public static DistExecutor.SafeRunnable syncDiscoveryData(Map<UUID, Instant> discovered) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                DiscoveryHandler.IDiscoveryHandler handler = DiscoveryHandler.getHandler(Minecraft.getInstance().player);
                discovered.forEach(handler::setDiscovered);
            }
        };
    }

    public static DistExecutor.SafeRunnable displayBonfireTravelled(Bonfire bonfire) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                String formattedDimName;
                if (I18n.exists(LocalStrings.getDimensionKey(bonfire.getDimension()))) {
                    String dimName = bonfire.getDimension().location().getPath().replaceAll("_", " ");
                    formattedDimName = WordUtils.capitalizeFully(dimName);
                } else {
                    formattedDimName = I18n.get(LocalStrings.getDimensionKey(bonfire.getDimension()));
                }
                Gui gui = Minecraft.getInstance().gui;
                gui.setTitle(Component.translatable(bonfire.getName()));
                gui.setSubtitle(Component.translatable(formattedDimName));
                gui.setTimes(10, 20, 10);
            }
        };
    }

    public static DistExecutor.SafeRunnable queueBonfireScreenshot(String name, UUID uuid) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                ScreenshotUtils.startScreenshotTimer(name, uuid);
            }
        };
    }

    public static DistExecutor.SafeRunnable deleteScreenshot(UUID uuid, String name) {
        return new DistExecutor.SafeRunnable() {
            @Override
            public void run() {
                if (BonfiresConfig.Client.deleteScreenshotsOnDestroyed) {
                    Path screenshotsDir = Paths.get(Minecraft.getInstance().gameDirectory.getPath(), "bonfires/");
                    String fileName = ScreenshotUtils.getFileNameString(name, uuid);
                    File screenshotFile = new File(screenshotsDir.toFile(), fileName);
                    if (screenshotFile.exists() && screenshotFile.isFile()) {
                        String path = screenshotFile.getPath();
                        if (!screenshotFile.delete()) {
                            Bonfires.LOGGER.warn("Failed to delete screenshot file {}", path);
                        } else {
                            Bonfires.LOGGER.info("Deleted screenshot for destroyed bonfire {}", fileName);
                        }
                    }
                }
            }
        };
    }
}