package org.example;

/**
 * 简单的计算器类，用于演示 Maven 工程的编译与单元测试。
 * 只保留加、减、乘、除四个最基本的运算，除数为 0 时抛出异常。
 */
public class Calculator {

    /**
     * 两数相加。
     *
     * @param a 被加数
     * @param b 加数
     * @return 两数之和
     */
    public int add(int a, int b) {
        return a + b;
    }

    /**
     * 两数相减。
     *
     * @param a 被减数
     * @param b 减数
     * @return 两数之差
     */
    public int subtract(int a, int b) {
        return a - b;
    }

    /**
     * 两数相乘。
     *
     * @param a 被乘数
     * @param b 乘数
     * @return 两数之积
     */
    public int multiply(int a, int b) {
        return a * b;
    }

    /**
     * 两数相除，结果为整数（向下取整）。
     *
     * @param a 被除数
     * @param b 除数
     * @return 两数之商
     * @throws ArithmeticException 当除数为 0 时抛出
     */
    public int divide(int a, int b) {
        if (b == 0) {
            throw new ArithmeticException("除数不能为 0");
        }
        return a / b;
    }
}
