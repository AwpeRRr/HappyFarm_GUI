package happyfarm.util;

import java.util.Locale;

public class FarmUtils {
    private FarmUtils() {
    }

    public static int parsePositiveInt(String text, String fieldName) {
        int value = parseInt(text, fieldName);
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "必须是正整数");
        }
        return value;
    }

    public static int parseNonNegativeInt(String text, String fieldName) {
        int value = parseInt(text, fieldName);
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "不能为负数");
        }
        return value;
    }

    public static String normalizeName(String name) {
        if (isBlank(name)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        return name.trim().replaceAll("\\s+", " ");
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static int randomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("随机数下限不能大于上限");
        }
        return (int) Math.floor(Math.random() * (max - min + 1)) + min;
    }

    public static String safeType(String type) {
        if (isBlank(type)) {
            return "";
        }
        String safe = type.trim().toUpperCase(Locale.ROOT);
        if ("动物".equals(type.trim())) {
            return "ANIMAL";
        }
        if ("植物".equals(type.trim())) {
            return "PLANT";
        }
        if ("工具".equals(type.trim()) || "TOOLITEM".equals(safe)) {
            return "TOOL";
        }
        return safe;
    }

    public static String escapeSaveField(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("%", "%25")
                .replace("\r", "%0D")
                .replace("\n", "%0A")
                .replace("|", "%7C");
    }

    public static String unescapeSaveField(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("%0D", "\r")
                .replace("%0A", "\n")
                .replace("%7C", "|")
                .replace("%25", "%");
    }

    private static int parseInt(String text, String fieldName) {
        if (isBlank(text)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(fieldName + "必须是整数", ex);
        }
    }
}
