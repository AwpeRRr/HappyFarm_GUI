package happyfarm.io;

import happyfarm.core.Farm;
import happyfarm.core.FarmResult;
import happyfarm.model.Animal;
import happyfarm.model.FarmObject;
import happyfarm.model.Plant;
import happyfarm.model.ToolItem;
import happyfarm.util.FarmUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Locale;
import java.util.stream.Stream;

public class SaveManager {
    private final Path saveDir;

    public SaveManager(String saveDirPath) {
        if (FarmUtils.isBlank(saveDirPath)) {
            throw new IllegalArgumentException("存档目录不能为空");
        }
        this.saveDir = Paths.get(saveDirPath).toAbsolutePath().normalize();
    }

    public Path getSaveDir() {
        return saveDir;
    }

    public ArrayList<Path> scanSaveFiles() throws IOException {
        ensureSaveDir();
        ArrayList<Path> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(saveDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".txt"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .forEach(files::add);
        }
        return files;
    }

    public void saveFarm(Farm farm, Path saveFile) throws IOException {
        if (farm == null) {
            throw new IllegalArgumentException("农场对象不能为空");
        }
        Path target = resolveSaveFile(saveFile);
        Path parent = target.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (PrintWriter writer = new PrintWriter(new BufferedWriter(
                new FileWriter(target.toFile(), StandardCharsets.UTF_8)))) {
            writer.println("ROWS|" + farm.getRowCount());
            ArrayList<LinkedList<FarmObject>> rows = farm.getRows();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                LinkedList<FarmObject> row = rows.get(rowIndex);
                for (int position = 0; position < row.size(); position++) {
                    writer.println("ITEM|" + (rowIndex + 1) + "|" + position + "|"
                            + row.get(position).toSaveString());
                }
            }
        }
    }

    public Farm loadFarm(Path saveFile) throws IOException {
        Path target = resolveSaveFile(saveFile);
        if (!Files.exists(target)) {
            throw new IOException("存档文件不存在：" + target);
        }

        Farm farm = null;
        try (BufferedReader reader = new BufferedReader(
                new FileReader(target.toFile(), StandardCharsets.UTF_8))) {
            String rawLine;
            int lineNumber = 0;
            while ((rawLine = reader.readLine()) != null) {
                lineNumber++;
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] fields = line.split("\\|", -1);
                try {
                    if ("ROWS".equals(fields[0])) {
                        if (fields.length != 2) {
                            throw new IllegalArgumentException("ROWS 行必须包含 2 个字段");
                        }
                        if (farm != null) {
                            throw new IllegalArgumentException("ROWS 行只能出现一次");
                        }
                        int rowCount = FarmUtils.parsePositiveInt(fields[1], "农场行数");
                        farm = new Farm(rowCount);
                    } else if ("ITEM".equals(fields[0])) {
                        if (farm == null) {
                            throw new IllegalArgumentException("ITEM 行必须出现在 ROWS 行之后");
                        }
                        if (fields.length != 8) {
                            throw new IllegalArgumentException("ITEM 行必须包含 8 个字段");
                        }
                        int rowIndex = FarmUtils.parsePositiveInt(fields[1], "行号") - 1;
                        int position = FarmUtils.parseNonNegativeInt(fields[2], "位置");
                        FarmObject object = parseObjectLine(line);
                        FarmResult result = farm.addObject(rowIndex, position, object);
                        if (!result.isSuccess()) {
                            throw new IllegalArgumentException(result.getMessage());
                        }
                    } else {
                        throw new IllegalArgumentException("未知记录类型：" + fields[0]);
                    }
                } catch (RuntimeException ex) {
                    throw new IOException("第 " + lineNumber + " 行解析失败：" + ex.getMessage(), ex);
                }
            }
        }

        if (farm == null) {
            throw new IOException("存档缺少 ROWS 行");
        }
        return farm;
    }

    public Path createSavePath(String saveName) {
        String safeName = FarmUtils.isBlank(saveName) ? "farm-save" : saveName.trim();
        safeName = safeName.replaceAll("[\\\\/:*?\"<>|]+", "_");
        String suffix = safeName.length() >= 4 ? safeName.substring(safeName.length() - 4) : "";
        if (!".txt".equalsIgnoreCase(suffix)) {
            safeName = safeName + ".txt";
        }
        return saveDir.resolve(safeName).toAbsolutePath().normalize();
    }

    private FarmObject parseObjectLine(String line) {
        String[] fields = line.trim().split("\\|", -1);
        if (fields.length != 8 || !"ITEM".equals(fields[0])) {
            throw new IllegalArgumentException("对象记录格式错误");
        }

        String type = FarmUtils.safeType(fields[3]);
        String name = FarmUtils.unescapeSaveField(fields[4]);
        int priority = FarmUtils.parsePositiveInt(fields[5], "优先级");
        String status = FarmUtils.unescapeSaveField(fields[6]);
        int careCount = FarmUtils.parseNonNegativeInt(fields[7], "照料次数");

        switch (type) {
            case "ANIMAL":
                return new Animal(name, priority, status, careCount);
            case "PLANT":
                return new Plant(name, priority, status, careCount);
            case "TOOL":
                return new ToolItem(name, priority, status, careCount);
            default:
                throw new IllegalArgumentException("不支持的对象类型：" + type);
        }
    }

    private Path resolveSaveFile(Path saveFile) {
        if (saveFile == null) {
            throw new IllegalArgumentException("存档路径不能为空");
        }
        if (saveFile.isAbsolute()) {
            return saveFile.normalize();
        }

        Path fromCurrentDirectory = saveFile.toAbsolutePath().normalize();
        if (fromCurrentDirectory.startsWith(saveDir)) {
            return fromCurrentDirectory;
        }
        return saveDir.resolve(saveFile).normalize();
    }

    private void ensureSaveDir() throws IOException {
        if (!Files.exists(saveDir)) {
            Files.createDirectories(saveDir);
        }
    }
}
