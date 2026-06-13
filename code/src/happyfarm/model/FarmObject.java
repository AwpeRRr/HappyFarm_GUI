package happyfarm.model;

import happyfarm.util.FarmUtils;

import java.util.Locale;
import java.util.Objects;

public abstract class FarmObject {
    protected String type;
    protected String name;
    protected int priority;
    protected String status;
    protected int careCount;

    protected FarmObject(String type, String name, int priority, String status, int careCount) {
        this.type = FarmUtils.safeType(type);
        this.name = FarmUtils.normalizeName(name);
        setPriority(priority);
        setStatus(status);
        if (careCount < 0) {
            throw new IllegalArgumentException("照料次数不能为负数");
        }
        this.careCount = careCount;
    }

    public abstract String care();

    public String toSaveString() {
        return type + "|"
                + FarmUtils.escapeSaveField(name) + "|"
                + priority + "|"
                + FarmUtils.escapeSaveField(status) + "|"
                + careCount;
    }

    public String getDisplayText() {
        return getTypeLabel() + "-" + name + "(P" + priority + ")";
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getPriority() {
        return priority;
    }

    public String getStatus() {
        return status;
    }

    public int getCareCount() {
        return careCount;
    }

    public void setStatus(String status) {
        this.status = FarmUtils.isBlank(status) ? "正常" : status.trim();
    }

    public void setPriority(int priority) {
        if (priority < 0) {
            throw new IllegalArgumentException("优先级不能为负数");
        }
        this.priority = priority;
    }

    public String getTypeLabel() {
        switch (type) {
            case "ANIMAL":
                return "动物";
            case "PLANT":
                return "植物";
            case "TOOL":
                return "工具";
            default:
                return type;
        }
    }

    protected String getExtraText() {
        return "";
    }

    @Override
    public String toString() {
        String base = getTypeLabel() + " " + name
                + " | 类型:" + type
                + " | 优先级:" + priority
                + " | 状态:" + status
                + " | 照料次数:" + careCount;
        String extra = getExtraText();
        return extra.isEmpty() ? base : base + " | " + extra;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof FarmObject)) {
            return false;
        }
        FarmObject other = (FarmObject) obj;
        return type.equalsIgnoreCase(other.type)
                && name.equalsIgnoreCase(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                type.toUpperCase(Locale.ROOT),
                name.toLowerCase(Locale.ROOT)
        );
    }
}
