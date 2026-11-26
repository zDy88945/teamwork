package org.example;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * 由合作者补充的边界测试：验证计算器在较大整数与负数场景下的结果。
 * 该文件由合作者在自己的本地副本中新增，提交并推送后由项目所有者拉取。
 */
public class CalculatorEdgeCasesTest {

    /** 被测对象 */
    private final Calculator calculator = new Calculator();

    /**
     * 验证较大整数相加不会溢出（结果仍在 int 范围内）。
     */
    @Test
    public void testAddLargeNumbers() {
        assertEquals(2_000_000_000, calculator.add(1_000_000_000, 1_000_000_000));
    }

    /**
     * 验证负数相减：-10 - (-5) = -5。
     */
    @Test
    public void testSubtractNegative() {
        assertEquals(-5, calculator.subtract(-10, -5));
    }

    /**
     * 验证负数与正数相乘：-3 * 4 = -12。
     */
    @Test
    public void testMultiplyNegative() {
        assertEquals(-12, calculator.multiply(-3, 4));
    }
}
