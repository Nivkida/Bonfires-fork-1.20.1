package wehavecookies56.bonfires.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import wehavecookies56.bonfires.Bonfires;

public class LevelUpScreen extends Screen {
    private static final ResourceLocation MENU = new ResourceLocation(Bonfires.modid, "textures/gui/level_up_menu.png");
    private static final ResourceLocation ARROW_LEFT = new ResourceLocation(Bonfires.modid, "textures/gui/arrow_left.png");
    private static final ResourceLocation ARROW_RIGHT = new ResourceLocation(Bonfires.modid, "textures/gui/arrow_right.png");

    private final BonfireScreen parentScreen;
    private int selectedPurchasedLevels;
    private int currentPurchasedLevels;
    private int currentXP;

    // Увеличенные размеры текстуры
    private final int tex_width = 200;
    private final int tex_height = 200;

    // Цвета для лучшей видимости на светлом фоне
    private static final int HEADER_COLOR = 0xFF333333;       // Темно-серый
    private static final int TITLE_COLOR = 0xFF000000;        // Черный
    private static final int LEVEL_COLOR = 0xFF8B4513;        // Коричневый
    private static final int COST_COLOR = 0xFF0066CC;         // Синий
    private static final int XP_COLOR = 0xFF228B22;           // Зеленый
    private static final int STATUS_GOOD_COLOR = 0xFF009900;  // Темно-зеленый
    private static final int STATUS_BAD_COLOR = 0xFFCC0000;   // Красный
    private static final int STATUS_NORMAL_COLOR = 0xFFDAA520;// Золотистый
    private static final int TEXT_COLOR = 0xFF333333;         // Темно-серый

    private Button leftArrow;
    private Button rightArrow;
    private Button confirmButton;

    protected LevelUpScreen(BonfireScreen parentScreen) {
        super(Component.literal("Повышение уровня"));
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();
        // Используем актуальные данные из объекта игрока
        this.currentPurchasedLevels = LevelRuneManager.getPlayerLevel();
        this.selectedPurchasedLevels = Math.max(currentPurchasedLevels, 1);
        this.currentXP = LevelRuneManager.getPlayerExperience();

        int guiLeft = (width - tex_width) / 2;
        int guiTop = (height - tex_height) / 2;

        // Стрелка влево
        addRenderableWidget(leftArrow = new ArrowButton(
                guiLeft + 60,
                guiTop + 65,
                button -> {
                    if (selectedPurchasedLevels > currentPurchasedLevels) {
                        selectedPurchasedLevels--;
                        updateButtonStates();
                    }
                },
                ARROW_LEFT
        ));

        // Стрелка вправо - используем актуальный опыт для проверки
        addRenderableWidget(rightArrow = new ArrowButton(
                guiLeft + 130,
                guiTop + 65,
                button -> {
                    // Проверяем возможность повышения на ОДИН уровень
                    if (selectedPurchasedLevels < 100) {
                        int nextLevel = selectedPurchasedLevels + 1;
                        int requiredXP = LevelCostManager.calculateRequiredXP(selectedPurchasedLevels, nextLevel);
                        if (LevelRuneManager.getPlayerExperience() >= requiredXP) {
                            selectedPurchasedLevels++;
                            updateButtonStates();
                        }
                    }
                },
                ARROW_RIGHT
        ));

        // Кнопка подтверждения
        addRenderableWidget(confirmButton = Button.builder(
                        Component.literal("Подтвердить"),
                        button -> {
                            int levelsToPurchase = selectedPurchasedLevels - currentPurchasedLevels;
                            int requiredXP = LevelCostManager.calculateRequiredXP(currentPurchasedLevels, selectedPurchasedLevels);

                            // Проверяем актуальный опыт
                            if (LevelRuneManager.getPlayerExperience() >= requiredXP && levelsToPurchase > 0) {
                                SkillPointManager.purchaseLevel(levelsToPurchase);
                            }
                        })
                .bounds(guiLeft + 50, guiTop + 120, 100, 20)
                .build());

        // Кнопка назад
        addRenderableWidget(Button.builder(
                        Component.literal("Назад"),
                        button -> minecraft.setScreen(parentScreen))
                .bounds(guiLeft + 50, guiTop + 160, 100, 20)
                .build());

        updateButtonStates();
    }

