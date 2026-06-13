package happyfarm.model;

import happyfarm.util.FarmUtils;

public class Plant extends FarmObject {
    private int growth;

    public Plant(String name, int priority) {
        this(name, priority, "成长中", 0);
    }

    public Plant(String name, int priority, String status, int careCount) {
        super("PLANT", name, priority, status, careCount);
        this.growth = Math.min(100, careCount * 12);
    }

    @Override
    public String care() {
        int change = FarmUtils.randomInt(8, 18);
        growth = Math.min(100, growth + change);
        careCount++;
        if (growth >= 100) {
            status = "成熟";
        } else if (growth >= 60) {
            status = "快速成长";
        } else {
            status = "成长中";
        }
        return "已浇水植物 " + name + "，成长值提升到 " + growth + "，当前状态：" + status;
    }

    public int getGrowth() {
        return growth;
    }

    @Override
    protected String getExtraText() {
        return "成长值:" + growth;
    }
}
