package org.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 由合作者补充的用例：覆盖计算器在 0、负数与较大整数边界上的行为。
 */
public class CalculatorZeroAndSignTest {

    /** 被测对象 */
    private final Calculator calculator = new Calculator();

    /**
     * 0 参与加法、乘法时结果应为另一个操作数或 0。
     */
    @Test
    public void testZeroOperand() {
        assertEquals(18, calculator.add(18, 0));
        assertEquals(18, calculator.add(0, 18));
        assertEquals(0, calculator.multiply(18, 0));
    }

    /**
     * 负数除以正数：-18 / 6 = -3。
     */
    @Test
    public void testDivideNegative() {
        assertEquals(-3, calculator.divide(-18, 6));
        assertEquals(-3, calculator.divide(18, -6));
    }

    /**
     * 被减数小于减数时结果为负数：6 - 18 = -12。
     */
    @Test
    public void testSubtractToNegative() {
        assertEquals(-12, calculator.subtract(6, 18));
    }
}
