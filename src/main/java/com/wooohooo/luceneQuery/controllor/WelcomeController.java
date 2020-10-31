package com.wooohooo.luceneQuery.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.IOUtils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jdk.nashorn.internal.runtime.JSONListAdapter;

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
    //上一次查询的词条
    private String prevQuery = "";
    //上次的全部结果
    private JSONArray prevResult = new JSONArray();
    //上次的结果个数
    private int prevTotalNum = 0;

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<?> welcome()
    {
        return new ResponseEntity<>("Welcome to Maven!", HttpStatus.OK);
    }

    @GetMapping("/queryNews")
    public JSONObject queryNews(@RequestParam(name = "name")String name,@RequestParam(name = "page")String page,@RequestParam(name="number")String number)
    {
        System.out.println("name= " + name);
        System.out.println("page=" +page);
        System.out.println("number=" + number);
        int _page = Integer.parseInt(page);
        int _number = Integer.parseInt(number);
        JSONObject result = new JSONObject();
        JSONArray newslist = new JSONArray();
        if(!name.equals(prevQuery))
        {
            query("./index", name, _page, _number);
            prevQuery = name;
        }
        for(int i=_page * _number; i <(_page + 1) * _number ; i++)
        {
            newslist.add(prevResult.get(i));
        }
        result.put("data", newslist);
        result.put("total", prevTotalNum);
        return result;
    }

    public void query(String indexDir, String queryContent, int page, int number)
    {
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
            TopDocs topDocs = searcher.search(query, 10000);
            prevResult.clear();
            int index = 0;
            //需要返回的字段
            Set<String>tag = new HashSet();
            tag.add("_id"); tag.add("content"); tag.add("publish_time"); tag.add("top_img");
            tag.add("url"); tag.add("source"); tag.add("imageurl"); tag.add("title");
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                index++;
                //拿到文档实例
                Document document = searcher.doc(scoreDoc.doc);
                //获取所有文档字段
                List<IndexableField> fieldList = document.getFields();
                //处理文档字段建立Json对象
                JSONObject jsonObject = new JSONObject();
                for (IndexableField field:fieldList){
                    //若字段不被需要
                    if(!tag.contains(field.name()))
                        continue;
                    //content内容大于300字的需要减为300字
                    if(field.name().equals("content"))
                    {
                        if(field.stringValue().length() > 300)
                        {
                            jsonObject.put(field.name(), field.stringValue().substring(0,300));
                        }
                        else jsonObject.put(field.name(), field.stringValue());
                    }
                    else jsonObject.put(field.name(), field.stringValue());
                }
                prevResult.add(jsonObject);
            }
            prevTotalNum = prevResult.size();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            IOUtils.close(reader);
        }
    }
}
