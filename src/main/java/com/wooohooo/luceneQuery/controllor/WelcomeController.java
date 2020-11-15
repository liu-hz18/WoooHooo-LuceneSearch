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
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.BinaryPoint;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PointRangeQuery;
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
import java.text.SimpleDateFormat;
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
    private long postTime = System.currentTimeMillis();
    //最低的相关度
    private float scoreOffset = 0.5f;
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
                        @RequestParam(name="number")String number, @RequestParam(name="relation")int relation)
    {
        long begintime = System.currentTimeMillis();
        //System.out.println("name= " + name);
        //System.out.println("page=" +page);
        //System.out.println("number=" + number);
        //System.out.println("relation="+ relation);
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
        int _page = Integer.parseInt(page);
        int _number = Integer.parseInt(number);
        JSONObject result = new JSONObject();
        JSONArray newslist = null;
        newslist = query("./index", name, _page, _number, relation);
        result.put("data", newslist);
        result.put("total", prevTotalNum);
        long endtinme=System.currentTimeMillis();
        System.out.println("time: "+(endtinme - begintime) + "ms");
        return result;
    }

    //获取动态新闻
    @RequestMapping(value = "/postNews", method = RequestMethod.POST)
    public String getNews(@RequestBody String object)
    {
         //System.out.println(object);
        Date date = new Date();
        System.out.println("receive news: "+ date.toString());
        JSONObject newsObject =JSON.parseObject(object);


        JSONArray newsList = newsObject.getJSONArray("news");
        //System.out.println(newsList.toString());
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
                writer.addDocument(document);
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
            //System.out.println(entry.getKey());
            if(entry.getKey().equals("publish_time"))
            {
                document.add(new TextField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString(), Field.Store.YES));
                        if(entry.getValue() == null) continue;
                        String newsTime = entry.getValue().toString();
                System.out.println(newsTime);
                        String year = newsTime.substring(0,4);
                        String month = newsTime.substring(5,7);
                        String day = newsTime.substring(8,10);
                        document.add(new IntPoint("time", Integer.parseInt(year) * 10000 + Integer.parseInt(month) * 100 + Integer.parseInt(day)));
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

    public JSONArray query(String indexDir, String queryContent, int page, int number, int timeRange)
    {
        IndexReader reader = null;
        JSONArray result = new JSONArray();
        try {
            long queryStart = System.currentTimeMillis();

            Date curDate = new Date();
            System.out.println(curDate.toString());
            //设置分词器
            Analyzer analyzer = new SmartChineseAnalyzer();
            //创建解析器
            BooleanClause.Occur[] flags = {BooleanClause.Occur.SHOULD,
                    BooleanClause.Occur.SHOULD};
            //双词条搜索
            Query termQuery = MultiFieldQueryParser.parse(queryContent,new String[]{"content","title"}, flags, analyzer);
            Query timeQuery = rangeQuery(timeRange);
            TopDocs topDocs = null;
            if(timeQuery == null)
            {
                topDocs = searcher.search(termQuery, 100 * 3000);
            }
            else
            {
                BooleanClause bcTitle = new BooleanClause(termQuery, BooleanClause.Occur.MUST);
                BooleanClause bcTime = new BooleanClause(timeQuery, BooleanClause.Occur.MUST);
                BooleanQuery booleanQuery = new BooleanQuery.Builder().add(bcTitle).add(bcTime).build();
                topDocs = searcher.search(booleanQuery, 100 * 3000);
            }
            System.out.println("queryTime: " + (System.currentTimeMillis()-queryStart) + "ms");
            long jsonStart = System.currentTimeMillis();
            //for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            int num = 0;
            int totalNum = topDocs.scoreDocs.length;
            long numStart = System.currentTimeMillis();
            for(;num < totalNum; num++)
            {
                if(topDocs.scoreDocs[num].score < scoreOffset)
                    break; 
            }
            System.out.println("numTime: " +(System.currentTimeMillis() - numStart) + "ms");
            prevTotalNum = num + 1;
            for(int i = page * number; i < page * number +number ;i++){
                if(num < i)
                    return result;
                //拿到文档实例
                Document document = searcher.doc(topDocs.scoreDocs[i].doc);
                //获取所有文档字段
                // List<IndexableField> fieldList = document.getFields();
                //处理文档字段建立Json对象
                JSONObject jsonObject = new JSONObject();
                for(int j=0;j<8;j++)
                {
                    IndexableField field = document.getField(tag[j]);
                    if(field == null)
                    {
                        jsonObject.put(tag[j], "");
                        continue;
                    }
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

    public Query rangeQuery(int timeRange)
    {
        Date date = new Date();
        String format = "yyyyMMdd";
        SimpleDateFormat dateFormat = new SimpleDateFormat(format);
        int today = Integer.parseInt(dateFormat.format(date));
        switch(timeRange)
        {
            //天
            case 1:
                Date yesterday = new Date();
                yesterday.setTime(date.getTime() - 1000*60*60*24);
                System.out.println(dateFormat.format(yesterday));
                int yesterdayInt = Integer.parseInt(dateFormat.format(yesterday));
                return IntPoint.newRangeQuery("time", yesterdayInt, today);
                //break;
            //周
            case 2:
                Date lastWeek = new Date();
                lastWeek.setTime(date.getTime() - 1000*60*60*24*7);
                System.out.println(dateFormat.format(lastWeek));
                int lastWeekInt = Integer.parseInt(dateFormat.format(lastWeek));
                return IntPoint.newRangeQuery("time", lastWeekInt, today);
                //break;
            //月
            case 3:
                Date lastMonth = new Date();
                lastMonth.setTime(date.getTime() - (long)1000*60*60*24*30);
                System.out.println(dateFormat.format(lastMonth));
                int lastMonthInt = Integer.parseInt(dateFormat.format(lastMonth));
                return IntPoint.newRangeQuery("time", lastMonthInt, today);
                //break;
            default:
                return null;
                //break;
        }
    }
}
