package happyfarm;

import happyfarm.gui.StartupMenu;
import happyfarm.gui.UiKit;
import happyfarm.io.SaveManager;

import javax.swing.SwingUtilities;

/**
 * 程序入口（成员 B 负责）。
 *
 * 启动后先弹出菜单：开始游戏 / 读取存档 / 退出。
 * 真正的农场窗口由菜单按需拉起。
 */
public class Main {

    /** 默认存档目录，相对项目根目录。 */
    private static final String SAVE_DIR = "code/saves";

    public static void main(String[] args) {
        SaveManager saveManager = new SaveManager(SAVE_DIR);
        UiKit.setDockIcon("🌻"); // dock/任务栏图标，设一次即可
        // 所有 Swing 组件的创建与显示都必须在事件派发线程（EDT）中进行。
        SwingUtilities.invokeLater(() -> new StartupMenu(saveManager).setVisible(true));
    }
}
