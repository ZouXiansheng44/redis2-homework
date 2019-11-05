package com.mingyin;

import redis.clients.jedis.Jedis;

import java.util.*;

public class RedisDemo1 {

    private static final int ARTICLES_PER_PAGE = 5;
    public static void main(String[] args) {
        //主函数调用
        new RedisDemo1().run();
        System.out.println("发布成功!");
    }
    /** 5 */
    public void run() {
        //连接redis服务
        Jedis conn = new Jedis("localhost");
        conn.select(10);

        //发布文章验证

//            String articleId = postArticle(
//                    conn, "username33dda", "A title33d", "http://www.google.com");
//            System.out.println("We posted a new article with id: " + articleId);

        //投票验证操作
        String articleId = "4";
        articleVote(conn, "other_user1", "article:" + articleId);
        List<Map<String,String>> articles = getArticles(conn, 1);
        printArticles(articles);
    }
    /** 1 */
    //文章接口，类似于一个文章的实体类
    public String postArticle(Jedis conn, String user, String title, String link)
    {//投票数与发布时间不用写入
        String articleId = String.valueOf(conn.incr("article:"));//自增ID
        String voted = "voted:" + articleId;
        conn.sadd(voted, user);
        long now = System.currentTimeMillis() / 1000;
        String article= "article:" + articleId;
        HashMap<String,String> articleData = new HashMap<String,String>();//把信息放到字典里
        articleData.put("title", title);
        articleData.put("link", link);
        articleData.put("user", user);
        articleData.put("now", String.valueOf(now));
        articleData.put("votes", "1");
        conn.hmset(article, articleData);//存入一篇文章
        conn.zadd("time:", now, article);//把当前的时间写入 //会问什么类型
        conn.zadd("vote：", 3,article);//初始化 把当前是票数写入
        return articleId;
    }
    /** 2 */
    //排序函数
    public void articleVote(Jedis conn, String user, String article)
    {
        String articleId = article.substring(article.indexOf(':') + 1);
        if (conn.sadd("voted:" + articleId, user) == 1) {//根据时间排序
            conn.hincrBy(article, "votes", 1);
            conn.zincrby("vote:", 1,article);//更新
        }
    }
    /** 3·根据先后顺序展示文章 */
    //分页取出函数
    //map里的数据是成对出现的
    public List<Map<String,String>> getArticles(Jedis conn, int page,String order)//根据投票数分页查询
    {
        int start = (page - 1) * ARTICLES_PER_PAGE;//
        int end = start + ARTICLES_PER_PAGE - 1;
    //    Set<String> ids = conn.zrevrange("time:", start, end);//分页查询
       Set<String> ids = conn.zrange("vote:", start, end);//分页查询
        List<Map<String,String>> articles = new ArrayList<Map<String,String>>();
        for (String id : ids){
            Map<String,String> articleData = conn.hgetAll(id);
            articleData.put("id", id);
            articles.add(articleData);
        }
        return articles;
    }
    //取出
    public List<Map<String,String>> getArticles(Jedis conn,int page){
        return getArticles(conn,page,"time:");
    }
    public List<Map<String,String>> getArticlesByScore(Jedis conn,int page){
        return getArticles(conn,page,"score:");
    }
    /**  04  */
    //取出函数
    private void printArticles(List<Map<String,String>> articles)
    {
        for (Map<String,String> article : articles)
        {
            System.out.println("  id: " + article.get("id"));
            for (Map.Entry<String,String> entry : article.entrySet())
            {
                if (entry.getKey().equals("id")){
                    continue;
                }
                System.out.println("    " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
