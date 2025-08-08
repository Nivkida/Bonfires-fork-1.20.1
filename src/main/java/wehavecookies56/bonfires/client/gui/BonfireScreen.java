package wehavecookies56.bonfires.client.gui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.text.WordUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import wehavecookies56.bonfires.Bonfires;
import wehavecookies56.bonfires.BonfiresConfig;
import wehavecookies56.bonfires.LocalStrings;
import wehavecookies56.bonfires.bonfire.Bonfire;
import wehavecookies56.bonfires.bonfire.BonfireRegistry;
import wehavecookies56.bonfires.client.ScreenshotUtils;
import wehavecookies56.bonfires.client.gui.widgets.BonfireButton;
import wehavecookies56.bonfires.client.gui.widgets.BonfireCustomButton;
import wehavecookies56.bonfires.client.gui.widgets.BonfirePageButton;
import wehavecookies56.bonfires.client.gui.widgets.DimensionTabButton;
import wehavecookies56.bonfires.packets.PacketHandler;
import wehavecookies56.bonfires.packets.server.RequestDimensionsFromServer;
import wehavecookies56.bonfires.packets.server.Travel;
import wehavecookies56.bonfires.tiles.BonfireTileEntity;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class BonfireScreen extends Screen {

    private static final ResourceLocation MENU = new ResourceLocation(Bonfires.modid, "textures/gui/bonfire_menu.png");
    public static final ResourceLocation TRAVEL_TEX = new ResourceLocation(Bonfires.modid, "textures/gui/travel_menu.png");

    private BonfireCustomButton screenshot, info;
    private Button travel;
    private Button leave;
    private Button levelUp;
    @SuppressWarnings("unused")
    private Button back;
    private Button next;
    private Button prev;

    public Map<ResourceKey<Level>, List<List<Bonfire>>> bonfires;
    private List<List<ResourceKey<Level>>> pages;

    private int currentPage = 0;
    public int bonfirePage = 0;

    private final BonfireTileEntity bonfire;
    private boolean travelOpen;

    // Button IDs
    private final int TRAVEL = 0;
    private final int LEAVE = 1;
    private final int LEVEL_UP = 2;
    private final int NEXT = 3;
    private final int PREV = 4;
    private final int TAB1 = 5;
    private final int TAB2 = 6;
    private final int TAB3 = 7;
    private final int TAB4 = 8;
    private final int TAB5 = 9;
    private final int TAB6 = 10;
    public final int BONFIRE1 = 11;
    private final int BONFIRE2 = 12;
    private final int BONFIRE3 = 13;
    private final int BONFIRE4 = 14;
    private final int BONFIRE5 = 15;
    private final int BONFIRE6 = 16;
    private final int BONFIRE7 = 17;
    private final int BONFIRE_NEXT = 18;
    private final int BONFIRE_PREV = 19;
    private final int SCREENSHOT = 20;
    private final int INFO = 21;

    public int dimTabSelected = TAB1;
    public int bonfireSelected = 0;
    public Bonfire selectedInstance;

    public DimensionTabButton[] tabs;
    private BonfireButton[] bonfireButtons;
    private BonfirePageButton bonfire_next;
    private BonfirePageButton bonfire_prev;

    private final int tex_height = 166;
    private final int travel_width = 195;
    public final int travel_height = 136;

    public BonfireRegistry registry;
    public List<ResourceKey<Level>> dimensions;
    public Map<UUID, String> ownerNames;

    Screenshot screenshotImage;
    boolean showInfo = true;
    boolean noScreenshot = false;

    public BonfireScreen(BonfireTileEntity bonfire, Map<UUID, String> ownerNames, List<ResourceKey<Level>> dimensions, BonfireRegistry registry) {
        super(Component.empty());
        this.bonfire = bonfire;
        this.ownerNames = ownerNames;
        this.registry = registry;
        this.minecraft = Minecraft.getInstance();
        this.dimensions = sortDimensions(dimensions);

        if (BonfiresConfig.Client.renderScreenshotsInGui) {
            screenshotImage = new Screenshot(Minecraft.getInstance().textureManager, new ResourceLocation(Bonfires.modid, bonfire.getID().toString()));
        }
    }

    private List<ResourceKey<Level>> sortDimensions(List<ResourceKey<Level>> dimensions) {
        return dimensions.stream()
                .sorted((o1, o2) -> {
                    if (o1.equals(Level.OVERWORLD)) return -1;
                    if (o1.equals(Level.NETHER)) return o2.equals(Level.OVERWORLD) ? 1 : -1;
                    if (o1.equals(Level.END)) {
                        if (o2.equals(Level.NETHER) || o2.equals(Level.OVERWORLD)) return 1;
                        return -1;
                    }
                    return 0;
                })
                .toList();
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton == 1) {
            Minecraft.getInstance().setScreen(new BonfireScreen(bonfire, ownerNames, dimensions, registry));
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    public void drawCenteredStringNoShadow(GuiGraphics guiGraphics, Font fr, String text, int x, int y, int color) {
        guiGraphics.drawString(fr, text, (x - (fr.width(text) / 2F)), (y - (fr.lineHeight / 2F)), color, false);
    }

    private Map<ResourceKey<Level>, List<List<Bonfire>>> createSeries(ResourceKey<Level> dimension) {
        List<Bonfire> bonfires = BonfireRegistry.sortBonfiresByTime(
                registry.getPrivateBonfiresByOwnerAndPublicPerDimension(Minecraft.getInstance().player.getUUID(), dimension.location()));

        bonfires.sort((o1, o2) -> o1.getId().equals(bonfire.getID()) ? -1 : 0);

        if (bonfires.isEmpty()) {
            return null;
        }

        List<List<Bonfire>> book = new ArrayList<>();
        int plus = bonfires.size() % 7 == 0 ? 0 : 1;

        for (int i = 0; i < (bonfires.size() / 7) + plus; i++) {
            int start = i * 7;
            if (bonfires.size() < 7) start = 0;
            List<Bonfire> page = (start + 7 > bonfires.size()) ?
                    bonfires.subList(start, bonfires.size()) :
                    bonfires.subList(start, start + 7);
            book.add(page);
        }

        return Map.of(dimension, book);
    }

    @Override
    public void tick() {
        if (bonfire.isRemoved() ||
                bonfire.getBlockPos().distManhattan(new Vec3i(
                        (int) minecraft.player.position().x,
                        (int) minecraft.player.position().y,
                        (int) minecraft.player.position().z)) > minecraft.player.getBlockReach() + 3) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        if (screenshotImage != null) {
            screenshotImage.close();
        }
        super.onClose();
    }

    private String getFormattedDimensionName(ResourceKey<Level> dimension) {
        if (I18n.exists(LocalStrings.getDimensionKey(dimension))) {
            String dimName = dimension.location().getPath().replaceAll("_", " ");
            return WordUtils.capitalizeFully(dimName);
        }
        return I18n.get(LocalStrings.getDimensionKey(dimension));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (ScreenshotUtils.isTakingScreenshot()) return;

        renderBackground(guiGraphics);
        guiGraphics.setColor(1, 1, 1, 1);
        Font font = Minecraft.getInstance().font;

        if (travelOpen) {
            renderTravelMenu(guiGraphics, mouseX, mouseY, partialTicks, font);
        } else {
            renderMainMenu(guiGraphics, font);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void renderTravelMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks, Font font) {
        drawTravelMenu(guiGraphics, mouseX, mouseY, partialTicks);

        if (tabs[dimTabSelected - 5] != null) {
            String formattedName = getFormattedDimensionName(tabs[dimTabSelected - 5].getDimension());
            guiGraphics.drawString(font, formattedName + " (" + tabs[dimTabSelected - 5].getDimension().location() + ")",
                    (int)((width / 2F) - 100), (int)((height / 2F) - 62), 1184274, false);
        }

        if (bonfireSelected >= BONFIRE1) {
            drawSelectedBonfire(guiGraphics, mouseX, mouseY, partialTicks);
        }

        renderTooltips(guiGraphics, mouseX, mouseY, font);
        renderPageNumbers(guiGraphics, font);
    }

    private void renderMainMenu(GuiGraphics guiGraphics, Font font) {
        int tex_width = 90;
        guiGraphics.blit(MENU, (width / 4) - (tex_width / 2), (height / 2) - (tex_height / 2), 0, 0, tex_width, tex_height);

        Bonfire currentBonfire = registry.getBonfire(bonfire.getID());
        if (currentBonfire != null) {
            drawCenteredStringNoShadow(guiGraphics, font, currentBonfire.getName(),
                    (width / 4), (height / 2) - (tex_height / 2) + 10, Color.WHITE.getRGB());

            if (!currentBonfire.isPublic()) {
                drawCenteredStringNoShadow(guiGraphics, font, Component.translatable(LocalStrings.TEXT_PRIVATE).getString(),
                        (width / 4), (height / 2) - (tex_height / 2) + 20, Color.WHITE.getRGB());
                drawCenteredStringNoShadow(guiGraphics, font, Component.translatable(LocalStrings.TEXT_PRIVATE).getString(),
                        (width / 4), (height / 2) - (tex_height / 2) + 30, Color.WHITE.getRGB());
            }

            drawCenteredStringNoShadow(guiGraphics, font, ownerNames.get(currentBonfire.getOwner()),
                    (width / 4), (height / 2) - (tex_height / 2) + 145, Color.WHITE.getRGB());

            int playerLevel = SkillPointManager.getPlayerLevel(minecraft.player);
            drawCenteredStringNoShadow(guiGraphics, font, "Уровень: " + playerLevel,
                    (width / 4), (height / 2) - (tex_height / 2) + 120, 0xFFFFFF);
        }
    }

    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, Font font) {
        if (selectedInstance != null) {
            int nameX = (width / 2) - 10 + 12;
            int nameY = (height / 2) - 45;
            int nameEndX = nameX + font.width(selectedInstance.getName());
            int nameEndY = nameY + font.lineHeight;

            if (mouseX >= nameX && mouseX <= nameEndX && mouseY >= nameY && mouseY <= nameEndY) {
                List<FormattedCharSequence> lines = List.of(
                        Component.translatable("ID: " + selectedInstance.getId()).getVisualOrderText(),
                        Component.translatable("TIME: " + selectedInstance.getTimeCreated().toString()).getVisualOrderText()
                );
                guiGraphics.renderTooltip(font, lines, mouseX, mouseY);
            }
        }

        for (DimensionTabButton currentTab : tabs) {
            if (currentTab.visible && currentTab.isMouseOver(mouseX, mouseY)) {
                String tabFormattedName = getFormattedDimensionName(currentTab.getDimension());
                guiGraphics.renderTooltip(font,
                        Component.translatable(tabFormattedName + " (" + currentTab.getDimension().location() + ")"),
                        mouseX, mouseY);
            }
        }
    }

    private void renderPageNumbers(GuiGraphics guiGraphics, Font font) {
        String pagesText = "0/0";
        if (bonfires.get(tabs[dimTabSelected - 5].getDimension()) != null) {
            pagesText = (bonfirePage + 1) + "/" + bonfires.get(tabs[dimTabSelected - 5].getDimension()).size();
        }

        int xZero = (width / 2) - (travel_width / 2) + 16;
        int yZero = (height / 2) - (travel_height / 2) + 128 - 17;
        guiGraphics.drawString(font, pagesText,
                xZero + (55 / 2) - font.width(pagesText) / 2,
                yZero + (14 / 2) - font.lineHeight / 2, 0xFFFFFF);
    }

    public Bonfire getSelectedBonfire() {
        if (bonfireSelected < BONFIRE1 || bonfires == null) return null;

        ResourceKey<Level> dimension = tabs[dimTabSelected - 5].getDimension();
        List<List<Bonfire>> dimensionBonfires = bonfires.get(dimension);

        if (dimensionBonfires == null || bonfirePage >= dimensionBonfires.size()) return null;

        List<Bonfire> pageBonfires = dimensionBonfires.get(bonfirePage);
        int index = bonfireSelected - BONFIRE1;

        return index < pageBonfires.size() ? pageBonfires.get(index) : null;
    }

    private void drawSelectedBonfire(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        if (selectedInstance == null) return;

        int nameX = (width / 2) - 10 + 12;
        int nameY = (height / 2) - 45;

        if (BonfiresConfig.Client.renderScreenshotsInGui && screenshotImage != null &&
                screenshotImage.textureLocation() != null && !noScreenshot) {
            guiGraphics.blit(screenshotImage.textureLocation(), nameX-3, nameY-5,
                    (float) ScreenshotUtils.width /2, 0, ScreenshotUtils.width, ScreenshotUtils.height,
                    ScreenshotUtils.width*2, ScreenshotUtils.height);
        }

        if (showInfo) {
            Font font = Minecraft.getInstance().font;
            guiGraphics.drawString(font, selectedInstance.getName(), nameX, nameY, Color.WHITE.getRGB());
            guiGraphics.drawString(font, "X:" + selectedInstance.getPos().getX() + " Y:" + selectedInstance.getPos().getY() +
                    " Z:" + selectedInstance.getPos().getZ(), nameX, nameY + font.lineHeight + 3, Color.WHITE.getRGB());
            guiGraphics.drawString(font, ownerNames.get(selectedInstance.getOwner()),
                    nameX, nameY + (font.lineHeight + 3) * 2, Color.WHITE.getRGB());
        }
    }

    @Nullable
    private File getBonfireScreenshot(String bonfireName, UUID bonfireUUID) {
        Path screenshotsDir = Paths.get(Minecraft.getInstance().gameDirectory.getPath(), "bonfires/");
        if (!Files.exists(screenshotsDir)) return null;

        String nameNoInvalid = bonfireName.replaceAll("[\\\\/:*?\"<>|]", "_").toLowerCase();
        String targetFilename = nameNoInvalid + "_" + bonfireUUID.toString() + ".png";

        return Arrays.stream(screenshotsDir.toFile().listFiles())
                .filter(File::isFile)
                .filter(file -> file.getName().equals(targetFilename))
                .findFirst()
                .orElse(null);
    }

    private void drawTravelMenu(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        int trueWidth = 219;
        for (DimensionTabButton tab : tabs) {
            tab.render(guiGraphics, mouseX, mouseY, partialTicks);
        }
        guiGraphics.blit(TRAVEL_TEX, (width / 2) - (trueWidth / 2), (height / 2) - (travel_height / 2), 0, 0, trueWidth, travel_height);
    }

    public void action(int id) {
        action(id, false);
    }

    public void action(int id, boolean closesScreen) {
        switch (id) {
            case SCREENSHOT:
                if (selectedInstance != null && bonfire.getID().equals(selectedInstance.getId())) {
                    ScreenshotUtils.startScreenshotTimer(selectedInstance.getName(), selectedInstance.getId());
                }
                break;

            case INFO:
                showInfo = !showInfo;
                break;

            case TRAVEL:
                handleTravelAction(closesScreen);
                break;

            case LEAVE:
                onClose();
                break;

            case LEVEL_UP:
                minecraft.setScreen(new LevelUpScreen(this));
                break;

            case NEXT:
                if (currentPage != pages.size()-1) {
                    currentPage++;
                    dimTabSelected = TAB1;
                    bonfireSelected = 0;
                }
                break;

            case PREV:
                if (currentPage != 0) {
                    currentPage--;
                    dimTabSelected = TAB1;
                    bonfireSelected = 0;
                }
                break;

            case BONFIRE_NEXT:
                if (bonfirePage != bonfires.get(tabs[dimTabSelected - 5].getDimension()).size()-1) {
                    bonfirePage++;
                    bonfireSelected = 0;
                }
                break;

            case BONFIRE_PREV:
                if (bonfirePage != 0) {
                    bonfirePage--;
                    bonfireSelected = 0;
                }
                break;

            case TAB1: case TAB2: case TAB3: case TAB4: case TAB5: case TAB6:
                handleTabSelection(id);
                break;

            case BONFIRE1: case BONFIRE2: case BONFIRE3: case BONFIRE4:
            case BONFIRE5: case BONFIRE6: case BONFIRE7:
                handleBonfireSelection(id);
                break;
        }

        updateButtons();
        if (!closesScreen) {
            PacketHandler.sendToServer(new RequestDimensionsFromServer());
        }
    }

    private void handleTravelAction(boolean closesScreen) {
        if (!travelOpen) {
            travelOpen = true;
            PacketHandler.sendToServer(new RequestDimensionsFromServer());
        } else if (selectedInstance != null) {
            Minecraft.getInstance().level.playSound(Minecraft.getInstance().player,
                    Minecraft.getInstance().player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
            Minecraft.getInstance().level.playSound(Minecraft.getInstance().player,
                    selectedInstance.getPos(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);

            PacketHandler.sendToServer(new Travel(selectedInstance));

            String formattedDimName = getFormattedDimensionName(selectedInstance.getDimension());
            Gui gui = Minecraft.getInstance().gui;
            gui.setTitle(Component.translatable(selectedInstance.getName()));
            gui.setSubtitle(Component.translatable(formattedDimName));
            gui.setTimes(10, 20, 10);

            onClose();
        }
    }

    private void handleTabSelection(int id) {
        dimTabSelected = id;
        bonfireSelected = 0;

        if (bonfires.get(tabs[dimTabSelected - 5].getDimension()) != null &&
                !bonfires.get(tabs[dimTabSelected - 5].getDimension()).isEmpty() &&
                !bonfires.get(tabs[dimTabSelected - 5].getDimension()).get(0).isEmpty()) {

            bonfireSelected = BONFIRE1;
            selectedInstance = registry.getBonfires().get(
                    bonfires.get(tabs[dimTabSelected - 5].getDimension()).get(0).get(0).getId());
            loadBonfireScreenshot();
        }

        bonfirePage = 0;
    }

    private void handleBonfireSelection(int id) {
        bonfireSelected = id;
        selectedInstance = getSelectedBonfire();
        if (BonfiresConfig.Client.renderScreenshotsInGui) {
            loadBonfireScreenshot();
        }
    }

    public void loadBonfireScreenshot() {
        if (selectedInstance == null) {
            noScreenshot = true;
            return;
        }

        File screenshotFile = getBonfireScreenshot(selectedInstance.getName(), selectedInstance.getId());
        if (screenshotFile == null) {
            noScreenshot = true;
            return;
        }

        try {
            if (screenshotImage != null) {
                screenshotImage.close();
                screenshotImage = new Screenshot(Minecraft.getInstance().textureManager,
                        new ResourceLocation(Bonfires.modid, bonfire.getID().toString()));
            }

            screenshotImage.upload(NativeImage.read(new FileInputStream(screenshotFile)));
            noScreenshot = false;
            info.visible = true;
            info.active = true;
        } catch (IOException e) {
            e.printStackTrace();
            noScreenshot = true;
        }
    }

    private void updateButtons() {
        // Hide all tabs initially
        Arrays.stream(tabs).forEach(tab -> tab.visible = false);

        if (travelOpen) {
            updateTravelMenuButtons();
        } else {
            updateMainMenuButtons();
        }
    }

    private void updateTravelMenuButtons() {
        // Update travel-specific buttons
        travel.visible = bonfireSelected >= BONFIRE1;
        travel.setX((width / 2) - 5 + 12);
        travel.setY((height / 2) + 38);

        if (bonfireSelected >= BONFIRE1) {
            travel.active = selectedInstance != null && !selectedInstance.getId().equals(bonfire.getID());

            info.visible = !noScreenshot;
            info.active = !noScreenshot;

            if (BonfiresConfig.Client.renderScreenshotsInGui && bonfire.getID().equals(selectedInstance.getId())) {
                screenshot.visible = true;
                screenshot.active = true;
                screenshot.setY(noScreenshot ? (height / 2) - 50 : (height / 2) - 36);
            } else {
                screenshot.visible = false;
                screenshot.active = false;
            }
        } else {
            info.visible = screenshot.visible = false;
            info.active = screenshot.active = false;
        }

        // Update dimension tabs
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].visible = i < pages.get(currentPage).size();
            if (tabs[i].visible) {
                tabs[i].setDimension(pages.get(currentPage).get(i));
            }
        }

        // Update bonfire buttons
        for (int i = 0; i < bonfireButtons.length; i++) {
            boolean visible = false;

            if (tabs[dimTabSelected - 5] != null && bonfires != null) {
                List<List<Bonfire>> dimensionBonfires = bonfires.get(tabs[dimTabSelected - 5].getDimension());
                if (dimensionBonfires != null && i < dimensionBonfires.get(bonfirePage).size()) {
                    visible = true;
                    bonfireButtons[i].setBonfire(dimensionBonfires.get(bonfirePage).get(i));
                }
            }

            bonfireButtons[i].visible = visible;
        }

        // Update navigation buttons
        leave.visible = levelUp.visible = false;
        next.visible = prev.visible = true;
        bonfire_prev.visible = bonfire_next.visible = true;

        prev.active = currentPage != 0;
        next.active = currentPage != pages.size() - 1;

        boolean hasBonfires = bonfires.get(tabs[dimTabSelected - 5].getDimension()) != null;
        bonfire_prev.active = bonfirePage != 0;
        bonfire_next.active = hasBonfires && bonfirePage != bonfires.get(tabs[dimTabSelected - 5].getDimension()).size() - 1;
    }

    private void updateMainMenuButtons() {
        bonfire_prev.visible = bonfire_prev.active = false;
        bonfire_next.visible = bonfire_next.active = false;

        travel.visible = true;
        travel.setX((width / 4) - (80 / 2));
        travel.setY((height / 2) - (tex_height / 2) + 20);

        leave.visible = true;
        leave.setX((width / 4) - (80 / 2));
        leave.setY((height / 2) - (tex_height / 2) + 41);

        levelUp.visible = true;
        levelUp.setX((width / 4) - (80 / 2));
        levelUp.setY((height / 2) - (tex_height / 2) + 62);

        next.visible = prev.visible = false;
        prev.active = next.active = false;
        info.visible = screenshot.visible = false;
        info.active = screenshot.active = false;

        Arrays.stream(tabs).forEach(tab -> tab.visible = false);
        Arrays.stream(bonfireButtons).forEach(button -> button.visible = false);

        // Adjust for private bonfires
        if (registry.getBonfire(bonfire.getID()) != null && !registry.getBonfire(bonfire.getID()).isPublic()) {
            travel.setY((height / 2) - (tex_height / 2) + 30);
            leave.setY((height / 2) - (tex_height / 2) + 51);
            levelUp.setY((height / 2) - (tex_height / 2) + 72);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !ScreenshotUtils.isTakingScreenshot();
    }

    @Override
    protected void init() {
        super.init();
        initializePages();
        initializeButtons();
        positionButtons();
        updateBonfires();
        setupInitialState();
        updateButtons();
    }

    private void initializePages() {
        pages = new ArrayList<>();
        bonfires = new HashMap<>();

        int plus = dimensions.size() % 6 == 0 ? 0 : 1;
        for (int i = 0; i < (dimensions.size() / 6) + plus; i++) {
            int start = i * 6;
            if (dimensions.size() < 6) start = 0;

            pages.add(dimensions.subList(start, Math.min(start + 6, dimensions.size())));
        }
    }

    private void initializeButtons() {
        int selectedX = (width / 2) - 17;
        int selectedY = (height / 2) - 50;

        addRenderableWidget(screenshot = new BonfireCustomButton(SCREENSHOT, selectedX + 16 + (103 - 16), selectedY,
                BonfireCustomButton.ButtonType.SCREENSHOT, button -> action(SCREENSHOT)));

        addRenderableWidget(info = new BonfireCustomButton(INFO, selectedX + 16 + (103 - 16), selectedY,
                BonfireCustomButton.ButtonType.INFO, button -> action(INFO)));

        addRenderableWidget(travel = Button.builder(Component.translatable(LocalStrings.BUTTON_TRAVEL),
                        button -> action(TRAVEL))
                .pos((width / 4) - (80 / 2), (height / 2) - (tex_height / 2) + 20)
                .size(80, 20)
                .build());

        addRenderableWidget(leave = Button.builder(Component.translatable(LocalStrings.BUTTON_LEAVE),
                        button -> action(LEAVE, true))
                .pos((width / 4) - (80 / 2), (height / 2) - (tex_height / 2) + 41)
                .size(80, 20)
                .build());

        addRenderableWidget(levelUp = Button.builder(Component.translatable("Повысить уровень"),
                        button -> action(LEVEL_UP))
                .pos((width / 4) - (80 / 2), (height / 2) - (tex_height / 2) + 62)
                .size(80, 20)
                .build());

        addRenderableWidget(next = Button.builder(Component.literal(">"), button -> action(NEXT))
                .pos(0, 0).size(20, 20).build());

        addRenderableWidget(prev = Button.builder(Component.literal("<"), button -> action(PREV))
                .pos(20, 0).size(20, 20).build());

        addRenderableWidget(bonfire_next = new BonfirePageButton(this, BONFIRE_NEXT, 0, 0, true));
        addRenderableWidget(bonfire_prev = new BonfirePageButton(this, BONFIRE_PREV, 8, 0, false));

        tabs = new DimensionTabButton[] {
                new DimensionTabButton(this, TAB1, 0, 0),
                new DimensionTabButton(this, TAB2, 0, 0),
                new DimensionTabButton(this, TAB3, 0, 0),
                new DimensionTabButton(this, TAB4, 0, 0),
                new DimensionTabButton(this, TAB5, 0, 0),
                new DimensionTabButton(this, TAB6, 0, 0)
        };

        bonfireButtons = new BonfireButton[] {
                new BonfireButton(this, BONFIRE1, 0, 0),
                new BonfireButton(this, BONFIRE2, 0, 0),
                new BonfireButton(this, BONFIRE3, 0, 0),
                new BonfireButton(this, BONFIRE4, 0, 0),
                new BonfireButton(this, BONFIRE5, 0, 0),
                new BonfireButton(this, BONFIRE6, 0, 0),
                new BonfireButton(this, BONFIRE7, 0, 0)
        };
    }

    private void positionButtons() {
        // Position tabs
        int sixTabs = 6 * 28;
        int gap = travel_width - sixTabs;

        for (int i = 0; i < tabs.length; i++) {
            addRenderableWidget(tabs[i]);
            tabs[i].setX(((width) / 2 - (travel_width / 2) + (i * 28) + gap / 2));
            tabs[i].setY((height / 2) - (travel_width / 2) + 1);
        }

        // Position bonfire buttons
        for (int i = 0; i < bonfireButtons.length; i++) {
            addRenderableWidget(bonfireButtons[i]);
            bonfireButtons[i].setX((width / 2) - 88 - 12);
            bonfireButtons[i].setY((height / 2) + (bonfireButtons[i].getHeight()) * i - 50);
        }

        // Position navigation buttons
        prev.setX(((width) / 2 - (travel_width / 2)) - 8);
        prev.setY((height / 2) - (travel_width / 2) + 6);

        next.setX(((width) / 2 - (travel_width / 2) + (6 * 28) + gap / 2));
        next.setY((height / 2) - (travel_width / 2) + 6);

        bonfire_prev.setX((width / 2) - (travel_width / 2) + 16);
        bonfire_prev.setY((height / 2) - (travel_height / 2) + 128 - 17);

        bonfire_next.setX((width / 2) - (travel_width / 2) + 63);
        bonfire_next.setY((height / 2) - (travel_height / 2) + 128 - 17);
    }

    private void setupInitialState() {
        // Find current dimension in pages
        for (int i = 0; i < pages.size(); i++) {
            for (int j = 0; j < pages.get(i).size(); j++) {
                if (Minecraft.getInstance().level.dimension().location().equals(pages.get(i).get(j).location())) {
                    currentPage = i;
                    dimTabSelected = j + TAB1;
                }
            }
        }

        bonfireSelected = BONFIRE1;
        selectedInstance = registry.getBonfire(bonfire.getID());
        loadBonfireScreenshot();
    }

    public void updateDimensionsFromServer(BonfireRegistry registry, List<ResourceKey<Level>> dimensions, Map<UUID, String> ownerNames) {
        this.dimensions = sortDimensions(dimensions);
        this.registry = registry;
        this.ownerNames = ownerNames;
        updateBonfires();
        updateButtons();
    }

    private void updateBonfires() {
        bonfires.clear();

        for (ResourceKey<Level> dim : dimensions) {
            Map<ResourceKey<Level>, List<List<Bonfire>>> series = createSeries(dim);
            if (series != null && series.get(dim) != null) {
                bonfires.put(dim, series.get(dim));
            }
        }

        if (selectedInstance != null && bonfireSelected != 0) {
            ResourceKey<Level> dimension = tabs[dimTabSelected - 5].getDimension();
            if (bonfires.get(dimension) == null ||
                    bonfires.get(dimension).get(bonfirePage).stream()
                            .noneMatch(b -> selectedInstance.getId().equals(b.getId()))) {
                selectedInstance = null;
                bonfireSelected = 0;
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static class Screenshot implements AutoCloseable {
        private static final ResourceLocation MISSING_LOCATION = null;
        private final TextureManager textureManager;
        private final ResourceLocation textureLocation;
        @Nullable
        private DynamicTexture texture;
        private boolean closed;

        public Screenshot(TextureManager pTextureManager, ResourceLocation pTextureLocation) {
            this.textureManager = pTextureManager;
            this.textureLocation = pTextureLocation;
        }

        public void upload(NativeImage pImage) {
            try {
                this.checkOpen();
                if (this.texture == null) {
                    this.texture = new DynamicTexture(pImage);
                } else {
                    this.texture.setPixels(pImage);
                    this.texture.upload();
                }
                this.textureManager.register(this.textureLocation, this.texture);
            } catch (Throwable throwable) {
                pImage.close();
                this.clear();
                throw throwable;
            }
        }

        public void clear() {
            this.checkOpen();
            if (this.texture != null) {
                this.textureManager.release(this.textureLocation);
                this.texture.close();
                this.texture = null;
            }
        }

        public int getHeight() {
            return texture != null ? texture.getPixels().getHeight() : 0;
        }

        public int getWidth() {
            return texture != null ? texture.getPixels().getWidth() : 0;
        }

        public ResourceLocation textureLocation() {
            return this.texture != null ? this.textureLocation : MISSING_LOCATION;
        }

        public void close() {
            this.clear();
            this.closed = true;
        }

        private void checkOpen() {
            if (this.closed) {
                throw new IllegalStateException("Icon already closed");
            }
        }
    }
}