package happyfarm.model;

import happyfarm.util.FarmUtils;

public class ToolItem extends FarmObject {
    private int durability;

    public ToolItem(String name, int priority) {
        this(name, priority, "可用", 0);
    }

    public ToolItem(String name, int priority, String status, int careCount) {
        super("TOOL", name, priority, status, careCount);
        this.durability = Math.max(0, Math.min(100, 80 - careCount * 3));
    }

    @Override
    public String care() {
        int repair = FarmUtils.randomInt(5, 15);
        durability = Math.min(100, durability + repair);
        careCount++;
        if (durability >= 80) {
            status = "状态良好";
        } else if (durability >= 40) {
            status = "可用";
        } else {
            status = "需要维修";
        }
        return "已维护工具 " + name + "，耐久度恢复到 " + durability + "，当前状态：" + status;
    }

    public int getDurability() {
        return durability;
    }

    @Override
    protected String getExtraText() {
        return "耐久度:" + durability;
    }
}
