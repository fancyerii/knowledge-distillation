# review_crawler

## 1. 工作流程
### 1.1 DumpReviewUrl
DumpReviewUrl程序定期从webpage表里将要抓取评论的url导出到一个目录，生成一个Json文件。
### 1.2 ImportReviewUrl
把上一步导出的url保存到review_status表里

### 1.3 ReviewScheduler
把review_status里状态为未抓取的评论url放到消息队列里

### 1.4 ReviewCrawler
这是一个接口，不同的网站需要写抓取翻页跳过的逻辑，而前面3个代码是可以重用的。这里给出了IfengReviewCrawler。需要实现的接口是：
```
	protected abstract List<ReviewContent> crawlAReview(CrawlTask task) throws Exception;
```
实现参考IfengReviewCrawler类。

## 2. 数据表
需要在原来的数据库里增加两个表：
```
CREATE TABLE `review_status` ( 
    `page_id` int NOT NULL,
	`page_url` varchar(1024) NOT NULL, 
	`add_time` datetime NOT NULL,
    `lastUpdate` datetime,
    `lastestReviewTime` varchar(50),
    `lastAdded` int,
    `total_review` int default 0,
    `update_interval_sec` int,
    `crawling_status` int default 0,
	PRIMARY KEY (`page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;	

CREATE TABLE `review_content` ( 
    `rv_id` varchar(255) NOT NULL,
    `page_id` int NOT NULL,
    `date` varchar(50),
    `content` mediumtext,
	PRIMARY KEY (`rv_id`),
	KEY `pid` (`page_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;	
```

## 3. 脚本示例
### 3.1 dumpReivewUrl
```
java  -Dlog4j.configuration=log4j-dumpurl.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.ifengcrawler.dumper.DumpReviewUrl ifeng ifeng_url_dump 2h false &
```

### 3.2  importReviewUrl
```
java  -Dlog4j.configuration=log4j-importreview.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.ifengcrawler.dumper.ImportReviewUrl ifeng_url_dump 2h false ifeng &

```

### 3.3 ReviewScheduler
```
java -Dlog4j.configuration=log4j-sche.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.reviewcrawler.scheduler.ReviewScheduler ifeng ifeng_review "jnp://ai-dev:1099" ai-dev:3333  &
```

### 3.4 ReviewCrawler
```
java -Dlog4j.configuration=log4j-crawl.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.reviewcrawler.crawler.ReviewCrawlerDriver 3 ifeng ifeng_review "jnp://ai-dev:1099" ai-dev:3333 com.antbrains.ifengcrawler.review.IfengReviewCrawler &
```

