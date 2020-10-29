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
import org.bson.Document;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;



import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import javax.sound.midi.MidiSystem;

import java.io.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static Lucene lucene = new Lucene(); 
    public static void main( String[] args )
    {
        
        //初始化索引数据库
        lucene.createIndex("./index");
        System.out.println("索引创建成功");
        //利用ssh连接远程服务器
        go();
        System.out.println("Set the ssh successful");
        //获取爬虫数据库
        MongoDatabase mongoDatabase = connectToMongo();
        System.out.println("mongoClient connect");
        //将爬虫数据库内数据建立索引
        addIndexDoc("./index", mongoDatabase);
        System.out.println("索引文档添加成功");
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
    /*
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
    */
    /**
     * 添加索引文档
     *
     * @param indexDir    索引存放位置
     * @param mongoDatabase 数据库
     */
    
    public static void addIndexDoc(String indexDir, MongoDatabase mongoDatabase) {
        //遍历mongo数据库
        MongoCollection<Document> collection = mongoDatabase.getCollection("news");
        BasicDBObject doc = new BasicDBObject();
        long start = System.currentTimeMillis();
        //数据量
        int count = (int)collection.countDocuments();
        System.out.println("count: "+count);
        //每次读取20000条
        int pageSize = 20000;
        //页数
        int pageCount = count/pageSize + 1;
        int page = 0;
        MongoCursor cursor = null;
        //while(page < pageSize)
        //{
            cursor = collection.find().iterator();
            int num = 0;
            while(cursor.hasNext())
            {
                System.out.println(num++);
                Document document = (Document)cursor.next();
                Set<Map.Entry<String, Object>> entrySet = document.entrySet();
                lucene.addIndexDoc(indexDir, entrySet);
            }
            if(cursor != null)
                cursor.close();
            //page++;
            //System.out.println(page);
        //}
    }
    

    /**
     * json内容转document文档
     *
     * @param jsonObj json内容
     * @return
     */
    /*
    public static Document jsonToDoc(JSONObject jsonObj) {
        Document document = new Document();
        Set<Map.Entry<String, Object>> entrySet = jsonObj.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
        }
        return document;
    }
    */

    /**
     * 查询文档
     * @param indexDir 索引存放位置
     * @param queryContent 查询单词内容
     * @parm page 查询页数
     * @parm number 查询条数
     * @return
     */
    /*
    public static String query(String indexDir, String queryContent, int page, int number) {

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
            Analyzer analyzer = new SmartChineseAnalyzer();
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
        return result.toString();
    }
    */
}
