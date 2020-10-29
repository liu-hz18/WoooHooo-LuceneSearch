package com.wooohooo.luceneQuery.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

/**
 * Hello world!
 *
 */
@RestController
public class WelcomeController
{
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<?> welcome()
    {
        return new ResponseEntity<>("Welcome to Maven!", HttpStatus.OK);
    }

    @GetMapping("/queryNews")
    public JSONArray queryNews(@RequestParam(name = "name")String name,@RequestParam(name = "page")String page,@RequestParam(name="number")String number)
    {
        System.out.println("name= " + name);
        System.out.println("page=" +page);
        System.out.println("number=" + number);
        return query("./index", name, Integer.parseInt(page), Integer.parseInt(number));
    }

    public JSONArray query(String indexDir, String queryContent, int page, int number)
    {
        IndexReader reader = null;
        JSONArray jsonArray = null;
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
            jsonArray = new JSONArray();
            int index = 0;
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                index++;
                //if(index <= number * (page-1))
                //    continue;
                //拿到文档实例
                Document document = searcher.doc(scoreDoc.doc);
                //获取所有文档字段
                List<IndexableField> fieldList = document.getFields();
                //处理文档字段建立Json对象
                JSONObject jsonObject = new JSONObject();
                for (IndexableField field:fieldList){
                    jsonObject.put(field.name(), field.stringValue());
                }
                jsonArray.add(jsonObject);
            }            
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            IOUtils.close(reader);
        }
        return jsonArray;
    }
}
