package happyfarm.gui;

import happyfarm.core.Farm;
import happyfarm.io.SaveManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * 进游戏前的启动菜单：开始游戏 / 读取存档 / 退出。
 * 选完了再把主窗口 FarmGUI 拉起来。
 */
public class StartupMenu extends JFrame {

    private final SaveManager saveManager;

    private static final Color BG = new Color(0xA9D29B);
    private static final Color INK = new Color(0x2B3A2E);

    public StartupMenu(SaveManager saveManager) {
        super("快乐农场");
        this.saveManager = saveManager;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 460);
        setMinimumSize(new Dimension(360, 400));
        setLocationRelativeTo(null);
        setIconImages(UiKit.emojiIcons("🌻"));
        setContentPane(buildContent());
    }

    private JPanel buildContent() {
        // 带点渐变的背景，看着不那么单调
        JPanel root = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, new Color(0xC6E3BC),
                        0, getHeight(), BG));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBorder(new EmptyBorder(40, 48, 40, 48));

        JLabel title = new JLabel("🌻 快乐农场", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 34));
        title.setForeground(INK);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel sub = new JLabel("农场对象管理系统", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 14));
        sub.setForeground(new Color(0x4A5B4E));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        root.add(title);
        root.add(Box.createVerticalStrut(6));
        root.add(sub);
        root.add(Box.createVerticalStrut(40));
        root.add(menuButton("🎮 开始游戏", new Color(0x3FAE6B), e -> onStart()));
        root.add(Box.createVerticalStrut(16));
        root.add(menuButton("📂 读取存档", new Color(0x4F8FD0), e -> onLoad()));
        root.add(Box.createVerticalStrut(16));
        root.add(menuButton("🚪 退出", new Color(0xC65B5B), e -> System.exit(0)));
        return root;
    }

    // 统一长相的大按钮
    private JButton menuButton(String text, Color color,
                               java.awt.event.ActionListener action) {
        JButton b = new JButton(text);
        UiKit.styleButton(b, color);
        b.setFont(new Font("SansSerif", Font.BOLD, 17)); // 菜单按钮比主界面大一号
        b.setBorder(BorderFactory.createEmptyBorder(12, 0, 12, 0));
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        b.addActionListener(action);
        return b;
    }

    // 开始游戏：进一个空农场，行数等用户在主界面里初始化
    private void onStart() {
        openGui(null, null, "新游戏：请输入行数后点击“初始化农场”。");
    }

    // 读取存档：没有就提示，一个直接读，多个弹框选
    private void onLoad() {
        ArrayList<Path> saves;
        try {
            saves = saveManager.scanSaveFiles();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "扫描存档失败：" + ex.getMessage(),
                    "读取存档", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (saves.isEmpty()) {
            JOptionPane.showMessageDialog(this, "还没有任何存档，先去“开始游戏”吧。",
                    "读取存档", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Path picked = (saves.size() == 1) ? saves.get(0) : askUserToPick(saves);
        if (picked == null) {
            return; // 取消选择，留在菜单
        }
        try {
            Farm farm = saveManager.loadFarm(picked);
            openGui(farm, picked, "已读取存档：" + picked.getFileName());
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "读取存档失败：" + ex.getMessage(),
                    "读取存档", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 多个存档时让用户挑一个
    private Path askUserToPick(ArrayList<Path> candidates) {
        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).getFileName().toString();
        }
        Object selected = JOptionPane.showInputDialog(this,
                "检测到多个存档，请选择要读取的存档：", "选择存档",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (selected == null) {
            return null;
        }
        for (Path path : candidates) {
            if (path.getFileName().toString().equals(selected.toString())) {
                return path;
            }
        }
        return null;
    }

    // 关掉菜单，把主窗口拉起来（savePath 非 null 时关联为当前存档）
    private void openGui(Farm farm, Path savePath, String startupMessage) {
        dispose();
        SwingUtilities.invokeLater(() -> {
            FarmGUI gui = new FarmGUI(farm, saveManager, savePath);
            gui.setVisible(true);
            gui.appendResult(startupMessage);
        });
    }
}
