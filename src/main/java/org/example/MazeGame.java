package org.example;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;

/**
 * 控制台迷宫游戏（进阶版）
 *
 * 相比基础版新增：
 *  1. 难度选择 + 3 关连闯，迷宫逐关变大
 *  2. 计时 + 计分（步数越少、用时越短、剩余提示越多，得分越高）
 *  3. 提示系统：按 h 用 BFS 算出到出口的最短路径（限次数）
 *  4. 困难难度带"战争迷雾"：只能看到玩家周围一小圈
 *
 * 操作：w/a/s/d 移动，h 提示，q 退出（每次输入后回车）
 */
public class MazeGame {

    // 地图符号
    static final char WALL = '#';
    static final char PATH = ' ';
    static final char TRAIL = '.';
    static final char PLAYER = '@';
    static final char EXIT = 'E';
    static final char HINT = '*';   // 提示路径
    static final char FOG = '·';    // 迷雾（不可见区域）

    static final int LEVELS = 3;      // 一局的总关卡数
    static final int FOG_RADIUS = 2;  // 迷雾可视半径（切比雪夫距离）
    static final Random RANDOM = new Random();

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

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("=== 迷宫游戏 · 进阶版 ===");

        Difficulty[] difficulties = {
            new Difficulty("简单", 6, 5, false),
            new Difficulty("普通", 9, 3, false),
            new Difficulty("困难", 12, 1, true),
        };
        System.out.println("选择难度：");
        for (int i = 0; i < difficulties.length; i++) {
            Difficulty d = difficulties[i];
            System.out.println("  " + (i + 1) + " - " + d.name
                + "（提示 " + d.hints + " 次" + (d.fog ? "，带迷雾" : "") + "）");
        }
        int idx = readInt(sc, "请输入 1~" + difficulties.length + "（回车默认 2）：", 2) - 1;
        idx = Math.max(0, Math.min(difficulties.length - 1, idx));
        Difficulty diff = difficulties[idx];
        System.out.println("已选择：" + diff.name + "，共 " + LEVELS + " 关，迷宫逐关变大。");

