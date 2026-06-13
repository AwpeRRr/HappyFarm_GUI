package happyfarm;

import happyfarm.core.Farm;
import happyfarm.io.SaveManager;
import happyfarm.model.Animal;
import happyfarm.model.FarmObject;
import happyfarm.model.Plant;
import happyfarm.model.ToolItem;
import happyfarm.util.SortUtil;

import java.nio.file.Path;
import java.util.ArrayList;

public class MemberATest {
    public static void main(String[] args) throws Exception {
        Farm farm = new Farm(3);

        System.out.println(farm.addObject(0, 0, new Animal("小牛", 2)).getMessage());
        System.out.println(farm.addObject(0, 1, new Plant("玉米", 1)).getMessage());
        System.out.println(farm.addObject(1, 0, new ToolItem("水壶", 3)).getMessage());
        System.out.println(farm.careObject(0, 0).getMessage());

        ArrayList<FarmObject> queryResult = farm.queryByName("牛");
        System.out.println("查询“牛”结果数量：" + queryResult.size());
        System.out.println("按类别排序：");
        System.out.print(SortUtil.formatObjects(SortUtil.sortByType(farm.getAllObjects())));
        System.out.println("按优先级排序：");
        System.out.print(SortUtil.formatObjects(SortUtil.sortByPriority(farm.getAllObjects())));

        SaveManager saveManager = new SaveManager("code/saves");
        Path firstSave = saveManager.createSavePath("member-a-demo");
        saveManager.saveFarm(farm, firstSave);
        System.out.println("已保存：" + firstSave);

        Farm loaded = saveManager.loadFarm(firstSave);
        Path secondSave = saveManager.createSavePath("member-a-loaded-copy");
        saveManager.saveFarm(loaded, secondSave);
        System.out.println("读取后再次保存：" + secondSave);
        System.out.println("当前存档数量：" + saveManager.scanSaveFiles().size());
    }
}
