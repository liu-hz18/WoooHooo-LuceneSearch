package com.wooohooo.luceneQuery;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.mongodb.client.MongoDatabase;
/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        String []args = {};
        String indexDir = "./index";
        App.main(args);
        assertTrue(App.verifyStaticThread());
        assertTrue(App.verifyIncrementalThread(indexDir));
        assertTrue(App.verifyIncrementalThread(null));
    }

    public void testConnectToMongo()
    {
        MongoDatabase dataBase = null;
        int count = 100;
        String indexDir = "./index";
        assertTrue((dataBase = App.connectToMongo()) != null);
        assertTrue(App.verifyCreateIndex(indexDir));
        assertTrue(App.verifyCreateIndex(null));
        assertTrue(App.verifyAddIndexDoc(indexDir, dataBase, count));
        assertTrue(App.verifyAddIndexDoc(null, dataBase, count));
        assertTrue(App.verifyOptimazeIndex(indexDir));
        assertTrue(App.verifyOptimazeIndex(null));
    }
}