    private void updateButtonStates() {
        // Получаем актуальные данные
        int actualXP = LevelRuneManager.getPlayerExperience();

        // Левая стрелка активна только если выбранный уровень > текущего
        leftArrow.active = selectedPurchasedLevels > currentPurchasedLevels;

        // Правая стрелка активна если:
        // 1. Не достигнут максимальный уровень
        // 2. Хватает опыта для следующего уровня
        if (selectedPurchasedLevels < 100) {
            int nextLevel = selectedPurchasedLevels + 1;
            int requiredXP = LevelCostManager.calculateRequiredXP(selectedPurchasedLevels, nextLevel);
            rightArrow.active = actualXP >= requiredXP;
        } else {
            rightArrow.active = false;
        }

        // Кнопка подтверждения активна если:
        // 1. Выбранный уровень > текущего
        // 2. Хватает опыта для ВСЕХ уровней
        int totalCost = LevelCostManager.calculateRequiredXP(currentPurchasedLevels, selectedPurchasedLevels);
        confirmButton.active = (selectedPurchasedLevels > currentPurchasedLevels) &&
                (actualXP >= totalCost);
    }

    @Override
    public void tick() {
        // Обновляем данные каждый тик
        int newLevel = LevelRuneManager.getPlayerLevel();
        int newXP = LevelRuneManager.getPlayerExperience();

        if (newLevel != currentPurchasedLevels || newXP != currentXP) {
            currentPurchasedLevels = newLevel;
            currentXP = newXP;

            // Если текущий уровень увеличился, обновляем выбранный уровень
            if (selectedPurchasedLevels < currentPurchasedLevels) {
                selectedPurchasedLevels = currentPurchasedLevels;
            }

            updateButtonStates();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        int guiLeft = (width - tex_width) / 2;
        int guiTop = (height - tex_height) / 2;
        int centerX = guiLeft + tex_width / 2;

        // Отрисовка фона
        guiGraphics.blit(MENU, guiLeft, guiTop, 0, 0, tex_width, tex_height, tex_width, tex_height);

        // Заголовок (без тени)
        drawCenteredStringWithoutShadow(guiGraphics, "Повышение уровня", centerX, guiTop + 15, HEADER_COLOR);

        // Текущий уровень (без тени)
        drawCenteredStringWithoutShadow(guiGraphics, "Текущий уровень: " + currentPurchasedLevels, centerX, guiTop + 35, LEVEL_COLOR);

        // Стоимость следующего уровня (без тени)
        int nextLevelCost = LevelCostManager.calculateRequiredXP(currentPurchasedLevels, currentPurchasedLevels + 1);
        drawCenteredStringWithoutShadow(guiGraphics, "Стоимость следующего: " + nextLevelCost, centerX, guiTop + 50, COST_COLOR);

        // Целевой уровень (между стрелками) (без тени)
        drawCenteredStringWithoutShadow(guiGraphics, String.valueOf(selectedPurchasedLevels), centerX, guiTop + 70, TITLE_COLOR);

        // Стоимость повышения до выбранного уровня (без тени)
        int totalCost = LevelCostManager.calculateRequiredXP(currentPurchasedLevels, selectedPurchasedLevels);
        drawCenteredStringWithoutShadow(guiGraphics, "Общая стоимость: " + totalCost, centerX, guiTop + 90, COST_COLOR);

        // Текущий опыт игрока (без тени)
        int playerXP = LevelRuneManager.getPlayerExperience();
        drawCenteredStringWithoutShadow(guiGraphics, "Ваш опыт: " + playerXP, centerX, guiTop + 105, XP_COLOR);

        // Информация о повышении уровня (без тени)
        if (selectedPurchasedLevels > currentPurchasedLevels) {
            int levelsToPurchase = selectedPurchasedLevels - currentPurchasedLevels;
            drawCenteredStringWithoutShadow(guiGraphics, "Повышение на " + levelsToPurchase + " уровней", centerX, guiTop + 25, TEXT_COLOR);
        }

        // Сообщение о статусе (перемещено между кнопками) (без тени)
        if (confirmButton.active) {
            drawCenteredStringWithoutShadow(guiGraphics, "Достаточно опыта для повышения", centerX, guiTop + 145, STATUS_GOOD_COLOR);
        } else if (selectedPurchasedLevels > currentPurchasedLevels) {
            drawCenteredStringWithoutShadow(guiGraphics, "Недостаточно опыта", centerX, guiTop + 145, STATUS_BAD_COLOR);
        } else {
            drawCenteredStringWithoutShadow(guiGraphics, "Выберите уровень для повышения", centerX, guiTop + 145, STATUS_NORMAL_COLOR);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    // Метод для отрисовки текста без тени
    private void drawCenteredStringWithoutShadow(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        int width = font.width(text);
        guiGraphics.drawString(font, text, centerX - width / 2, y, color, false);
    }

    private static class ArrowButton extends Button {
        private final ResourceLocation texture;

        public ArrowButton(int x, int y, OnPress onPress, ResourceLocation texture) {
            super(x, y, 20, 20, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            if (this.visible) {
                if (!this.active) {
                    guiGraphics.setColor(0.5F, 0.5F, 0.5F, 1.0F);
                }
                guiGraphics.blit(texture, this.getX(), this.getY(), 0, 0, 20, 20, 20, 20);
                guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }
}