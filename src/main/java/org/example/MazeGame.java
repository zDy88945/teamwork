package org.example;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

/**
 * 简单的控制台迷宫游戏
 *
 * 玩法：
 *   输入 w/a/s/d 后回车，控制玩家 @ 移动
 *   走到右下角的出口 E 就算赢，输入 q 退出
 *
 * 地图符号：
 *   # 墙    空格 路    . 走过的路    @ 玩家    E 出口
 */
public class MazeGame {

    static final char WALL = '#';
    static final char PATH = ' ';
    static final char TRAIL = '.';
    static final char PLAYER = '@';
    static final char EXIT = 'E';

    static final Random RANDOM = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("=== 迷宫游戏 ===");
        System.out.print("迷宫大小（3~30，直接回车默认 10）：");
        int size = 10;
        String line = sc.nextLine().trim();
        if (!line.isEmpty()) {
            try {
                size = Integer.parseInt(line);
            } catch (NumberFormatException ignored) {
                // 不是数字就用默认值
            }
        }
        size = Math.max(3, Math.min(30, size));

        // 生成迷宫，玩家从左上角 (1,1) 出发，出口在右下角
        char[][] maze = generateMaze(size);
        int playerRow = 1;
        int playerCol = 1;
        int exitRow = maze.length - 2;
        int exitCol = maze[0].length - 2;
        int steps = 0;

        System.out.println("用 w/a/s/d 移动（每次输入后按回车），q 退出");

        while (true) {
            printMaze(maze, playerRow, playerCol, exitRow, exitCol);
            System.out.println("步数：" + steps + "，请输入方向：");

            String cmd = sc.nextLine().trim().toLowerCase();
            if (cmd.isEmpty()) {
                continue;
            }
            char key = cmd.charAt(0);

            if (key == 'q') {
                System.out.println("再见！");
                break;
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
                System.out.println("无效按键，请用 w/a/s/d");
                continue;
            }

            int nr = playerRow + dr;
            int nc = playerCol + dc;
            if (maze[nr][nc] == WALL) {
                System.out.println("撞墙啦！");
                continue;
            }

            // 旧位置标记为走过的路，然后移动
            maze[playerRow][playerCol] = TRAIL;
            playerRow = nr;
            playerCol = nc;
            steps++;

            if (playerRow == exitRow && playerCol == exitCol) {
                printMaze(maze, playerRow, playerCol, exitRow, exitCol);
                System.out.println("🎉 恭喜你走出迷宫！一共用了 " + steps + " 步。");
                break;
            }
        }
        sc.close();
    }

    /**
     * 用"递归回溯"算法生成迷宫：
     * 一开始全是墙，从左上角出发随机挖路，
     * 每次往前挖两格，并把中间的那堵墙打通。
     */
    static char[][] generateMaze(int size) {
        int rows = size * 2 + 1;   // 奇数，保证四周有一圈外墙
        int cols = size * 2 + 1;
        char[][] maze = new char[rows][cols];
        for (char[] row : maze) {
            Arrays.fill(row, WALL);
        }
        carve(maze, 1, 1);
        return maze;
    }

    /** 从 (r,c) 开始随机挖路 */
    static void carve(char[][] maze, int r, int c) {
        maze[r][c] = PATH;
        // 四个方向：上下左右，每次跳两格
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        Collections.shuffle(Arrays.asList(dirs), RANDOM); // 随机顺序，迷宫才会不一样

        for (int[] d : dirs) {
            int nr = r + d[0];
            int nc = c + d[1];
            // 超出迷宫范围或已经挖过就跳过
            if (nr <= 0 || nr >= maze.length - 1 || nc <= 0 || nc >= maze[0].length - 1) {
                continue;
            }
            if (maze[nr][nc] == WALL) {
                maze[r + d[0] / 2][c + d[1] / 2] = PATH; // 打通中间那堵墙
                carve(maze, nr, nc);
            }
        }
    }

    /** 打印迷宫（玩家和出口画在迷宫的字符上） */
    static void printMaze(char[][] maze, int playerRow, int playerCol, int exitRow, int exitCol) {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < maze.length; r++) {
            for (int c = 0; c < maze[0].length; c++) {
                if (r == playerRow && c == playerCol) {
                    sb.append(PLAYER);
                } else if (r == exitRow && c == exitCol) {
                    sb.append(EXIT);
                } else {
                    sb.append(maze[r][c]);
                }
            }
            sb.append('\n');
        }
        System.out.print(sb);
    }
}
