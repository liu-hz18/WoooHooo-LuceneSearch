package com.wooohooo.luceneQuery;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.lucene.search.Query;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;
import com.wooohooo.luceneQuery.controller.WelcomeController;

public class WelcomControllerTest extends TestCase{
    public void testGetNews()
    {
        JSONObject newsObject = new JSONObject();
        newsObject.put("news", new JSONArray());

        WelcomeController welcomeController = new WelcomeController();
        //assertTrue(welcomeController.getNews(JSON.toJSONString(newsObject)).equals("receive"));
    }
    
    public void testRangeQuery()
    {
        int example0 = 0;
        int example1 = 1;
        int example2 = 2;
        int example3 = 3;
        int example4 = 10000;

        WelcomeController welcomeController = new WelcomeController();
        assertTrue(welcomeController.rangeQuery(example0) == null);
        assertTrue(welcomeController.rangeQuery(example1) != null);
        assertTrue(welcomeController.rangeQuery(example2) != null);
        assertTrue(welcomeController.rangeQuery(example3) != null);
        assertTrue(welcomeController.rangeQuery(example4) == null);
    }

    public void testQueryNews()
    {
        String testCase_0 = "iphone12";
        String testCase_1 = "美国";
        String testCase_2 = "华为";

        WelcomeController welcomeController = new WelcomeController();
        //assertTrue(welcomeController.queryNews(testCase_0, "0", "1", 0) != null);
        //assertTrue(welcomeController.queryNews(testCase_1, "0", "1", 0) != null);
        //assertTrue(welcomeController.queryNews(testCase_2, "0", "1", 0) != null);
    }
}
