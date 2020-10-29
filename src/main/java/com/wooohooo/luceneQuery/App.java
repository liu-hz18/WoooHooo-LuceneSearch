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
import javax.sound.midi.MidiSystem;

import java.io.*;

/**
 * Hello world!
 *
 */
@SpringBootApplication(scanBasePackages = {"com.wooohooo.luceneQuery"}, exclude = MongoAutoConfiguration.class)
public class App 
{
    public static MongoDB mongoDB = new MongoDB(); 

    static class IndexThread extends Thread{
        public void run()
        {
            //初始化索引数据库
        createIndex("./index");
        System.out.println("索引创建成功");
        //利用ssh连接远程服务器
        go();
        System.out.println("Set the ssh successful");
        //获取爬虫数据库
        MongoDatabase mongoDatabase = connectToMongo();
        System.out.println("mongoClient connect");
        //将爬虫数据库内数据建立索引
        addIndexDoc("./index", mongoDatabase);
        //System.out.println("索引文档添加成功");
        }
    }
    public static void main( String[] args )
    {
        IndexThread thread = new IndexThread();
        thread.start();
        
        ConfigurableApplicationContext applicationContext = SpringApplication.run(App.class, args);
    }

    public static void go()
    {
        try{
            JSch jsch = new JSch();
            Session session = jsch.getSession("ubuntu", "49.233.52.61", 22);
            session.setPassword("48*~VbNY93Aq");
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            System.out.println(session.getServerVersion());//这里打印SSH服务器版本信息
 
            //ssh -L 192.168.0.102:5555:192.168.0.101:3306 yunshouhu@192.168.0.102  正向代理
           int assinged_port = session.setPortForwardingL("localhost", 27018, "127.0.0.1", 27017);//端口映射 转发
 
           System.out.println("localhost:" + assinged_port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MongoDatabase connectToMongo()
    {
        MongoClient mongoClient = new MongoClient("localhost", 27018);
        return mongoClient.getDatabase("StaticNews");
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
    
    public static void addIndexDoc(String indexDir, MongoDatabase mongoDatabase) {
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
            List<Set<Map.Entry<String, Object>>> entrySetList = mongoDB.getDocument(mongoDatabase);
            for(int i=0;i<entrySetList.size();i++)
            {
                Document document = new Document();
                for (Map.Entry<String, Object> entry : entrySetList.get(i)) {
                    document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
                }
                writer.addDocument(document);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(writer);
        }
    }

    public List<JSONObject>query(String indexDir, String queryContent, int page, int number)
    {
        StringBuilder result = new StringBuilder();
        IndexReader reader = null;
        try {
            //获取目录
            Directory directory = FSDirectory.open(Paths.get((indexDir)));
            //获取reader
            reader = DirectoryReader.open(directory);
            //获取索引实例
            IndexSearcher searcher = new  IndexSearcher(reader);
            //设置分词器
            Analyzer analyzer = new StandardAnalyzer();
            //创建解析器
            BooleanClause.Occur[] flags = {BooleanClause.Occur.SHOULD,
                    BooleanClause.Occur.SHOULD};
            //QueryParser queryParser = new QueryParser(queryParam, analyzer);
            Query query = MultiFieldQueryParser.parse(queryContent,new String[]{"content","title"}, flags, analyzer);
            TopDocs topDocs = searcher.search(query, page * number);
            System.out.println("topDocs内容:" + JSON.toJSONString(topDocs));
            int index = 0;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                index++;
                if(index <= number * (page-1))
                    continue;
                //拿到文档实例
                Document document = searcher.doc(scoreDoc.doc);
                //获取所有文档字段
                List<IndexableField> fieldList = document.getFields();
                //处理文档字段
                for (IndexableField field:fieldList){
                    result.append(field.name());
                    result.append(":");
                    result.append(field.stringValue());
                    result.append(",\r\n");
                }
            }
            System.out.println("查询结果："+result);
            
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            IOUtils.close(reader);
        }
        return null;
    }
}
