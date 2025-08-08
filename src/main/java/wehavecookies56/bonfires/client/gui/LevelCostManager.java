package wehavecookies56.bonfires.client.gui;

public class LevelCostManager {
    private static final int[] LEVEL_COSTS = new int[100];
    private static final int[] TOTAL_XP = new int[101];

    static {
        // Устанавливаем кастомные стоимости для первых 6 уровней
        LEVEL_COSTS[0] = 9;    // 0 -> 1
        LEVEL_COSTS[1] = 42;   // 1 -> 2
        LEVEL_COSTS[2] = 100;  // 2 -> 3
        LEVEL_COSTS[3] = 180;  // 3 -> 4
        LEVEL_COSTS[4] = 288;  // 4 -> 5
        LEVEL_COSTS[5] = 315;  // 5 -> 6

        // Уровни 6-30: 315 XP за уровень
        for (int i = 6; i < 30; i++) {
            LEVEL_COSTS[i] = 315;
        }

        // Уровни 30-99: 575 XP за уровень
        for (int i = 30; i < LEVEL_COSTS.length; i++) {
            LEVEL_COSTS[i] = 575;
        }

        // Предварительный расчет общего опыта для каждого уровня
        TOTAL_XP[0] = 0;
        for (int level = 1; level <= 100; level++) {
            TOTAL_XP[level] = TOTAL_XP[level - 1] + LEVEL_COSTS[level - 1];
        }
    }

    public static int calculateRequiredXP(int currentLevel, int targetLevel) {
        // Проверка корректности входных данных
        if (currentLevel < 0 || targetLevel <= currentLevel || targetLevel > 100) {
            return 0;
        }

        // Расчет разницы в опыте между целевым и текущим уровнем
        return TOTAL_XP[targetLevel] - TOTAL_XP[currentLevel];
    }

    public static boolean canAffordLevelUp(int currentLevel, int targetLevel, int currentXP) {
        int requiredXP = calculateRequiredXP(currentLevel, targetLevel);
        return currentXP >= requiredXP;
    }

    // Метод для получения стоимости следующего уровня
    public static int getNextLevelCost(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= 100) {
            return 0;
        }
        return LEVEL_COSTS[currentLevel];
    }

    // Метод для получения общего опыта до определенного уровня
    public static int getTotalXPForLevel(int level) {
        if (level < 0 || level > 100) {
            return 0;
        }
        return TOTAL_XP[level];
    }
}