package com.wooohooo.luceneQuery;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.lucene.search.Query;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;
import com.wooohooo.luceneQuery.controllor.WelcomeController;
public class WelcomControllerTest extends TestCase{
    public void testGetNews()
    {
        JSONObject newsObject = new JSONObject();
        JSONArray newsList = new JSONArray();
        JSONObject news0 = new JSONObject();
        JSONObject news1 = new JSONObject();
        JSONObject news2 = new JSONObject();
        news0.put("publish_time", "2020-11-16 15:30");
        news0.put("content", "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890");
        news1.put("content", "1");
        news2.put("content", "2");
        news2.put("publish_time", null);
        newsList.add(news0);
        newsList.add(news1);
        newsList.add(news2);
        newsObject.put("news", newsList);

        WelcomeController welcomeController = new WelcomeController();
        assertTrue(welcomeController.getNews(JSON.toJSONString(newsObject)).equals("receive"));
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
        assertTrue(welcomeController.queryNews(testCase_0, "0", "1", 0) != null);
        assertTrue(welcomeController.queryNews(testCase_1, "0", "1", 0) != null);
        assertTrue(welcomeController.queryNews(testCase_2, "0", "1", 0) != null);
    }

    public void testQuery()
    {
        String testCase0 = "iphone12";
        String testCase1 = "美国";
        String testCase2 = "华为";
        WelcomeController welcomeController = new WelcomeController();
        assertTrue(welcomeController.query(null, testCase0, 0, 10, 0) != null);
        assertTrue(welcomeController.query(null, testCase1, 0, 10, 0) != null);
        assertTrue(welcomeController.query(null, testCase2, 0, 10, 1) != null);
    }

    public void testWelcome()
    {
        WelcomeController welcomeController = new WelcomeController();
        assertTrue(welcomeController.welcome() != null);
    }

    public void testOptimaze()
    {
        String indexDir = "./index";
        WelcomeController welcomeController = new WelcomeController();
        assertTrue(welcomeController.verifyOptimazeIndex(indexDir));
        assertTrue(welcomeController.verifyOptimazeIndex(null));
    }
}
