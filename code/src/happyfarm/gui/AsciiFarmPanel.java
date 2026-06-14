package happyfarm.gui;

import happyfarm.model.Animal;
import happyfarm.model.FarmObject;
import happyfarm.model.Plant;
import happyfarm.model.ToolItem;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 底部那块 ascii 小农场，纯属好玩。
 * 动物用 getMood() 决定动得勤不勤，植物和工具不动。
 * 鼠标可以把东西拖来拖去。
 */
public class AsciiFarmPanel extends JPanel {

    // 草场大小（带一圈栅栏）
    private static final int COLS = 52;
    private static final int ROWS = 8;

    // 字号，面板高度也是按它算的
    private static final int FONT_SIZE = 14;

    // 定时器节拍，具体动不动还得看各自心情
    private static final int STEP_MS = 360;

    // 每个 tick 心情大概掉多少（慢慢变蔫）
    private static final double MOOD_DRAIN = 0.08;

    // 藏的小彩蛋：心情已经满了还硬要照料，就能冲到 101，进入暴走
    private static final int HYPER = 101;

    // 暴走能嗨多久（tick 数），按节拍算大概十来秒
    private static final int HYPER_TICKS = 28;

    // 配色，跟属性卡那边对上
    private static final Color FIELD = new Color(0xDDEFD8);
    private static final Color GRASS = new Color(0xA9D29B);
    private static final Color FENCE = new Color(0x9C7B4E);
    private static final Color ANIMAL_C = new Color(0xE8607A);
    private static final Color PLANT_C = new Color(0x2F9E59);
    private static final Color TOOL_C = new Color(0xCC7A1F);

    // 草场里的一只东西
    private static final class Sprite {
        FarmObject ref;
        int col, row;
        char glyph;
        Color color;
        boolean mobile;
        int cooldown;     // 还要等几个 tick 才动
        double liveMood;  // 本地心情，会随时间慢慢掉，照料后回升
        int lastCare;     // 上次记下的照料次数，用来发现又被喂了一口
        int hyperTicks;   // 暴走还能持续几个 tick，>0 就一直嗨
    }

    private final List<Sprite> sprites = new ArrayList<>();
    private final Random rng = new Random();
    private final Timer timer;

    // 上次画的时候记下来的位置，拖动时拿来换算格子
    private int lastOx = 8, lastOy = 6, lastCw = 8, lastChh = 16;
    // 正在拖的那只，没有就是 null
    private Sprite dragging;

    public AsciiFarmPanel() {
        setBackground(new Color(0xF4F6F8));
        // 高度按字体行高算，免得最后一行被切掉
        FontMetrics fm = getFontMetrics(new Font(Font.MONOSPACED, Font.BOLD, FONT_SIZE));
        int gridH = ROWS * fm.getHeight();
        setPreferredSize(new Dimension(0, gridH + 16));
        timer = new Timer(STEP_MS, e -> {
            stepAnimals();
            repaint();
        });
        installDragHandler();
    }

