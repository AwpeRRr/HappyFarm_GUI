package happyfarm.gui;

import happyfarm.model.Animal;
import happyfarm.model.FarmObject;
import happyfarm.model.Plant;
import happyfarm.model.ToolItem;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * 自定义绘制的「RPG 属性卡」面板（成员 B GUI 层）。
 * 不再用纯文本，而是用 Graphics2D 画出：圆角卡片、按类型着色的头部、
 * 大图标、星级优先级、状态徽章，以及核心数值的渐变进度条。
 *
 * 数据全部通过成员 A 对象的公开方法读取（getName/getPriority/getStatus/
 * getCareCount 及各子类的 getMood/getGrowth/getDurability），不修改后端。
 */
public class AttrCardPanel extends JPanel {

    /** 当前展示的对象，null 表示无选中。 */
    private FarmObject object;
    /** 是否处于隐藏彩蛋的「爽翻天」暴走态（由外部传入）。 */
    private boolean hyper;

    // 背景与文字颜色
    private static final Color BG = new Color(0xF4F6F8);
    private static final Color CARD = Color.WHITE;
    private static final Color INK = new Color(0x2B2F33);
    private static final Color SUB = new Color(0x8A9099);
    private static final Color TRACK = new Color(0xE6E9ED);

    public AttrCardPanel() {
        setBackground(BG);
        setPreferredSize(new Dimension(320, 300));
    }

    /** 切换展示对象并重绘。 */
    public void setObject(FarmObject object) {
        setObject(object, false);
    }

    /** 切换展示对象，并标明它是否正处于「爽翻天」暴走态。 */
    public void setObject(FarmObject object, boolean hyper) {
        this.object = object;
        this.hyper = hyper && object != null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int pad = 16;
        int cardX = pad, cardY = pad, cardW = w - pad * 2;

        if (object == null) {
            drawPlaceholder(g2, cardX, cardY, cardW);
            g2.dispose();
            return;
        }
        drawCard(g2, cardX, cardY, cardW);
        g2.dispose();
    }

