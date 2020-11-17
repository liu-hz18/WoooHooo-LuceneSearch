package com.wooohooo.luceneQuery;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.*;
import com.mongodb.*;
import org.bson.Document;

import java.util.*;
import java.io.*;

public class MongoDB
{
    /**
     * 添加索引文档
     *
     * @param indexDir    索引存放位置
     * @param mongoDatabase 数据库
     */
    
    public List<Set<Map.Entry<String, Object>>> getDocument(MongoDatabase mongoDatabase, int count) {
        //遍历mongo数据库
        MongoCollection<Document> collection = mongoDatabase.getCollection("news");
        //每次读取20000条
        int limitSize = 20000;
        MongoCursor cursor = null;
        cursor = collection.find().skip(count).limit(limitSize).iterator();
        int num = count;
        List<Set<Map.Entry<String, Object>>> entrySetList = new ArrayList<>();
        while(cursor.hasNext())
        {
            if(num % 1000 == 0)
            {
                System.err.println(num);
            }
                
            num++;
            Document document = (Document)cursor.next();
            entrySetList.add(document.entrySet());
        }
        cursor.close();
        
        return entrySetList;
    }
}