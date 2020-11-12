package com.wooohooo.luceneQuery;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import com.mongodb.client.MongoClients;
import com.mongodb.MongoClient;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.*;
import com.mongodb.*;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.TextSearchOptions;
import com.mongodb.client.model.Projections;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.BinaryPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.lang.Thread;
import java.text.SimpleDateFormat;
import javax.sound.midi.MidiSystem;

import java.io.*;


@SpringBootApplication(scanBasePackages = {"com.wooohooo.luceneQuery"}, exclude = MongoAutoConfiguration.class)
public class App 
{
    private static MongoDB mongoDB = new MongoDB(); 
    private static int incrementalNewsNum = 0;
    private static long timeInterval = 3600 * 1000;

    static class StaticIndexThread extends Thread{
        public void run()
        {
            //初始化索引数据库
        createIndex("./index");
        System.out.println("索引创建成功");
        //获取爬虫数据库
        MongoDatabase mongoDatabase = connectToMongo();
        System.out.println("mongoClient connect");
        //统计数据库内数据总量
        MongoCollection collection = mongoDatabase.getCollection("news");
        int count = (int)collection.countDocuments();
        //collection = null;
        System.out.println("count: "+ count);
        //测试 只爬100000条
        //将爬虫数据库内数据建立索引 
        for(int i=0;i<count;i+=20000)
        {
            //每次建立一批索引，每批20000个
            //addIndexDoc("./index", mongoDatabase, i);
        }
        optimazeIndex("./index");
        System.out.println("索引文档添加成功");
        mongoDatabase=null;
        }
    }
    
    static class IncrementalIndexThread extends Thread
    {
        //增量索引
        public void run()
        {
            while(true)
            {
                try
                {
                    MongoDatabase mongoDatabase = connectToMongo();
                    MongoCollection collection = mongoDatabase.getCollection("dynamicNews");
                    int incrementalCount = (int)collection.countDocuments();
                    System.out.println("incrementalCount: " + incrementalCount);
                    int existNum = incrementalNewsNum / 100000;
                    for(int i=incrementalNewsNum; i<incrementalCount; i+=20000)
                    {
                        addIndexDoc("./index", mongoDatabase, i);
                    }
                    incrementalNewsNum = incrementalCount;
                    int newNum = incrementalNewsNum / 100000;
                    if(newNum > existNum)
                    {
                        optimazeIndex("./index");
                    }
                    this.sleep(timeInterval);
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                }
            }
            
        }
    }
    public static void main( String[] args )
    {
        StaticIndexThread staticThread = new StaticIndexThread();
        staticThread.start();
        String format = "yyyyMMdd";
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date lastWeek = new Date();
        lastWeek.setTime(date.getTime() - 1000 * 60 * 60 * 24 * 7);
        System.out.println(sdf.format(lastWeek));
        ConfigurableApplicationContext applicationContext = SpringApplication.run(App.class, args);
        
        //IncrementalIndexThread incrementalThread = new IncrementalIndexThread();
        //incrementalThread.start(); 
    }

    public static MongoDatabase connectToMongo()
    {
        MongoClient mongoClient = new MongoClient("49.233.52.61", 30001);
        return mongoClient.getDatabase("NewsCopy");
    }

/**
     * 创建索引
     *
     * @param indexDir 索引存放位置
     */


    public static void createIndex(String indexDir) {
        IndexWriter writer = null;
        try {
            //获取目录
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            //设置分词器
            Analyzer analyzer = new SmartChineseAnalyzer();
            //准备config
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            //创建lucene实例
            writer = new IndexWriter(directory, indexWriterConfig);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(writer);
        }
    }

    /**
     * 添加索引文档
     *
     * @param indexDir    索引存放位置
     * @param mongoDatabase 数据库
     */
    
    public static void addIndexDoc(String indexDir, MongoDatabase mongoDatabase, int count) {
        IndexWriter writer = null;
        try {
            //获取目录
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            //设置分词器
            Analyzer analyzer = new SmartChineseAnalyzer();
            //准备config
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            //创建lucene实例
            writer = new IndexWriter(directory, indexWriterConfig);
            List<Set<Map.Entry<String, Object>>> entrySetList = mongoDB.getDocument(mongoDatabase, count);
            long luceneStartTime = System.currentTimeMillis();
            for(int i=0;i<entrySetList.size();i++)
            {
                Document document = new Document();
                for (Map.Entry<String, Object> entry : entrySetList.get(i)) {
                    if(entry.getKey().equals("publish_time"))
                    {
                        document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
                        if(entry.getValue() == null) continue;
                        String newsTime = entry.getValue().toString();
                        String year = newsTime.substring(0,4);
                        String month = newsTime.substring(5,7);
                        String day = newsTime.substring(8,10);
                        document.add(new IntPoint("time", Integer.parseInt(year) * 10000 + Integer.parseInt(month) * 100 + Integer.parseInt(day)));                    
                    }
                    else
                        document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
                }
                writer.addDocument(document);
            }
            long luceneEndTime = System.currentTimeMillis();
            System.out.println("lucene func: " + (luceneEndTime-luceneStartTime) + "ms");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(writer);
        }
    }

    public static void optimazeIndex(String indexDir)
    {
        IndexWriter writer = null;
        try {
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            Analyzer analyzer = new SmartChineseAnalyzer();
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
            writer = new IndexWriter(directory, indexWriterConfig);
            writer.forceMerge(1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            IOUtils.close(writer);
        }
    }
}
