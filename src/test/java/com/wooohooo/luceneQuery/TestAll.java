package com.wooohooo.luceneQuery;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAll {
    public static void main()
    {
        CalculateTest calculateTest = new CalculateTest();
        calculateTest.testAdd();
        calculateTest.testSubTract();
    }
}
