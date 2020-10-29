package com.wooohooo.luceneQuery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;

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

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import javax.sound.midi.MidiSystem;

import java.io.*;

public class MongoDB
{
    /**
     * 添加索引文档
     *
     * @param indexDir    索引存放位置
     * @param mongoDatabase 数据库
     */
    
    public List<Set<Map.Entry<String, Object>>> getDocument(MongoDatabase mongoDatabase) {
        //遍历mongo数据库
        MongoCollection<Document> collection = mongoDatabase.getCollection("news");
        BasicDBObject doc = new BasicDBObject();
        long start = System.currentTimeMillis();
        //数据量
        int count = (int)collection.countDocuments();
        System.out.println("count: "+count);
        //每次读取20000条
        int pageSize = 100000;
        //页数
        int pageCount = count/pageSize + 1;
        int page = 0;
        MongoCursor cursor = null;
        //while(page < pageSize)
        //{
            cursor = collection.find().limit(pageSize).iterator();
            int num = 0;
            List<Set<Map.Entry<String, Object>>> entrySetList = new ArrayList<>();
            while(cursor.hasNext())
            {
                if(num % 1000 == 0)
                    System.out.println(num);
                num++;
                Document document = (Document)cursor.next();
                entrySetList.add(document.entrySet());
            }
            if(cursor != null)
                cursor.close();
            //page++;
            //System.out.println(page);
        //}
        return entrySetList;
    }
}