        boolean playing = true;
        while (playing) {
            int totalScore = playGame(sc, diff);
            System.out.println("======================================");
            System.out.println("游戏结束！总分：" + totalScore);
            System.out.print("再来一局？(y/n)：");
            playing = sc.nextLine().trim().toLowerCase().startsWith("y");
        }
        System.out.println("再见！");
        sc.close();
    }

    /** 打完一整局（共 LEVELS 关），返回总分 */
    static int playGame(Scanner sc, Difficulty diff) {
        int totalScore = 0;
        for (int level = 1; level <= LEVELS; level++) {
            int size = diff.baseSize + (level - 1) * 2; // 迷宫逐关变大
            int levelScore = playLevel(sc, diff, level, size);
            if (levelScore < 0) {
                return totalScore; // -1 表示中途退出
            }
            totalScore += levelScore;
            System.out.println("第 " + level + " 关完成！本关得分 " + levelScore + "，累计 " + totalScore);
            System.out.println();
        }
        return totalScore;
    }

    /** 玩一关，返回本关得分；中途按 q 返回 -1 */
    static int playLevel(Scanner sc, Difficulty diff, int level, int size) {
        char[][] maze = generateMaze(size);
        int pr = 1, pc = 1;                        // 玩家位置
        int er = maze.length - 2, ec = maze[0].length - 2; // 出口位置
        int steps = 0;
        int hintsLeft = diff.hints;
        Set<String> hintPath = Collections.emptySet();
        long start = System.nanoTime();

        System.out.println("----- 第 " + level + " 关（迷宫 " + size + "x" + size + "）-----");
        System.out.println("w/a/s/d 移动，h 提示（剩 " + hintsLeft + " 次），q 退出");

        while (true) {
            long seconds = (System.nanoTime() - start) / 1_000_000_000L;
            printMaze(maze, pr, pc, er, ec, hintPath, diff.fog);
            System.out.println("关卡 " + level + " | 步数 " + steps + " | 用时 " + seconds + "s | 提示剩 " + hintsLeft);
            System.out.print("> ");

            String line = sc.nextLine().trim().toLowerCase();
            if (line.isEmpty()) {
                continue;
            }
            char key = line.charAt(0);

            if (key == 'q') {
                System.out.println("已退出本局。");
                return -1;
            }
            if (key == 'h') {
                if (hintsLeft <= 0) {
                    System.out.println("提示次数用完了！");
                    continue;
                }
                // 用 BFS 求从当前位置到出口的最短路径，并标成 *
                hintPath = pathToSet(findPath(maze, pr, pc, er, ec), pr, pc, er, ec);
                hintsLeft--;
                continue;
            }

            int dr = 0, dc = 0;
            if (key == 'w') {
                dr = -1;
            } else if (key == 's') {
                dr = 1;
            } else if (key == 'a') {
                dc = -1;
            } else if (key == 'd') {
                dc = 1;
            } else {
                System.out.println("无效按键，请用 w/a/s/d/h/q");
                continue;
            }

            int nr = pr + dr;
            int nc = pc + dc;
            if (maze[nr][nc] == WALL) {
                System.out.println("撞墙啦！");
                continue;
            }

            maze[pr][pc] = TRAIL; // 旧位置标记为走过的路
            pr = nr;
            pc = nc;
            steps++;

            if (pr == er && pc == ec) {
                long finishSeconds = (System.nanoTime() - start) / 1_000_000_000L;
                printMaze(maze, pr, pc, er, ec, Collections.emptySet(), diff.fog);
                int score = scoreFor(steps, finishSeconds, hintsLeft);
                System.out.println("🎉 走出第 " + level + " 关！用时 " + finishSeconds + "s，步数 " + steps + "，得分 " + score);
                return score;
            }
        }
    }

    /** 得分 = 基础分 - 步数扣分 - 时间扣分 + 提示加分，最低 0 */
    static int scoreFor(int steps, long seconds, int hintsLeft) {
        int score = 2000 - steps * 10 - (int) seconds * 5 + hintsLeft * 50;
        return Math.max(0, score);
    }

    // ---------------- 迷宫生成 ----------------

    /**
     * "递归回溯"算法生成迷宫：一开始全是墙，从左上角出发随机挖路，
     * 每次往前挖两格，并把中间那堵墙打通。这样保证任意两点之间有且仅有一条路。
     */
    static char[][] generateMaze(int size) {
        int rows = size * 2 + 1; // 奇数，保证四周有一圈外墙
        int cols = size * 2 + 1;
        char[][] maze = new char[rows][cols];
        for (char[] row : maze) {
            Arrays.fill(row, WALL);
        }
        carve(maze, 1, 1);
        return maze;
    }

    /** 从 (r,c) 出发随机挖路 */
    static void carve(char[][] maze, int r, int c) {
        maze[r][c] = PATH;
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}}; // 上下左右，每次跳两格
        Collections.shuffle(Arrays.asList(dirs), RANDOM);  // 随机顺序，迷宫才不一样

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            if (nr <= 0 || nr >= maze.length - 1 || nc <= 0 || nc >= maze[0].length - 1) {
                continue; // 超出迷宫范围
            }
            if (maze[nr][nc] == WALL) {
                maze[r + d[0] / 2][c + d[1] / 2] = PATH; // 打通中间那堵墙
                carve(maze, nr, nc);
            }
        }
    }

    // ---------------- 最短路径（BFS） ----------------

    /** 用广度优先搜索求从 (sr,sc) 到 (er,ec) 的最短路径，返回途经的每个格子 */
    static List<int[]> findPath(char[][] maze, int sr, int sc, int er, int ec) {
        int rows = maze.length, cols = maze[0].length;
        boolean[][] visited = new boolean[rows][cols];
        int[][] prev = new int[rows][cols]; // 记录每个格子是从哪个格子走来的（编码成 r*cols+c）
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
            return Collections.emptyList(); // 没有路（正常不会发生）
        }

        // 从终点顺着 prev 一路回溯到起点
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

    /** 把路径转成 Set（排除起点和终点），方便渲染时判断 */
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

    // ---------------- 渲染 ----------------

    /** 打印迷宫（玩家、出口、提示路径、迷雾都画在迷宫字符上） */
    static void printMaze(char[][] maze, int pr, int pc, int er, int ec, Set<String> hintPath, boolean fog) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < maze.length; r++) {
            for (int c = 0; c < maze[0].length; c++) {
                if (fog && !isVisible(pr, pc, r, c)) {
                    sb.append(FOG); // 迷雾里什么都看不见
                } else if (r == pr && c == pc) {
                    sb.append(PLAYER);
                } else if (r == er && c == ec) {
                    sb.append(EXIT);
                } else if (hintPath.contains(r + "," + c)) {
                    sb.append(HINT);
                } else {
                    sb.append(maze[r][c]);
                }
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }

    /** 判断 (r,c) 是否在玩家可视范围内（切比雪夫距离 <= FOG_RADIUS） */
    static boolean isVisible(int pr, int pc, int r, int c) {
        return Math.max(Math.abs(r - pr), Math.abs(c - pc)) <= FOG_RADIUS;
    }

    // ---------------- 工具 ----------------

    /** 读一个整数，输入为空或非法时返回默认值 */
    static int readInt(Scanner sc, String prompt, int def) {
        System.out.print(prompt);
        String line = sc.nextLine().trim();
        if (line.isEmpty()) {
            return def;
        }
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
