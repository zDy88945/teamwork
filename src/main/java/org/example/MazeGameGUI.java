package org.example;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * 迷宫游戏 · 图形界面版（Swing）
 *
 * 和控制台版的区别：用方向键 / WASD 即时移动，不用回车。
 * 迷宫生成（递归回溯）和最短路径（BFS）算法与控制台版相同。
 *
 * 操作：
 *   方向键 或 WASD  移动
 *   H                显示到出口的最短路径提示
 *   R                重新开始本局
 */
public class MazeGameGUI extends JPanel {

    // 地图字符（用于生成迷宫，绘制时直接判断字符）
    static final char WALL = '#';
    static final char PATH = ' ';
    static final char TRAIL = '.';

    static final int LEVELS = 3;      // 一局的总关卡数
    static final int FOG_RADIUS = 2;  // 迷雾可视半径（切比雪夫距离）
    static final Random RANDOM = new Random();

    // 配色
    static final Color WALL_COLOR = new Color(44, 44, 66);
    static final Color PATH_COLOR = new Color(245, 245, 248);
    static final Color TRAIL_COLOR = new Color(210, 222, 240);
    static final Color PLAYER_COLOR = new Color(232, 72, 72);
    static final Color EXIT_COLOR = new Color(60, 200, 90);
    static final Color HINT_COLOR = new Color(255, 214, 90);
    static final Color FOG_COLOR = new Color(22, 22, 34);

    /** 难度配置：迷宫基础大小、提示次数、是否带迷雾 */
    static class Difficulty {
        final String name;
        final int baseSize;
        final int hints;
        final boolean fog;

        Difficulty(String name, int baseSize, int hints, boolean fog) {
            this.name = name;
            this.baseSize = baseSize;
            this.hints = hints;
            this.fog = fog;
        }
    }

    static final Difficulty[] DIFFICULTIES = {
        new Difficulty("简单", 6, 5, false),
        new Difficulty("普通", 9, 3, false),
        new Difficulty("困难", 12, 1, true),
    };

    // 游戏状态
    private Difficulty diff;
    private char[][] maze;
    private int pr, pc;      // 玩家位置
    private int er, ec;      // 出口位置
    private int level = 1;
    private int steps;
    private int hintsLeft;
    private int totalScore;
    private long levelStartNanos;
    private Set<String> hintPath = Collections.emptySet();

    // 撞墙提示（短暂显示）
    private String flash;
    private long flashUntil;

    private final JLabel statusLabel = new JLabel();
    private final MazePanel mazePanel = new MazePanel();

    public MazeGameGUI() {
        super(new BorderLayout());

        statusLabel.setFont(new Font("Dialog", Font.BOLD, 13));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(235, 235, 240));
        add(statusLabel, BorderLayout.NORTH);
        add(mazePanel, BorderLayout.CENTER);

        bindKeys();

