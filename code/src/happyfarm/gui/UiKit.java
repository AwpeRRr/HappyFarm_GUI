package happyfarm.gui;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Taskbar;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * GUI 层的小工具：把 emoji 画成窗口图标，再统一一下按钮长相。
 * 放一起省得每个窗口各写一遍。
 */
public final class UiKit {

    private UiKit() {
    }

    /**
     * 设置 dock / 任务栏图标（应用级，全局一次就够）。
     * macOS 的 Dock、部分 Linux 桌面会读这个；Windows 任务栏一般跟窗口图标走。
     * 老平台或不支持时静默跳过，不影响启动。
     */
    public static void setDockIcon(String emoji) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(renderEmoji(emoji, 128));
                }
            }
        } catch (UnsupportedOperationException | SecurityException ignored) {
            // 平台不支持就算了，窗口图标还在
        }
    }

    /** 把一个 emoji 画成几种尺寸的图标，给 setIconImages 用（任务栏/标题栏会挑合适的）。 */
    static List<Image> emojiIcons(String emoji) {
        List<Image> list = new ArrayList<>();
        for (int size : new int[] {16, 32, 48, 64, 128}) {
            list.add(renderEmoji(emoji, size));
        }
        return list;
    }

    /** 把 emoji 居中画到一张透明的方图上。 */
    private static BufferedImage renderEmoji(String emoji, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        // 字号取略小于整图，留点边
        Font font = new Font("SansSerif", Font.PLAIN, (int) (size * 0.78));
        g2.setFont(font);
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int tw = fm.stringWidth(emoji);
        int x = (size - tw) / 2;
        int y = (size - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(emoji, x, y);
        g2.dispose();
        return img;
    }

    /** 给按钮统一上色：扁平、白字、手型光标、去掉焦点框。 */
    static void styleButton(JButton b, Color bg) {
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setFont(b.getFont().deriveFont(Font.BOLD, 13f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