    // 鼠标拖动：按下抓住、拖动跟随、松开放下
    private void installDragHandler() {
        MouseAdapter handler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragging = spriteAt(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragging == null) {
                    return;
                }
                int[] cell = toCell(e.getX(), e.getY());
                dragging.col = cell[0];
                dragging.row = cell[1];
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragging != null) {
                    dragging.cooldown = 2; // 刚放下别急着又跑
                    dragging = null;
                }
            }
        };
        addMouseListener(handler);
        addMouseMotionListener(handler);
    }

    // 看看鼠标这格上面有没有东西，后画的在上层所以倒着找
    private Sprite spriteAt(int px, int py) {
        int[] cell = toCell(px, py);
        for (int i = sprites.size() - 1; i >= 0; i--) {
            Sprite s = sprites.get(i);
            if (s.col == cell[0] && s.row == cell[1]) {
                return s;
            }
        }
        return null;
    }

    // 像素坐标换成格子坐标，别跑出栅栏
    private int[] toCell(int px, int py) {
        int c = (px - lastOx) / Math.max(1, lastCw);
        int r = (py - lastOy) / Math.max(1, lastChh);
        c = Math.min(COLS - 2, Math.max(1, c));
        r = Math.min(ROWS - 2, Math.max(1, r));
        return new int[] {c, r};
    }

    // 面板没显示的时候就别让定时器空转了
    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    /**
     * 拿农场最新的对象重建一遍精灵：还在的按引用保住原位置，
     * 新来的随机找个空格子放，没了的就删掉。
     */
    public void setObjects(List<FarmObject> objects) {
        Map<FarmObject, Sprite> old = new IdentityHashMap<>();
        for (Sprite s : sprites) {
            old.put(s.ref, s);
        }
        sprites.clear();
        if (objects != null) {
            for (FarmObject o : objects) {
                Sprite s = old.get(o);
                if (s == null) {
                    s = createSprite(o);
                }
                applyLook(s, o);
                sprites.add(s);
            }
        }
        repaint();
    }

    // 新对象，找个空位放下
    private Sprite createSprite(FarmObject o) {
        Sprite s = new Sprite();
        s.ref = o;
        int[] pos = randomFreeCell();
        s.col = pos[0];
        s.row = pos[1];
        s.liveMood = realMood(o); // 初始活力就用真实心情
        s.lastCare = o.getCareCount();
        return s;
    }

    // 定一下长啥样、能不能动；顺便把照料后涨上来的心情接回本地
    private void applyLook(Sprite s, FarmObject o) {
        if (o instanceof Animal) {
            s.glyph = 'Q';
            s.color = ANIMAL_C;
            s.mobile = true;
            int care = o.getCareCount();
            int real = realMood(o);
            // 又被喂了一口：心情没满就正常回升，已经满 100 还喂就触发彩蛋暴走一阵
            if (care > s.lastCare) {
                if (real >= 100) {
                    s.liveMood = HYPER;
                    s.hyperTicks = HYPER_TICKS;
                } else {
                    s.liveMood = Math.max(s.liveMood, real);
                }
            }
            s.lastCare = care;
        } else if (o instanceof Plant) {
            s.glyph = 'Y';
            s.color = PLANT_C;
            s.mobile = false;
        } else if (o instanceof ToolItem) {
            s.glyph = 'T';
            s.color = TOOL_C;
            s.mobile = false;
        } else {
            s.glyph = '*';
            s.color = Color.DARK_GRAY;
            s.mobile = false;
        }
    }

    // 随机挑个没被占的格子
    private int[] randomFreeCell() {
        int x = 1, y = 1;
        for (int attempt = 0; attempt < 12; attempt++) {
            x = 1 + rng.nextInt(COLS - 2);
            y = 1 + rng.nextInt(ROWS - 2);
            boolean taken = false;
            for (Sprite s : sprites) {
                if (s.col == x && s.row == y) {
                    taken = true;
                    break;
                }
            }
            if (!taken) {
                break;
            }
        }
        return new int[] {x, y};
    }

    // 让动物动一动。心情高的又快又勤，心情低的半天挪一下；同时心情慢慢往下掉
    private void stepAnimals() {
        for (Sprite s : sprites) {
            if (!s.mobile || s == dragging) {
                continue;
            }
            boolean hyper = s.hyperTicks > 0;
            if (hyper) {
                s.hyperTicks--;
            }
            // 暴走时心情不掉，嗨完直接落回 100 以内；平时待着就蔫
            if (!hyper) {
                s.liveMood = Math.max(0, s.liveMood - MOOD_DRAIN);
            } else {
                s.liveMood = 100; // 嗨完就停在满值，不会卡在 101
            }
            int mood = (int) s.liveMood;

            if (s.cooldown > 0) {
                s.cooldown--;
                continue;
            }
            if (hyper) {
                // 满血暴走：横冲直撞，每拍都动
                int dx = (rng.nextInt(3) - 1) * 3;
                int dy = (rng.nextInt(3) - 1) * 3;
                s.col = Math.min(COLS - 2, Math.max(1, s.col + dx));
                s.row = Math.min(ROWS - 2, Math.max(1, s.row + dy));
                s.cooldown = 0;
                continue;
            }
            // 心情够高才迈大步
            int reach = mood >= 70 ? 2 : 1;
            int dx = (rng.nextInt(3) - 1) * reach;
            int dy = (rng.nextInt(3) - 1) * reach;
            s.col = Math.min(COLS - 2, Math.max(1, s.col + dx));
            s.row = Math.min(ROWS - 2, Math.max(1, s.row + dy));
            // 冷却拉开差距：满心情几乎每拍都走，没心情要歇十几拍
            s.cooldown = (100 - mood) / 6;
        }
    }

    // 真实心情（0–100），不是动物就当 0
    private int realMood(FarmObject o) {
        return (o instanceof Animal) ? ((Animal) o).getMood() : 0;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        Font mono = new Font(Font.MONOSPACED, Font.BOLD, FONT_SIZE);
        g2.setFont(mono);
        FontMetrics fm = g2.getFontMetrics();
        int cw = fm.charWidth('W');
        int chh = fm.getHeight();
        int gw = COLS * cw;
        int gh = ROWS * chh;
        int ox = Math.max(8, (getWidth() - gw) / 2);
        int oy = 6;

        // 记下这次的位置，拖动时要用
        lastOx = ox;
        lastOy = oy;
        lastCw = cw;
        lastChh = chh;

        // 草地底色
        g2.setColor(FIELD);
        g2.fillRect(ox, oy, gw, gh);

        // 栅栏 + 草地上撒点纹理
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                boolean border = (r == 0 || r == ROWS - 1 || c == 0 || c == COLS - 1);
                char ch;
                Color col;
                if (border) {
                    if (r == 0 || r == ROWS - 1) {
                        ch = (c == 0 || c == COLS - 1) ? '+' : '-';
                    } else {
                        ch = '|';
                    }
                    col = FENCE;
                } else {
                    // 用坐标算出来的固定纹理，这样不会一帧一个样
                    ch = ((c * 7 + r * 13) % 11 == 0) ? '\"'
                            : (((c * 5 + r * 3) % 9 == 0) ? '.' : ' ');
                    col = GRASS;
                }
                if (ch != ' ') {
                    g2.setColor(col);
                    g2.drawString(String.valueOf(ch),
                            ox + c * cw, oy + r * chh + fm.getAscent());
                }
            }
        }

        // 把动物植物工具画上去
        for (Sprite s : sprites) {
            // 暴走的动物换个亮眼颜色和字形，一眼能看出来
            if (s.mobile && s.hyperTicks > 0) {
                g2.setColor(new Color(0xFF2D6F));
                g2.drawString("@", ox + s.col * cw, oy + s.row * chh + fm.getAscent());
            } else {
                g2.setColor(s.color);
                g2.drawString(String.valueOf(s.glyph),
                        ox + s.col * cw, oy + s.row * chh + fm.getAscent());
            }
        }
        g2.dispose();
    }
}
