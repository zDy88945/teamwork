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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

/**
 * 迷宫游戏（Swing 图形版，最简）
 *
 * 玩法：方向键 或 WASD 移动红色小球，走到右下角绿色出口即获胜。
 * R 键重新生成迷宫。
 *
 * 迷宫用"递归回溯"算法随机生成，保证一定有路。
 */
public class MazeGameGUI extends JPanel {

    // 地图字符
    static final char WALL = '#';
    static final char PATH = ' ';
    static final char TRAIL = '.';

    static final int SIZE = 15; // 15 x 15 个格子

    // 配色
    static final Color WALL_COLOR = new Color(44, 44, 66);
    static final Color PATH_COLOR = new Color(245, 245, 248);
    static final Color TRAIL_COLOR = new Color(210, 222, 240);
    static final Color PLAYER_COLOR = new Color(232, 72, 72);
    static final Color EXIT_COLOR = new Color(60, 200, 90);

    private final Random random = new Random();

    private char[][] maze;
    private int pr, pc; // 玩家位置
    private int er, ec; // 出口位置
    private int steps;

    private final JLabel statusLabel = new JLabel();
    private final MazePanel mazePanel = new MazePanel();

    public MazeGameGUI() {
        super(new BorderLayout());

        statusLabel.setFont(new Font("Dialog", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(235, 235, 240));
        add(statusLabel, BorderLayout.NORTH);
        add(mazePanel, BorderLayout.CENTER);

        bindKeys();
        newGame();
    }

    /** 生成新迷宫并回到起点 */
    private void newGame() {
        maze = generateMaze(SIZE);
        pr = 1;
        pc = 1;
        er = maze.length - 2;
        ec = maze[0].length - 2;
        steps = 0;
        updateStatus();
        mazePanel.repaint();
    }

    /** 绑定方向键 / WASD / R，用 InputMap+ActionMap，不依赖焦点 */
    private void bindKeys() {
        bind(KeyEvent.VK_UP, () -> movePlayer(-1, 0));
        bind(KeyEvent.VK_W, () -> movePlayer(-1, 0));
        bind(KeyEvent.VK_DOWN, () -> movePlayer(1, 0));
        bind(KeyEvent.VK_S, () -> movePlayer(1, 0));
        bind(KeyEvent.VK_LEFT, () -> movePlayer(0, -1));
        bind(KeyEvent.VK_A, () -> movePlayer(0, -1));
        bind(KeyEvent.VK_RIGHT, () -> movePlayer(0, 1));
        bind(KeyEvent.VK_D, () -> movePlayer(0, 1));
        bind(KeyEvent.VK_R, this::newGame);
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

    /** 移动一步 */
    private void movePlayer(int dr, int dc) {
        if (maze == null) {
            return;
        }
        int nr = pr + dr;
        int nc = pc + dc;
        if (nr < 0 || nr >= maze.length || nc < 0 || nc >= maze[0].length || maze[nr][nc] == WALL) {
            return; // 撞墙不动
        }
        maze[pr][pc] = TRAIL;
        pr = nr;
        pc = nc;
        steps++;
        updateStatus();
        mazePanel.repaint();

        if (pr == er && pc == ec) {
            int again = JOptionPane.showConfirmDialog(this,
                "🎉 你赢了！共走了 " + steps + " 步。\n再来一局？",
                "获胜", JOptionPane.YES_NO_OPTION);
            if (again == JOptionPane.YES_OPTION) {
                newGame();
            } else {
                System.exit(0);
            }
        }
    }

    private void updateStatus() {
        statusLabel.setText("步数 " + steps + "     （方向键/WASD 移动 · R 重开）");
    }

    // ---------------- 迷宫生成（递归回溯） ----------------

    /** 生成 size x size 个格子的迷宫，网格 (2*size+1) 见方 */
    private char[][] generateMaze(int size) {
        int rows = size * 2 + 1;
        int cols = size * 2 + 1;
        char[][] m = new char[rows][cols];
        for (char[] row : m) {
            Arrays.fill(row, WALL);
        }
        carve(m, 1, 1);
        return m;
    }

    /** 从 (r,c) 出发随机挖路：每次跳两格，并打通中间的墙 */
    private void carve(char[][] m, int r, int c) {
        m[r][c] = PATH;
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        Collections.shuffle(Arrays.asList(dirs), random); // 随机顺序，迷宫才不一样
        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr <= 0 || nr >= m.length - 1 || nc <= 0 || nc >= m[0].length - 1) {
                continue; // 超出迷宫范围
            }
            if (m[nr][nc] == WALL) {
                m[r + d[0] / 2][c + d[1] / 2] = PATH; // 打通中间那堵墙
                carve(m, nr, nc);
            }
        }
    }

    // ---------------- 绘制 ----------------

    /** 负责画迷宫的面板 */
    private class MazePanel extends JPanel {
        MazePanel() {
            setPreferredSize(new Dimension(640, 640));
            setBackground(WALL_COLOR);
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
            int ox = (getWidth() - cell * cols) / 2;
            int oy = (getHeight() - cell * rows) / 2;

            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    int x = ox + c * cell, y = oy + r * cell;
                    if (maze[r][c] == WALL) {
                        g2.setColor(WALL_COLOR);
                    } else if (maze[r][c] == TRAIL) {
                        g2.setColor(TRAIL_COLOR);
                    } else {
                        g2.setColor(PATH_COLOR);
                    }
                    g2.fillRect(x, y, cell, cell);

                    // 出口画成绿色方块
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
            JFrame frame = new JFrame("迷宫游戏");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setContentPane(new MazeGameGUI());
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
