package happyfarm;

import happyfarm.core.Farm;
import happyfarm.gui.FarmGUI;
import happyfarm.io.SaveManager;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 程序入口（成员 B 负责）。
 *
 * 启动逻辑：
 *  - 创建指向 code/saves 的 SaveManager。
 *  - 扫描存档目录：
 *      没有存档      -> 打开空界面，等待用户输入行数并点击“初始化”。
 *      恰好一个存档  -> 自动读取该存档。
 *      多个存档      -> 弹出选择框让用户挑一个读取（也可取消，进入新建界面）。
 */
public class Main {

    /** 默认存档目录，相对项目根目录。 */
    private static final String SAVE_DIR = "code/saves";

    public static void main(String[] args) {
        SaveManager saveManager = new SaveManager(SAVE_DIR);

        // 启动时先扫描一次存档目录，决定初始农场。扫描属于 IO，放在进入事件派发线程前完成即可，
        // 这里数据量很小，不需要单独开线程；真正可能阻塞的保存/读取在 GUI 中用 SwingWorker 处理。
        ArrayList<Path> saves = scanQuietly(saveManager);
        StartupChoice choice = decideStartup(saves);

        // 所有 Swing 组件的创建与显示都必须在事件派发线程（EDT）中进行。
        SwingUtilities.invokeLater(() -> launch(saveManager, choice));
    }

    /** 在 EDT 中构建并显示窗口，必要时弹出存档选择框。 */
    private static void launch(SaveManager saveManager, StartupChoice choice) {
        Farm farm = null;
        String startupMessage;

        switch (choice.mode) {
            case AUTO_LOAD:
                try {
                    farm = saveManager.loadFarm(choice.autoLoadPath);
                    startupMessage = "已自动读取唯一存档：" + choice.autoLoadPath.getFileName();
                } catch (IOException ex) {
                    farm = null;
                    startupMessage = "自动读取存档失败：" + ex.getMessage() + "，请新建农场或重新读取。";
                }
                break;
            case CHOOSE:
                Path picked = askUserToPick(choice.candidates);
                if (picked != null) {
                    try {
                        farm = saveManager.loadFarm(picked);
                        startupMessage = "已读取存档：" + picked.getFileName();
                    } catch (IOException ex) {
                        farm = null;
                        startupMessage = "读取存档失败：" + ex.getMessage() + "，请新建农场或重新读取。";
                    }
                } else {
                    startupMessage = "未选择存档，请输入行数后点击“初始化”新建农场。";
                }
                break;
            case NEW_FARM:
            default:
                startupMessage = "未发现存档，请输入行数后点击“初始化”新建农场。";
                break;
        }

        FarmGUI gui = new FarmGUI(farm, saveManager);
        gui.setVisible(true);
        gui.appendResult(startupMessage);
    }

    /** 弹出存档选择对话框，返回用户选中的路径；取消返回 null。 */
    private static Path askUserToPick(ArrayList<Path> candidates) {
        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).getFileName().toString();
        }
        Object selected = JOptionPane.showInputDialog(
                null,
                "检测到多个存档，请选择要读取的存档：",
                "选择存档",
                JOptionPane.QUESTION_MESSAGE,
                null,
                names,
                names[0]);
        if (selected == null) {
            return null;
        }
        String name = selected.toString();
        for (Path path : candidates) {
            if (path.getFileName().toString().equals(name)) {
                return path;
            }
        }
        return null;
    }

    /** 扫描存档目录，出错时返回空列表（不让启动崩溃）。 */
    private static ArrayList<Path> scanQuietly(SaveManager saveManager) {
        try {
            return saveManager.scanSaveFiles();
        } catch (IOException ex) {
            return new ArrayList<>();
        }
    }

    /**
     * 根据存档列表决定启动方式。抽成纯函数便于单独验证三种分支。
     */
    static StartupChoice decideStartup(ArrayList<Path> saves) {
        if (saves == null || saves.isEmpty()) {
            return StartupChoice.newFarm();
        }
        if (saves.size() == 1) {
            return StartupChoice.autoLoad(saves.get(0));
        }
        return StartupChoice.choose(saves);
    }

    /** 启动决策结果。 */
    static class StartupChoice {
        enum Mode { NEW_FARM, AUTO_LOAD, CHOOSE }

        final Mode mode;
        final Path autoLoadPath;
        final ArrayList<Path> candidates;

        private StartupChoice(Mode mode, Path autoLoadPath, ArrayList<Path> candidates) {
            this.mode = mode;
            this.autoLoadPath = autoLoadPath;
            this.candidates = candidates;
        }

        static StartupChoice newFarm() {
            return new StartupChoice(Mode.NEW_FARM, null, new ArrayList<>());
        }

        static StartupChoice autoLoad(Path path) {
            return new StartupChoice(Mode.AUTO_LOAD, path, new ArrayList<>());
        }

        static StartupChoice choose(ArrayList<Path> candidates) {
            return new StartupChoice(Mode.CHOOSE, null, new ArrayList<>(candidates));
        }
    }
}
