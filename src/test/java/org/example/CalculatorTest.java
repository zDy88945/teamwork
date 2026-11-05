package org.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

/**
 * Calculator 的单元测试类。
 * 源码目录位于 src/test/java，Maven 的 test 阶段会自动编译并运行这些用例。
 */
public class CalculatorTest {

    /** 被测对象：每个测试方法执行前都会重新创建 */
    private final Calculator calculator = new Calculator();

    /**
     * 测试加法：正数、负数与 0 的情况。
     */
    @Test
    public void testAdd() {
        assertEquals(24, calculator.add(18, 6));
        assertEquals(-12, calculator.add(-18, 6));
        assertEquals(0, calculator.add(0, 0));
    }

    /**
     * 测试减法。
     */
    @Test
    public void testSubtract() {
        assertEquals(12, calculator.subtract(18, 6));
        assertEquals(-24, calculator.subtract(-18, 6));
    }

    /**
     * 测试乘法。
     */
    @Test
    public void testMultiply() {
        assertEquals(108, calculator.multiply(18, 6));
        assertEquals(0, calculator.multiply(18, 0));
    }

    /**
     * 测试除法，并验证除数为 0 时抛出 ArithmeticException。
     */
    @Test
    public void testDivide() {
        assertEquals(3, calculator.divide(18, 6));
        assertEquals(0, calculator.divide(1, 2));
        assertThrows(ArithmeticException.class, () -> calculator.divide(1, 0));
    }
}
