package happyfarm.gui;

import happyfarm.core.Farm;
import happyfarm.core.FarmResult;
import happyfarm.io.SaveManager;
import happyfarm.model.Animal;
import happyfarm.model.FarmObject;
import happyfarm.model.Plant;
import happyfarm.model.ToolItem;
import happyfarm.util.FarmUtils;
import happyfarm.util.SortUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.ExecutionException;

/**
 * 农场图形界面（成员 B 负责），采用 Swing 三栏布局：
 *  左栏：输入框、类型下拉框、各类操作按钮。
 *  中栏：按农场行显示对象按钮，点击可选中对象。
 *  右栏：结果文本区，显示每次操作的反馈。
 *
 * 约定（与存档格式、成员 A 接口保持一致）：
 *  - 界面上的“行号”从 1 开始输入，内部调用 Farm 时转换成从 0 开始的 rowIndex。
 *  - “位置”从 0 开始，与存档格式一致。
 *  - GUI 不直接修改 Farm 内部容器，只调用其公开方法。
 *  - 保存、读取等 IO 操作放入 SwingWorker 子线程，避免界面卡死。
 */
public class FarmGUI extends JFrame {

    /** 新建农场时的默认行数。 */
    private static final int DEFAULT_ROW_COUNT = 3;

    /** 结果区时间戳格式。 */
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    /** 操作序号计数器，用于结果区分块编号。 */
    private int logSeq = 0;

    /** 当前农场，可能为 null（尚未初始化）。 */
    private Farm farm;
    /** 存档管理器，由启动类注入。 */
    private final SaveManager saveManager;

    // ===== 左栏输入组件 =====
    private JComboBox<String> typeComboBox;
    private JTextField nameField;
    private JTextField priorityField;
    private JTextField rowField;
    private JTextField positionField;

    // ===== 左栏操作按钮 =====
    private JButton initButton;
    private JButton addButton;
    private JButton queryButton;
    private JButton careButton;
    private JButton deleteButton;
    private JButton clearButton;
    private JButton saveButton;
    private JButton loadButton;
    private JButton sortByTypeButton;
    private JButton sortByPriorityButton;

    // ===== 中栏 / 右栏 =====
    private JPanel farmPanel;
    /** 底部会动的 ASCII 农场（纯娱乐，动物随机走动）。 */
    private AsciiFarmPanel asciiFarm;
    /** 右上属性板：随点击切换显示当前选中对象的 RPG 属性卡（自定义绘制）。 */
    private AttrCardPanel attrCard;
    /** 属性板当前显示的对象，用于暴走态结束时刷新它。 */
    private FarmObject attrObject;
    /** 右下操作日志：只记录简洁的操作流水。 */
    private JTextArea resultArea;

    public FarmGUI(Farm farm, SaveManager saveManager) {
        super("快乐农场对象管理系统");
        this.farm = farm;
        this.saveManager = saveManager;
        initComponents();
        bindEvents();
        refreshFarmPanel();
    }

