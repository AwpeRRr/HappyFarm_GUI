package happyfarm.core;

import happyfarm.model.FarmObject;
import happyfarm.util.FarmUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;

public class Farm {
    private final ArrayList<LinkedList<FarmObject>> rows;

    public Farm(int rowCount) {
        if (rowCount <= 0) {
            throw new IllegalArgumentException("农场行数必须大于 0");
        }
        rows = new ArrayList<>();
        for (int i = 0; i < rowCount; i++) {
            rows.add(new LinkedList<>());
        }
    }

    public int getRowCount() {
        return rows.size();
    }

    public ArrayList<LinkedList<FarmObject>> getRows() {
        ArrayList<LinkedList<FarmObject>> copy = new ArrayList<>();
        for (LinkedList<FarmObject> row : rows) {
            copy.add(new LinkedList<>(row));
        }
        return copy;
    }

    public FarmResult addObject(int rowIndex, int position, FarmObject object) {
        if (object == null) {
            return FarmResult.fail("添加失败：对象不能为空");
        }
        if (!isValidRow(rowIndex)) {
            return FarmResult.fail("添加失败：行号越界");
        }
        LinkedList<FarmObject> row = rows.get(rowIndex);
        if (position < 0 || position > row.size()) {
            return FarmResult.fail("添加失败：位置越界，应在 0 到 " + row.size() + " 之间");
        }
        row.add(position, object);
        return FarmResult.success(
                "添加成功：第 " + (rowIndex + 1) + " 行第 " + position + " 位 -> " + object.getDisplayText(),
                object
        );
    }

    public ArrayList<FarmObject> queryByName(String name) {
        ArrayList<FarmObject> result = new ArrayList<>();
        if (FarmUtils.isBlank(name)) {
            return result;
        }
        String keyword = name.trim().toLowerCase(Locale.ROOT);
        for (FarmObject object : getAllObjects()) {
            if (object.getName().toLowerCase(Locale.ROOT).contains(keyword)) {
                result.add(object);
            }
        }
        return result;
    }

    public FarmResult careObject(int rowIndex, int position) {
        FarmObject object = getObjectAt(rowIndex, position);
        if (object == null) {
            return FarmResult.fail("照料失败：行号或位置不存在");
        }
        String message = object.care();
        return FarmResult.success(message, object);
    }

    public FarmResult removeObject(int rowIndex, int position) {
        if (!hasObjectAt(rowIndex, position)) {
            return FarmResult.fail("删除失败：行号或位置不存在");
        }
        FarmObject removed = rows.get(rowIndex).remove(position);
        return FarmResult.success(
                "删除成功：第 " + (rowIndex + 1) + " 行第 " + position + " 位 -> " + removed.getDisplayText(),
                removed
        );
    }

    public FarmResult clearFarm() {
        int count = getAllObjects().size();
        for (LinkedList<FarmObject> row : rows) {
            row.clear();
        }
        return FarmResult.success("清空成功：已删除 " + count + " 个对象");
    }

    public FarmObject getObjectAt(int rowIndex, int position) {
        if (!hasObjectAt(rowIndex, position)) {
            return null;
        }
        return rows.get(rowIndex).get(position);
    }

    public ArrayList<FarmObject> getAllObjects() {
        ArrayList<FarmObject> objects = new ArrayList<>();
        for (LinkedList<FarmObject> row : rows) {
            objects.addAll(row);
        }
        return objects;
    }

    private boolean isValidRow(int rowIndex) {
        return rowIndex >= 0 && rowIndex < rows.size();
    }

    private boolean hasObjectAt(int rowIndex, int position) {
        if (!isValidRow(rowIndex)) {
            return false;
        }
        LinkedList<FarmObject> row = rows.get(rowIndex);
        return position >= 0 && position < row.size();
    }
}
