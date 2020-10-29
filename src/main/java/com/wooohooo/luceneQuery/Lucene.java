package com.wooohooo.luceneQuery;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;

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

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import javax.sound.midi.MidiSystem;

import java.io.*;

public class Lucene
{
    /**
     * 创建索引
     *
     * @param indexDir 索引存放位置
     */


    public void createIndex(String indexDir) {
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
    
    public void addIndexDoc(String indexDir, Set<Map.Entry<String, Object>> entrySet) {
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
            Document document = new Document();
            for (Map.Entry<String, Object> entry : entrySet) {
                document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
            }
            writer.addDocument(document);

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