    /** 创建并布局所有组件。 */
    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1080, 740);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(buildLeftPanel(), BorderLayout.WEST);
        add(buildFarmPanel(), BorderLayout.CENTER);
        add(buildResultPanel(), BorderLayout.EAST);
        add(buildAsciiFarm(), BorderLayout.SOUTH);
    }

    /** 左栏：输入区 + 操作按钮。 */
    private JPanel buildLeftPanel() {
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        leftPanel.setPreferredSize(new Dimension(280, 0));

        // ---- 输入区 ----
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.setBorder(BorderFactory.createTitledBorder("对象信息 / 位置"));

        typeComboBox = new JComboBox<>(new String[] {"动物", "植物", "工具"});
        nameField = new JTextField();
        priorityField = new JTextField("1");
        rowField = new JTextField(String.valueOf(DEFAULT_ROW_COUNT));
        positionField = new JTextField("0");

        form.add(new JLabel("类型"));
        form.add(typeComboBox);
        form.add(new JLabel("名称"));
        form.add(nameField);
        form.add(new JLabel("优先级(≥1)"));
        form.add(priorityField);
        form.add(new JLabel("行号(从1)"));
        form.add(rowField);
        form.add(new JLabel("位置(从0)"));
        form.add(positionField);
        leftPanel.add(form);

        // ---- 操作按钮区 ----
        JPanel actions = new JPanel(new GridLayout(0, 2, 6, 6));
        actions.setBorder(BorderFactory.createTitledBorder("操作"));

        initButton = new JButton("初始化农场");
        addButton = new JButton("添加对象");
        queryButton = new JButton("查询");
        careButton = new JButton("照料");
        deleteButton = new JButton("删除");
        clearButton = new JButton("清空农场");
        saveButton = new JButton("保存游戏");
        loadButton = new JButton("读取存档");
        sortByTypeButton = new JButton("按类别排序");
        sortByPriorityButton = new JButton("按优先级排序");

        actions.add(initButton);
        actions.add(addButton);
        actions.add(queryButton);
        actions.add(careButton);
        actions.add(deleteButton);
        actions.add(clearButton);
        actions.add(saveButton);
        actions.add(loadButton);
        actions.add(sortByTypeButton);
        actions.add(sortByPriorityButton);
        leftPanel.add(actions);

        return leftPanel;
    }

    /** 中栏：农场图形区，外层包一个滚动面板。 */
    private JScrollPane buildFarmPanel() {
        farmPanel = new JPanel();
        farmPanel.setLayout(new BoxLayout(farmPanel, BoxLayout.Y_AXIS));
        farmPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(farmPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder("农场（点击对象可选中）"));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    /**
     * 右栏：上下分割。上半是「属性板」，点击对象后切换显示其 RPG 属性卡；
     * 下半是「操作日志」，记录每次操作的简洁流水。两者用 JSplitPane 分隔，
     * 用户可拖动调整上下比例。
     */
    private JComponent buildResultPanel() {
        // ---- 右上：属性板（自定义绘制的 RPG 属性卡）----
        attrCard = new AttrCardPanel();
        JScrollPane attrScroll = new JScrollPane(attrCard);
        attrScroll.setBorder(BorderFactory.createTitledBorder("属性板（点击农场对象查看）"));
        attrScroll.getVerticalScrollBar().setUnitIncrement(16);

        // ---- 右下：操作日志 ----
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(resultArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("操作日志"));

        JSplitPane split = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, attrScroll, logScroll);
        split.setResizeWeight(0.55); // 属性卡略大一些，拖动后保持比例
        split.setPreferredSize(new Dimension(360, 0));
        return split;
    }

    /** 底部：会动的 ASCII 农场，外面套个标题边框。 */
    private JComponent buildAsciiFarm() {
        asciiFarm = new AsciiFarmPanel();
        asciiFarm.setBorder(BorderFactory.createTitledBorder("农场一角（动物会自己溜达）"));
        // 暴走结束时，如果属性板正显示这只动物，刷新一下把「爽翻天」还原成真实状态
        asciiFarm.setHyperListener(() -> showAttr(attrObject));
        syncAsciiFarm();
        return asciiFarm;
    }

    /** 把当前农场所有对象同步给底部 ASCII 农场（农场为空则清空）。 */
    private void syncAsciiFarm() {
        if (asciiFarm == null) {
            return;
        }
        asciiFarm.setObjects(farm == null ? null : farm.getAllObjects());
    }

    /** 向右侧结果区追加一行消息，并滚动到底部。 */
    public void appendResult(String message) {
        resultArea.append(message + System.lineSeparator());
        resultArea.setCaretPosition(resultArea.getDocument().getLength());
    }

    /**
     * 以「分块」形式记录一次操作：带序号、时间和标题的表头，
     * 多行详情统一缩进，块尾用分隔线隔开，使右侧日志清晰不杂乱。
     *
     * @param title  操作标题，例如「添加对象」「照料」
     * @param detail 操作详情，可有多行（用换行分隔），允许为空
     */
    private void logEntry(String title, String detail) {
        logSeq++;
        StringBuilder sb = new StringBuilder();
        sb.append("#").append(logSeq)
                .append("  [").append(LocalTime.now().format(TIME_FORMAT)).append("]  ")
                .append(title)
                .append(System.lineSeparator());
        if (detail != null && !detail.isEmpty()) {
            for (String line : detail.split("\\R")) {
                sb.append("    ").append(line).append(System.lineSeparator());
            }
        }
        sb.append("────────────────────");
        appendResult(sb.toString());
    }

    // ==================== 事件绑定 ====================

    /**
     * 绑定所有按钮事件。
     * 多数按钮使用 Lambda 表达式；查询按钮特意使用匿名内部类，
     * 以便体现两种事件处理写法（满足课程知识点检查）。
     */
    private void bindEvents() {
        initButton.addActionListener(e -> onInitClicked());
        addButton.addActionListener(e -> onAddClicked());

        // 匿名内部类写法，保留一个作为知识点示例。
        queryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onQueryClicked();
            }
        });

        careButton.addActionListener(e -> onCareClicked());
        deleteButton.addActionListener(e -> onDeleteClicked());
        clearButton.addActionListener(e -> onClearClicked());
        saveButton.addActionListener(e -> onSaveClicked());
        loadButton.addActionListener(e -> onLoadClicked());
        sortByTypeButton.addActionListener(e -> onSortByTypeClicked());
        sortByPriorityButton.addActionListener(e -> onSortByPriorityClicked());
    }

    // ==================== 输入读取与校验 ====================

    /** 读取“行号”输入框，转换成从 0 开始的内部下标。 */
    private int readRowIndex() {
        int row = FarmUtils.parsePositiveInt(rowField.getText(), "行号");
        return row - 1;
    }

    /** 读取“位置”输入框（从 0 开始）。 */
    private int readPosition() {
        return FarmUtils.parseNonNegativeInt(positionField.getText(), "位置");
    }

    /**
     * 根据左侧输入框创建一个农场对象。
     * 优先级强制 ≥ 1，与存档解析（parsePositiveInt）保持一致，避免存档读不回来。
     */
    private FarmObject buildObjectFromInput() {
        String name = FarmUtils.normalizeName(nameField.getText());
        int priority = FarmUtils.parsePositiveInt(priorityField.getText(), "优先级");
        String type = (String) typeComboBox.getSelectedItem();
        switch (type) {
            case "动物":
                return new Animal(name, priority);
            case "植物":
                return new Plant(name, priority);
            case "工具":
                return new ToolItem(name, priority);
            default:
                throw new IllegalArgumentException("未知的对象类型：" + type);
        }
    }

    /** 是否已经有农场，没有则提示并返回 false。 */
    private boolean requireFarm() {
        if (farm == null) {
            logEntry("提示", "当前没有农场，请先输入行数并点击“初始化农场”。");
            return false;
        }
        return true;
    }

    // ==================== 事件处理 ====================

    /** 初始化农场：用“行号”输入框的数字作为行数（≥1）。 */
    private void onInitClicked() {
        try {
            int rowCount = FarmUtils.parsePositiveInt(rowField.getText(), "行数");
            farm = new Farm(rowCount);
            logEntry("初始化农场", "成功：新建 " + rowCount + " 行农场。");
            showAttr(null); // 新农场无选中对象
            refreshFarmPanel();
        } catch (RuntimeException ex) {
            logEntry("初始化农场", "失败：" + ex.getMessage());
        }
    }

    /** 添加对象到指定行和位置。 */
    private void onAddClicked() {
        if (!requireFarm()) {
            return;
        }
        try {
            FarmObject object = buildObjectFromInput();
            int rowIndex = readRowIndex();
            int position = readPosition();
            FarmResult result = farm.addObject(rowIndex, position, object);
            logEntry("添加对象", result.getMessage());
            if (result.isSuccess()) {
                showAttr(object);
                refreshFarmPanel();
            }
        } catch (RuntimeException ex) {
            logEntry("添加对象", "失败：" + ex.getMessage());
        }
    }

    /** 按名称模糊查询，结果输出到右侧。 */
    private void onQueryClicked() {
        if (!requireFarm()) {
            return;
        }
        String keyword = nameField.getText();
        if (FarmUtils.isBlank(keyword)) {
            logEntry("查询", "失败：请输入名称关键字。");
            return;
        }
        ArrayList<FarmObject> found = farm.queryByName(keyword);
        if (found.isEmpty()) {
            logEntry("查询", "未找到对象：" + keyword.trim());
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("关键字“").append(keyword.trim()).append("”匹配 ")
                    .append(found.size()).append(" 个：");
            for (FarmObject object : found) {
                sb.append(System.lineSeparator()).append("  · ").append(object.getName())
                        .append("（").append(object.getTypeLabel()).append("）");
            }
            logEntry("查询", sb.toString());
            // 把第一个匹配对象显示到属性板，方便直接查看属性。
            showAttr(found.get(0));
        }
    }

    /** 照料选中位置的对象。 */
    private void onCareClicked() {
        if (!requireFarm()) {
            return;
        }
        try {
            int rowIndex = readRowIndex();
            int position = readPosition();
            FarmResult result = farm.careObject(rowIndex, position);
            logEntry("照料", result.getMessage());
            if (result.isSuccess()) {
                refreshFarmPanel(); // 先刷新，让底部农场判定是否进入暴走
                showAttr(farm.getObjectAt(rowIndex, position));
            }
        } catch (RuntimeException ex) {
            logEntry("照料", "失败：" + ex.getMessage());
        }
    }

    /** 删除选中位置的对象。 */
    private void onDeleteClicked() {
        if (!requireFarm()) {
            return;
        }
        try {
            FarmResult result = farm.removeObject(readRowIndex(), readPosition());
            logEntry("删除", result.getMessage());
            if (result.isSuccess()) {
                showAttr(null); // 对象已删除，清空属性板
                refreshFarmPanel();
            }
        } catch (RuntimeException ex) {
            logEntry("删除", "失败：" + ex.getMessage());
        }
    }

    /** 清空农场但保留行数。 */
    private void onClearClicked() {
        if (!requireFarm()) {
            return;
        }
        FarmResult result = farm.clearFarm();
        logEntry("清空农场", result.getMessage());
        showAttr(null); // 农场已清空，重置属性板
        refreshFarmPanel();
    }

    /** 按类别排序显示（不改变农场实际位置）。 */
    private void onSortByTypeClicked() {
        if (!requireFarm()) {
            return;
        }
        logEntry("按类别排序",
                SortUtil.formatObjects(SortUtil.sortByType(farm.getAllObjects())));
    }

    /** 按优先级从高到低排序显示。 */
    private void onSortByPriorityClicked() {
        if (!requireFarm()) {
            return;
        }
        logEntry("按优先级排序（高 -> 低）",
                SortUtil.formatObjects(SortUtil.sortByPriority(farm.getAllObjects())));
    }

    /** 点击对象按钮后，把行号和位置同步回输入框，并在右上属性板切换显示该对象。 */
    private void updateSelectedPosition(int rowIndex, int position) {
        rowField.setText(String.valueOf(rowIndex + 1));
        positionField.setText(String.valueOf(position));
        FarmObject object = farm.getObjectAt(rowIndex, position);
        showAttr(object);
        if (object != null) {
            logEntry("选中对象",
                    "第 " + (rowIndex + 1) + " 行第 " + position + " 位：" + object.getName());
        }
    }

    // ==================== 中栏刷新 ====================

    /**
     * 根据当前农场重建中间区域。每一行一个横向面板，行内每个对象渲染成按钮，
     * 点击按钮调用 updateSelectedPosition 选中该对象。对象按钮的事件在此动态创建。
     */
    private void refreshFarmPanel() {
        farmPanel.removeAll();

        if (farm == null) {
            JLabel empty = new JLabel("暂无农场，请输入行数后点击“初始化农场”。");
            empty.setBorder(new EmptyBorder(8, 8, 8, 8));
            farmPanel.add(empty);
        } else {
            ArrayList<LinkedList<FarmObject>> rows = farm.getRows();
            for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                farmPanel.add(buildRowPanel(rowIndex, rows.get(rowIndex)));
            }
        }

        // 重新布局并重绘，确保按钮立即显示/消失。
        farmPanel.revalidate();
        farmPanel.repaint();

        // 同步底部 ASCII 农场（新增对象随机落位，消失的移除，动物保留原位）。
        syncAsciiFarm();
    }

    /** 构建农场某一行的面板。 */
    private JPanel buildRowPanel(int rowIndex, LinkedList<FarmObject> row) {
        JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        rowPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JLabel label = new JLabel("第 " + (rowIndex + 1) + " 行：");
        rowPanel.add(label);

        if (row.isEmpty()) {
            rowPanel.add(new JLabel("（空）"));
        } else {
            for (int position = 0; position < row.size(); position++) {
                FarmObject object = row.get(position);
                // 按原始要求的样式：按钮上只显示「类型符号 + 名称」，不显示优先级。
                // 优先级仍可通过悬浮提示和右侧结果区查看。
                JButton objectButton = new JButton(buttonText(object));
                objectButton.setToolTipText(object.toString());
                // 在循环中需要 final 局部变量供 Lambda 捕获。
                final int capturedRow = rowIndex;
                final int capturedPosition = position;
                objectButton.addActionListener(
                        e -> updateSelectedPosition(capturedRow, capturedPosition));
                rowPanel.add(objectButton);
            }
        }
        return rowPanel;
    }

    /**
     * 农场按钮上显示的文字：类型符号 + 名称（贴近原始要求的界面样式，不显示优先级）。
     * 不修改成员 A 的 getDisplayText()，由 GUI 层根据对象类型自己拼接。
     */
    private String buttonText(FarmObject object) {
        return typeSymbol(object) + " " + object.getName();
    }

    /** 根据对象类型返回一个图形符号，区分动物 / 植物 / 工具。 */
    private String typeSymbol(FarmObject object) {
        if (object instanceof Animal) {
            return "♥"; // ♥
        }
        if (object instanceof Plant) {
            return "♣"; // ♣
        }
        if (object instanceof ToolItem) {
            return "⚒"; // ⚒
        }
        return "●"; // ● 兜底
    }

    /**
     * 把属性卡切换到右上属性板（自定义绘制，实现「点击切换」效果）。
     * 传 null 则恢复占位提示。具体绘制逻辑在 {@link AttrCardPanel}。
     */
    private void showAttr(FarmObject object) {
        attrObject = object;
        attrCard.setObject(object, asciiFarm != null && asciiFarm.isHyper(object));
    }


    /**
     * 保存游戏。先让用户输入存档名，再在 SwingWorker 子线程中写文件，
     * 避免大文件保存时界面卡死；完成后在 done() 中回到 EDT 刷新提示。
     */
    private void onSaveClicked() {
        if (!requireFarm()) {
            return;
        }
        String saveName = JOptionPane.showInputDialog(
                this, "请输入存档名（自动追加 .txt）：", "保存游戏",
                JOptionPane.QUESTION_MESSAGE);
        if (saveName == null) {
            return; // 用户取消
        }
        Path target = saveManager.createSavePath(saveName);
        saveInBackground(target);
    }

    /**
     * 读取存档。先扫描已有存档列表供用户选择，再在子线程读取。
     */
    private void onLoadClicked() {
        ArrayList<Path> saves;
        try {
            saves = saveManager.scanSaveFiles();
        } catch (Exception ex) {
            logEntry("读取存档", "扫描存档失败：" + ex.getMessage());
            return;
        }
        if (saves.isEmpty()) {
            logEntry("读取存档", "没有可读取的存档。");
            return;
        }

        String[] names = new String[saves.size()];
        for (int i = 0; i < saves.size(); i++) {
            names[i] = saves.get(i).getFileName().toString();
        }
        Object selected = JOptionPane.showInputDialog(
                this, "请选择要读取的存档：", "读取存档",
                JOptionPane.QUESTION_MESSAGE, null, names, names[0]);
        if (selected == null) {
            return; // 用户取消
        }
        for (Path path : saves) {
            if (path.getFileName().toString().equals(selected.toString())) {
                loadSaveInBackground(path);
                return;
            }
        }
    }

    /** 在子线程保存农场，完成后回到 EDT 提示结果。 */
    private void saveInBackground(Path path) {
        setActionButtonsEnabled(false);
        appendResult("正在保存到 " + path.getFileName() + " ……");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                saveManager.saveFarm(farm, path);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get(); // 触发子线程中抛出的异常
                    logEntry("保存游戏", "成功：" + path);
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    logEntry("保存游戏", "失败：" + cause.getMessage());
                } finally {
                    setActionButtonsEnabled(true);
                }
            }
        }.execute();
    }

    /** 在子线程读取存档，完成后回到 EDT 替换农场并刷新界面。 */
    private void loadSaveInBackground(Path path) {
        setActionButtonsEnabled(false);
        appendResult("正在读取 " + path.getFileName() + " ……");
        new SwingWorker<Farm, Void>() {
            @Override
            protected Farm doInBackground() throws Exception {
                return saveManager.loadFarm(path);
            }

            @Override
            protected void done() {
                try {
                    farm = get();
                    refreshFarmPanel();
                    showAttr(null); // 读取的新农场尚未选中对象
                    logEntry("读取存档", "成功：" + path.getFileName());
                } catch (InterruptedException | ExecutionException ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    logEntry("读取存档", "失败：" + cause.getMessage());
                } finally {
                    setActionButtonsEnabled(true);
                }
            }
        }.execute();
    }

    /** IO 进行中禁用保存/读取按钮，避免重复触发。 */
    private void setActionButtonsEnabled(boolean enabled) {
        saveButton.setEnabled(enabled);
        loadButton.setEnabled(enabled);
    }
}
