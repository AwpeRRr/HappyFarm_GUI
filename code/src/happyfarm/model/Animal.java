package happyfarm.model;

import happyfarm.util.FarmUtils;

public class Animal extends FarmObject {
    private int mood;

    public Animal(String name, int priority) {
        this(name, priority, "平静", 0);
    }

    public Animal(String name, int priority, String status, int careCount) {
        super("ANIMAL", name, priority, status, careCount);
        this.mood = Math.min(100, 50 + careCount * 5);
    }

    @Override
    public String care() {
        int change = FarmUtils.randomInt(1, 5);
        mood = Math.min(100, mood + change);
        careCount++;
        if (mood >= 80) {
            status = "开心";
        } else if (mood >= 60) {
            status = "活跃";
        } else {
            status = "平静";
        }
        return "已喂养动物 " + name + "，心情 +" + change + "，当前状态：" + status;
    }

    public int getMood() {
        return mood;
    }

    @Override
    protected String getExtraText() {
        return "心情:" + mood;
    }
}