        // 每 100ms 刷新一次状态栏和画面（用于计时）
        new Timer(100, e -> {
            updateStatus();
            mazePanel.repaint();
        }).start();
    }

    /** 绑定方向键 / WASD / H / R，用 InputMap+ActionMap，不依赖焦点 */
    private void bindKeys() {
        bind(KeyEvent.VK_UP, () -> movePlayer(-1, 0));
        bind(KeyEvent.VK_W, () -> movePlayer(-1, 0));
        bind(KeyEvent.VK_DOWN, () -> movePlayer(1, 0));
        bind(KeyEvent.VK_S, () -> movePlayer(1, 0));
        bind(KeyEvent.VK_LEFT, () -> movePlayer(0, -1));
        bind(KeyEvent.VK_A, () -> movePlayer(0, -1));
        bind(KeyEvent.VK_RIGHT, () -> movePlayer(0, 1));
        bind(KeyEvent.VK_D, () -> movePlayer(0, 1));
        bind(KeyEvent.VK_H, this::showHint);
        bind(KeyEvent.VK_R, this::restartGame);
    }

    private void bind(int keyCode, Runnable action) {
        String key = "k" + keyCode;
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        im.put(KeyStroke.getKeyStroke(keyCode, 0), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    // ---------------- 游戏流程 ----------------

    /** 选难度并开始第一关（窗口显示后调用） */
    public void startGame() {
        String[] options = new String[DIFFICULTIES.length];
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            options[i] = DIFFICULTIES[i].name;
        }
        int choice = JOptionPane.showOptionDialog(
            this,
            "选择难度：\n简单：提示 5 次\n普通：提示 3 次\n困难：提示 1 次，带战争迷雾",
            "迷宫游戏",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null, options, options[1]);
        if (choice < 0) {
            choice = 1; // 关掉对话框就默认普通
        }
        diff = DIFFICULTIES[choice];
        totalScore = 0;
        level = 1;
        startLevel();
    }

    /** 开始当前关 */
    private void startLevel() {
        int size = diff.baseSize + (level - 1) * 2; // 迷宫逐关变大
        maze = generateMaze(size);
        pr = 1;
        pc = 1;
        er = maze.length - 2;
        ec = maze[0].length - 2;
        steps = 0;
        hintsLeft = diff.hints;
        hintPath = Collections.emptySet();
        flash = null;
        levelStartNanos = System.nanoTime();
        mazePanel.repaint();
    }

    /** 重新开始本局（同难度，从第 1 关） */
    private void restartGame() {
        totalScore = 0;
        level = 1;
        startLevel();
    }

    /** 移动一步 */
    private void movePlayer(int dr, int dc) {
        if (maze == null) {
            return;
        }
        int nr = pr + dr;
        int nc = pc + dc;
        if (nr < 0 || nr >= maze.length || nc < 0 || nc >= maze[0].length || maze[nr][nc] == WALL) {
            flash = "撞墙啦！";
            flashUntil = System.currentTimeMillis() + 600;
            return;
        }
        maze[pr][pc] = TRAIL;
        pr = nr;
        pc = nc;
        steps++;

        if (pr == er && pc == ec) {
            levelCleared();
        }
    }

    /** 用 BFS 显示到出口的最短路径 */
    private void showHint() {
        if (maze == null || hintsLeft <= 0) {
            flash = "提示次数用完了！";
            flashUntil = System.currentTimeMillis() + 600;
            return;
        }
        hintPath = pathToSet(findPath(maze, pr, pc, er, ec), pr, pc, er, ec);
        hintsLeft--;
    }

    /** 过关：结算本关分数，进入下一关或通关 */
    private void levelCleared() {
        long seconds = (System.nanoTime() - levelStartNanos) / 1_000_000_000L;
        int score = scoreFor(steps, seconds, hintsLeft);
        totalScore += score;

        JOptionPane.showMessageDialog(this,
            "🎉 通过第 " + level + " 关！\n步数 " + steps + "，用时 " + seconds + " 秒\n本关得分 " + score + "，累计 " + totalScore,
            "过关", JOptionPane.INFORMATION_MESSAGE);

        if (level >= LEVELS) {
            int again = JOptionPane.showConfirmDialog(this,
                "恭喜通关！总分 " + totalScore + "\n再来一局？",
                "通关", JOptionPane.YES_NO_OPTION);
            if (again == JOptionPane.YES_OPTION) {
                startGame();
            } else {
                System.exit(0);
            }
        } else {
            level++;
            startLevel();
        }
    }

    /** 得分 = 基础分 - 步数扣分 - 时间扣分 + 提示加分，最低 0 */
    static int scoreFor(int steps, long seconds, int hintsLeft) {
        int score = 2000 - steps * 10 - (int) seconds * 5 + hintsLeft * 50;
        return Math.max(0, score);
    }

    // ---------------- 迷宫生成 ----------------

    /** "递归回溯"算法生成迷宫：保证任意两点之间有且仅有一条路 */
    static char[][] generateMaze(int size) {
        int rows = size * 2 + 1;
        int cols = size * 2 + 1;
        char[][] maze = new char[rows][cols];
        for (char[] row : maze) {
            Arrays.fill(row, WALL);
        }
        carve(maze, 1, 1);
        return maze;
    }

    static void carve(char[][] maze, int r, int c) {
        maze[r][c] = PATH;
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        Collections.shuffle(Arrays.asList(dirs), RANDOM);
        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr <= 0 || nr >= maze.length - 1 || nc <= 0 || nc >= maze[0].length - 1) {
                continue;
            }
            if (maze[nr][nc] == WALL) {
                maze[r + d[0] / 2][c + d[1] / 2] = PATH;
                carve(maze, nr, nc);
            }
        }
    }

    // ---------------- 最短路径（BFS） ----------------

    static List<int[]> findPath(char[][] maze, int sr, int sc, int er, int ec) {
        int rows = maze.length, cols = maze[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int[][] prev = new int[rows][cols];
        for (int[] row : prev) {
            Arrays.fill(row, -1);
        }
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{sr, sc});
        visited[sr][sc] = true;
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            if (cur[0] == er && cur[1] == ec) {
                break;
            }
            for (int[] d : dirs) {
                int nr = cur[0] + d[0];
                int nc = cur[1] + d[1];
                if (nr < 0 || nr >= rows || nc < 0 || nc >= cols || visited[nr][nc]) {
                    continue;
                }
                if (maze[nr][nc] == WALL) {
                    continue;
                }
                visited[nr][nc] = true;
                prev[nr][nc] = cur[0] * cols + cur[1];
                queue.add(new int[]{nr, nc});
            }
        }

        if (!visited[er][ec]) {
            return Collections.emptyList();
        }
        List<int[]> path = new ArrayList<>();
        int r = er, c = ec;
        while (!(r == sr && c == sc)) {
            path.add(new int[]{r, c});
            int p = prev[r][c];
            r = p / cols;
            c = p % cols;
        }
        path.add(new int[]{sr, sc});
        Collections.reverse(path);
        return path;
    }

    static Set<String> pathToSet(List<int[]> path, int sr, int sc, int er, int ec) {
        Set<String> set = new HashSet<>();
        for (int[] p : path) {
            if ((p[0] == sr && p[1] == sc) || (p[0] == er && p[1] == ec)) {
                continue;
            }
            set.add(p[0] + "," + p[1]);
        }
        return set;
    }

    static boolean inSight(int pr, int pc, int r, int c) {
        return Math.max(Math.abs(r - pr), Math.abs(c - pc)) <= FOG_RADIUS;
    }

    // ---------------- 状态栏与绘制 ----------------

    private void updateStatus() {
        long seconds = (maze == null) ? 0 : (System.nanoTime() - levelStartNanos) / 1_000_000_000L;
        StringBuilder sb = new StringBuilder();
        sb.append("关卡 ").append(level).append('/').append(LEVELS)
          .append("  |  步数 ").append(steps)
          .append("  |  用时 ").append(seconds).append('s')
          .append("  |  总分 ").append(totalScore)
          .append("  |  提示剩 ").append(hintsLeft);
        if (flash != null && System.currentTimeMillis() < flashUntil) {
            sb.append("  |  ⚠ ").append(flash);
        }
        sb.append("    （方向键/WASD 移动 · H 提示 · R 重开）");
        statusLabel.setText(sb.toString());
    }

    /** 负责画迷宫的面板 */
    private class MazePanel extends JPanel {
        MazePanel() {
            setPreferredSize(new Dimension(640, 640));
            setBackground(FOG_COLOR);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (maze == null) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int rows = maze.length, cols = maze[0].length;
            int cell = Math.min(getWidth() / cols, getHeight() / rows);
            if (cell < 2) {
                cell = 2;
            }
            int ox = (getWidth() - cell * cols) / 2;   // 居中偏移
            int oy = (getHeight() - cell * rows) / 2;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = ox + c * cell, y = oy + r * cell;

                    if (diff.fog && !inSight(pr, pc, r, c)) {
                        g2.setColor(FOG_COLOR);
                        g2.fillRect(x, y, cell, cell);
                        continue;
                    }
                    if (hintPath.contains(r + "," + c)) {
                        g2.setColor(HINT_COLOR);
                    } else if (maze[r][c] == WALL) {
                        g2.setColor(WALL_COLOR);
                    } else if (maze[r][c] == TRAIL) {
                        g2.setColor(TRAIL_COLOR);
                    } else {
                        g2.setColor(PATH_COLOR);
                    }
                    g2.fillRect(x, y, cell, cell);

                    // 出口画成绿色小方块
                    if (r == er && c == ec) {
                        g2.setColor(EXIT_COLOR);
                        g2.fillRect(x + cell / 4, y + cell / 4, cell / 2, cell / 2);
                    }
                }
            }

            // 玩家画成红色圆
            int px = ox + pc * cell, py = oy + pr * cell;
            g2.setColor(PLAYER_COLOR);
            g2.fillOval(px + cell / 5, py + cell / 5, cell * 3 / 5, cell * 3 / 5);
        }
    }

    // ---------------- 入口 ----------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("迷宫游戏 · 图形版");
            MazeGameGUI game = new MazeGameGUI();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(game);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            game.startGame();
        });
    }
}
