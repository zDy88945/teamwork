package org.example;

import org.apache.log4j.Logger;

/**
 * 工程入口类。演示在 Maven 工程中同时使用 Log4j 记录日志、
 * 调用业务类 Calculator，并把计算结果输出到控制台。
 */
public class App {

    /** Log4j 日志对象：由 log4j.properties 决定输出格式与级别 */
    private static final Logger LOGGER = Logger.getLogger(App.class);

    /**
     * 程序入口。
     *
     * @param args 命令行参数（本示例未使用）
     */
    public static void main(String[] args) {
        Calculator calculator = new Calculator();

        int a = 18;
        int b = 6;

        LOGGER.info("开始执行 maven-demo 示例程序");
        LOGGER.info("当前 JDK 版本：" + System.getProperty("java.version"));

        int sum = calculator.add(a, b);
        int difference = calculator.subtract(a, b);
        int product = calculator.multiply(a, b);
        int quotient = calculator.divide(a, b);

        LOGGER.info(a + " + " + b + " = " + sum);
        LOGGER.info(a + " - " + b + " = " + difference);
        LOGGER.info(a + " * " + b + " = " + product);
        LOGGER.info(a + " / " + b + " = " + quotient);

        // 计算结果同时打印到标准输出，便于在 IDE 控制台中直接查看
        System.out.printf("%d + %d = %d%n", a, b, sum);
        System.out.printf("%d - %d = %d%n", a, b, difference);
        System.out.printf("%d * %d = %d%n", a, b, product);
        System.out.printf("%d / %d = %d%n", a, b, quotient);
        System.out.println("maven-demo 运行结束");
    }
}
