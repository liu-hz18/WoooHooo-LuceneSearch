package com.wooohooo.luceneQuery;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class CalculateTest extends TestCase
{
    //对方法max进行测试
    public void testAdd(){
        int x = 1;  //测试数据
        int y = 2;
        int z = -1;
        Calculate cal = new Calculate();
        int result1 = cal.add(x, y);
        int result2 = cal.add(y,x);
        int result3 = cal.add(z, x);
        assertTrue(result1 == 3);
        assertTrue(result2 == 3);
        assertTrue(result3 == 0);
    }
 
    //对方法maxSubArr测试
    public void testSubTract(){
        Calculate cal = new Calculate();
 
 
        assertTrue(cal.subtract(1, 2) == -1);
        assertTrue(cal.subtract(2, 1) == 1);
        assertTrue(cal.subtract(100, 50) == 50);
        assertTrue(cal.subtract(50, 100) == -50);
    }
}