    /** 无选中对象时的占位卡。 */
    private void drawPlaceholder(Graphics2D g2, int x, int y, int w) {
        int h = 120;
        g2.setColor(CARD);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, 22, 22));
        g2.setColor(SUB);
        g2.setFont(font(Font.PLAIN, 14));
        drawCentered(g2, "点击农场里的对象", x, y + 48, w);
        drawCentered(g2, "这里会显示它的属性", x, y + 72, w);
    }

    /** 绘制完整属性卡。 */
    private void drawCard(Graphics2D g2, int x, int y, int w) {
        Color theme = themeColor(object);
        int headH = 84;
        int cardH = 256;

        // 卡片底（带轻微阴影）
        g2.setColor(new Color(0, 0, 0, 18));
        g2.fill(new RoundRectangle2D.Float(x + 2, y + 4, w, cardH, 22, 22));
        g2.setColor(CARD);
        g2.fill(new RoundRectangle2D.Float(x, y, w, cardH, 22, 22));

        // 头部渐变带（仅上方圆角）
        Graphics2D hg = (Graphics2D) g2.create();
        hg.setClip(new RoundRectangle2D.Float(x, y, w, headH + 22, 22, 22));
        hg.setPaint(new GradientPaint(x, y, theme.brighter(),
                x + w, y + headH, theme));
        hg.fillRect(x, y, w, headH);
        hg.dispose();

        // 头部：图标圆 + 名称 + 类型
        int icoD = 52, icoX = x + 18, icoY = y + 16;
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillOval(icoX, icoY, icoD, icoD);
        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.PLAIN, 26));
        drawCenteredIn(g2, typeSymbol(object), icoX, icoY, icoD, icoD);

        int tx = icoX + icoD + 14;
        g2.setColor(Color.WHITE);
        g2.setFont(font(Font.BOLD, 19));
        g2.drawString(object.getName(), tx, y + 40);
        g2.setFont(font(Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawString(object.getTypeLabel(), tx, y + 62);

        // 正文区
        int rowX = x + 20;
        int cy = y + headH + 28;

        // 优先级星级
        g2.setColor(SUB);
        g2.setFont(font(Font.PLAIN, 12));
        g2.drawString("优先级", rowX, cy);
        drawStars(g2, rowX + 56, cy - 11, object.getPriority(), theme);
        cy += 30;

        // 状态徽章：暴走时盖成亮眼的「爽翻天」彩蛋状态
        g2.setColor(SUB);
        g2.drawString("状态", rowX, cy);
        if (hyper) {
            drawBadge(g2, rowX + 56, cy - 14, "爽翻天!", new Color(0xFF2D6F));
        } else {
            drawBadge(g2, rowX + 56, cy - 14, object.getStatus(), theme);
        }
        cy += 30;

        // 照料次数
        g2.setColor(SUB);
        g2.drawString("照料", rowX, cy);
        g2.setColor(INK);
        g2.setFont(font(Font.BOLD, 13));
        g2.drawString(object.getCareCount() + " 次", rowX + 56, cy);
        cy += 34;

        // 核心数值进度条
        String statName = statName(object);
        if (statName != null) {
            int val = statValue(object);
            g2.setColor(SUB);
            g2.setFont(font(Font.PLAIN, 12));
            g2.drawString(statName, rowX, cy - 6);
            g2.setColor(INK);
            g2.setFont(font(Font.BOLD, 12));
            String num = val + " / 100";
            int nw = g2.getFontMetrics().stringWidth(num);
            g2.drawString(num, x + w - 20 - nw, cy - 6);
            drawMeter(g2, rowX, cy, w - 40, val, theme);
        }
    }

    /** 渐变进度条。 */
    private void drawMeter(Graphics2D g2, int x, int y, int w, int value, Color theme) {
        int h = 14;
        int v = Math.max(0, Math.min(100, value));
        g2.setColor(TRACK);
        g2.fill(new RoundRectangle2D.Float(x, y, w, h, h, h));
        int fw = Math.round(w * (v / 100f));
        if (fw > 0) {
            g2.setPaint(new GradientPaint(x, y, theme.brighter(), x + w, y, theme));
            g2.fill(new RoundRectangle2D.Float(x, y, Math.max(fw, h), h, h, h));
        }
    }

    /** 五星优先级。 */
    private void drawStars(Graphics2D g2, int x, int y, int priority, Color theme) {
        int stars = Math.min(5, Math.max(0, priority));
        g2.setFont(font(Font.PLAIN, 16));
        for (int i = 0; i < 5; i++) {
            g2.setColor(i < stars ? theme : TRACK);
            g2.drawString("★", x + i * 20, y + 14);
        }
        if (priority > 5) {
            g2.setColor(SUB);
            g2.setFont(font(Font.PLAIN, 12));
            g2.drawString("+" + (priority - 5), x + 5 * 20 + 2, y + 13);
        }
    }

    /** 状态徽章（圆角小标签）。 */
    private void drawBadge(Graphics2D g2, int x, int y, String text, Color theme) {
        g2.setFont(font(Font.BOLD, 12));
        int tw = g2.getFontMetrics().stringWidth(text);
        int bw = tw + 20, bh = 22;
        g2.setColor(tint(theme, 0.16f));
        g2.fill(new RoundRectangle2D.Float(x, y, bw, bh, bh, bh));
        g2.setColor(theme.darker());
        g2.drawString(text, x + 10, y + 15);
    }

    // ===== 工具方法 =====

    private void drawCentered(Graphics2D g2, String s, int x, int baseline, int w) {
        int sw = g2.getFontMetrics().stringWidth(s);
        g2.drawString(s, x + (w - sw) / 2, baseline);
    }

    private void drawCenteredIn(Graphics2D g2, String s, int x, int y, int w, int h) {
        java.awt.FontMetrics fm = g2.getFontMetrics();
        int sw = fm.stringWidth(s);
        int sy = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g2.drawString(s, x + (w - sw) / 2, sy);
    }

    private Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    /** 把主题色按比例与白色混合，得到浅色调。 */
    private Color tint(Color c, float alpha) {
        int r = Math.round(c.getRed() * alpha + 255 * (1 - alpha));
        int g = Math.round(c.getGreen() * alpha + 255 * (1 - alpha));
        int b = Math.round(c.getBlue() * alpha + 255 * (1 - alpha));
        return new Color(r, g, b);
    }

    private Color themeColor(FarmObject o) {
        if (o instanceof Animal) { return new Color(0xE8607A); }
        if (o instanceof Plant) { return new Color(0x3FAE6B); }
        if (o instanceof ToolItem) { return new Color(0xE0913A); }
        return new Color(0x6B7280);
    }

    private String typeSymbol(FarmObject o) {
        if (o instanceof Animal) { return "♥"; }
        if (o instanceof Plant) { return "♣"; }
        if (o instanceof ToolItem) { return "⚒"; }
        return "●";
    }

    private String statName(FarmObject o) {
        if (o instanceof Animal) { return "心情"; }
        if (o instanceof Plant) { return "成长"; }
        if (o instanceof ToolItem) { return "耐久"; }
        return null;
    }

    private int statValue(FarmObject o) {
        if (o instanceof Animal) { return ((Animal) o).getMood(); }
        if (o instanceof Plant) { return ((Plant) o).getGrowth(); }
        if (o instanceof ToolItem) { return ((ToolItem) o).getDurability(); }
        return 0;
    }
}
