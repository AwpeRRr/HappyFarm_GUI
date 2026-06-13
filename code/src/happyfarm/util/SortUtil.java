package happyfarm.util;

import happyfarm.model.FarmObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class SortUtil {
    private SortUtil() {
    }

    public static ArrayList<FarmObject> sortByType(ArrayList<FarmObject> objects) {
        ArrayList<FarmObject> sorted = copyOf(objects);
        Collections.sort(sorted, Comparator
                .comparing(FarmObject::getType)
                .thenComparing(FarmObject::getName)
                .thenComparing(Comparator.comparingInt(FarmObject::getPriority).reversed()));
        return sorted;
    }

    public static ArrayList<FarmObject> sortByPriority(ArrayList<FarmObject> objects) {
        ArrayList<FarmObject> sorted = copyOf(objects);
        Collections.sort(sorted, Comparator
                .comparingInt(FarmObject::getPriority).reversed()
                .thenComparing(FarmObject::getType)
                .thenComparing(FarmObject::getName));
        return sorted;
    }

    public static TreeMap<String, ArrayList<FarmObject>> groupByType(ArrayList<FarmObject> objects) {
        TreeMap<String, ArrayList<FarmObject>> grouped = new TreeMap<>();
        for (FarmObject object : copyOf(objects)) {
            grouped.computeIfAbsent(object.getType(), key -> new ArrayList<>()).add(object);
        }
        return grouped;
    }

    public static String formatObjects(ArrayList<FarmObject> objects) {
        ArrayList<FarmObject> list = copyOf(objects);
        if (list.isEmpty()) {
            return "没有对象。";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            builder.append(i + 1).append(". ")
                    .append(list.get(i))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    public static String formatGroupedObjects(TreeMap<String, ArrayList<FarmObject>> grouped) {
        if (grouped == null || grouped.isEmpty()) {
            return "没有对象。";
        }
        StringBuilder builder = new StringBuilder();
        for (String type : grouped.keySet()) {
            builder.append("[").append(type).append("]")
                    .append(System.lineSeparator())
                    .append(formatObjects(grouped.get(type)));
        }
        return builder.toString();
    }

    private static ArrayList<FarmObject> copyOf(ArrayList<FarmObject> objects) {
        return objects == null ? new ArrayList<>() : new ArrayList<>(objects);
    }
}
