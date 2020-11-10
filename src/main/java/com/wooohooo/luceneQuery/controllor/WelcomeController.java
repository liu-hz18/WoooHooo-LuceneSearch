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
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;

import javax.sound.midi.MidiSystem;
import javax.swing.event.TreeWillExpandListener;

import java.io.*;

/**
 * Hello world!
 *
 */
@RestController
public class WelcomeController
{
    //上次的结果个数
    private int prevTotalNum = 0;
    private int test = -1;
    IndexSearcher searcher = null;
    private int dynamicNewsNum = 0;
    //需要返回的字段
    String []tag = {"_id","content","publish_time","top_img","url","source","imageurl","title"};
    @RequestMapping(value = "/", method = RequestMethod.GET)
    public ResponseEntity<?> welcome()
    {
        return new ResponseEntity<>("Welcome to Maven!", HttpStatus.OK);
    }

    @GetMapping("/queryNews")
    public JSONObject queryNews(@RequestParam(name = "name")String name,@RequestParam(name = "page")String page,
                        @RequestParam(name="number")String number, @RequestParam(name="relation")String relation)
    {
        long begintime = System.currentTimeMillis();
        System.out.println("name= " + name);
        System.out.println("page=" +page);
        System.out.println("number=" + number);
        System.out.println("relation="+ relation);
        if(test == -1)
        {
            try{
                
                //获取目录
            Directory directory = FSDirectory.open(Paths.get(("./index")));
            //获取reader
            IndexReader reader = DirectoryReader.open(directory);
            //获取索引实例
            searcher = new  IndexSearcher(reader);
            test = 1;
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
        boolean sort_by_time = false;
        if(!relation.equals("1")) sort_by_time = true;
        int _page = Integer.parseInt(page);
        int _number = Integer.parseInt(number);
        JSONObject result = new JSONObject();
        JSONArray newslist = null;
        newslist = query("./index", name, _page, _number, sort_by_time);
        result.put("data", newslist);
        result.put("total", prevTotalNum);
        long endtinme=System.currentTimeMillis();
        System.out.println("time: "+(endtinme - begintime) + "ms");
        return result;
    }

    //获取动态新闻
    @PostMapping("/")
    public String getNews(@RequestParam(name = "news")JSONObject newsObject)
    {
        JSONArray newsList = newsObject.getJSONArray("news");
        addIndex("./index", newsList);
        return "receive";
    }
    //建立动态索引
    public void addIndex(String indexDir, JSONArray newsList)
    {
        IndexWriter writer = null;
        try{
            int existNum = dynamicNewsNum / 100000;
            //获取目录
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            //设置分词器
            Analyzer analyzer = new SmartChineseAnalyzer();
            //索引设置
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            //获取索引
            writer = new IndexWriter(directory, config);

            //解析jsonArray并建立索引
            for(int i=0;i<newsList.size();i++)
            {
                Document document = new Document();
                JSONObject newsObject = newsList.getJSONObject(i);
                document = jsonToDoc(newsObject);
            }
            dynamicNewsNum += newsList.size();
            //每100000条索引创建后合并优化索引
            if(dynamicNewsNum / 100000 > existNum)
                optimazeIndex("./index");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally{
            IOUtils.close(writer);
        }
    }
    //从json文件转到document
    public Document jsonToDoc(JSONObject newsObject)
    {
        Document document = new Document();
        Set<Map.Entry<String, Object>> entrySet = newsObject.entrySet();
        for (Map.Entry<String, Object> entry : entrySet) {
            if(entry.getKey().equals("publish_time"))
            {
                document.add(new StringField("publish_time", entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
                document.add(new SortedDocValuesField("publish_time", new BytesRef((entry.getValue()==null?"":entry.getValue()).toString().getBytes()))); 
            }
            else
            {
                document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
            }
        }
        return document;
    }
    //优化索引
    public void optimazeIndex(String indexDir)
    {
        IndexWriter writer = null;
        try
        {
            //获取目录
            Directory directory = FSDirectory.open(Paths.get(indexDir));
            //设置分词器
            Analyzer analyzer = new SmartChineseAnalyzer();
            //索引设置
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            //获取索引
            writer = new IndexWriter(directory, config);
            //合并索引
            writer.forceMerge(1);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            IOUtils.close(writer);
        }
        
    }

    public JSONArray query(String indexDir, String queryContent, int page, int number, boolean sort_by_time)
    {
        IndexReader reader = null;
        JSONArray result = new JSONArray();
        try {
            long queryStart = System.currentTimeMillis();
            
            //设置分词器
            Analyzer analyzer = new SmartChineseAnalyzer();
            //创建解析器
            BooleanClause.Occur[] flags = {BooleanClause.Occur.SHOULD,
                    BooleanClause.Occur.SHOULD};
            //双词条搜索
            Query query = MultiFieldQueryParser.parse(queryContent,new String[]{"content","title"}, flags, analyzer);
            TopDocs topDocs = null;
            if(sort_by_time)
            {
                topDocs = searcher.search(query, (page+1)*number, 
                new Sort(new SortField("publish_time", SortField.Type.STRING, true)));
            }
            else
            {
                topDocs = searcher.search(query, (page + 1) * number);
            }
            System.out.println("queryTime: " + (System.currentTimeMillis()-queryStart) + "ms");
            long jsonStart = System.currentTimeMillis();
            //for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            for(int i = page * number; i < page * number +number ;i++){
                //拿到文档实例
                Document document = searcher.doc(topDocs.scoreDocs[i].doc);
                //获取所有文档字段
                // List<IndexableField> fieldList = document.getFields();
                //处理文档字段建立Json对象
                JSONObject jsonObject = new JSONObject();
                for(int j=0;j<8;j++)
                {
                    IndexableField field = document.getField(tag[j]);
                    if(j == 1)
                    {
                        String content = field.stringValue();
                        if(field.stringValue().length() > 300)
                        {
                            content = content.substring(0,300);
                        }
                        jsonObject.put(field.name(), content);
                    }
                    else
                    {
                        jsonObject.put(field.name(), field.stringValue());
                    }
                }
                result.add(jsonObject);
            }
            System.out.println("jsonTime: " + (System.currentTimeMillis() - jsonStart) + "ms");
            prevTotalNum = topDocs.totalHits;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
