## 1. knowledge-distillation是什么？
在很多垂直领域，都有搜索和與情分析的需求。虽然有很多开源的爬虫，搜索引擎，数据抽取，自然语言处理，文本挖掘的工具，但是没有一个完整的开源项目提供简单可用的系统。knowledge-distillation的目标就是提供一个简单但是易于扩展的这样一个系统。不过目前knowledge-distillation只是提供了一个网站的定向抓取和抽取的工具。

## 2. 网站定向(SiteCrawler)抓取
要实现上面的目标，第一步就是获取数据。

和通用搜索引擎不同，我们的定向抓取是遵循一个有向无环图（也就是树）的抓取路径，这样的抓取效率更高效，可以跳过不关注的网页。而且可以自定义更新策略，比如列表页的刷新频率是一天，而内容页不需要刷新。

另外一个特点就是抓取和抽取同时进行，为了节省空间，我们可以不存储原始网页，而只是存储抽取出来感兴趣的属性，比如只保留标题，时间，作者和正文，而那些导航，广告等内容可以丢弃。当然如果空间足够的话还是保留原始内容比较好，因为即使是同一个网站，网页的结构也可能有一下变化。我们通过观察有限的网页归纳出来的抽取模板可能会对某些网页失效，另外我们的需求也可能发生变化，原来不用导航栏现在又可能需要了。如果不保存，则又需要重新抓取。

### 2.1 SiteCrawler的基本概念
#### 2.1.1 WebPage
对应com.antbrains.sc.data.WebPage
一个WebPage就是一般一个网页（但对于列表页也可能是多个翻页的页面），它包括了如下字段：

| 字段名        | 字段类型           |说明 |
| ------------- |:-------------:| -----:|
| id      | Integer | id，如果是mysql，对应自增id，如果是 hbase，则用url的md5做id |
| url      | String      |   网页的url |
| redirectedUrl      | String      |   重定向url |
| title | String      |    Html的 &lt;title&gt;标签，不是待抽取的网页的“正文” |
| charSet      | String      |   网页的编码，通过head或者自动推测出来的编码 |
| tags      | List&lt;String&gt;      |   这个网页的一些标签，比如可以用这个字段来区分不同类型的网页 |
| charSet      | String      |   网页的编码，通过head或者自动推测出来的编码 |
| depth      | int      |   从树根遍历到当前网页的深度，树根是0 |
| content      | String      |   网页的HTML |
| lastVisitTime      | Date      |   网页的上次抓取时间 |
| lastFinishTime      | Date      |   上次“完成”时间，这个字段用来记住列表页最近的一个链接的时间，更新是用来“跳过”已经“翻”过的页码 |
| attrs      | Map&lt;String, String&gt;       |   抽取的一些属性放到这里 |
| attrsFromParent      | Map&lt;String, String&gt;       |   从父亲页面抽取的一些属性，抽取父亲页面的链接时附加在链接上的一些属性，我们可以把他放到孩子页面的这个属性里 |
| crawlPriority      | int      |   抓取优先级（目前并没有实现）|
| otherInfo      | Map&lt;String, Object&gt;      |   放一些其它的信息|
| blocks      | List&lt;Block&gt;      |   这个页面包含的Block，Block的概念参考下面|

#### 2.1.2 Block
对应com.antbrains.sc.data.Block
Block是一个网页的一“块”链接，比如下图的正文列表页是一个Block，右侧的推荐栏是一个Block。
需要注意的是，对于一个列表页，一般都会有不定数目的页数，url可能会发生变化，比如：
http://fo.ifeng.com/listpage/119/1/list.shtml 
http://fo.ifeng.com/listpage/119/2/list.shtml
...
我们认为 http://fo.ifeng.com/listpage/119/1/list.shtml 是一个列表页，它的正文链接Block不单单包括自己那一页的链接(Link)，而且还包括不断向下翻页包含的链接。
也就是说我们的数据库里会有 http://fo.ifeng.com/listpage/119/1/list.shtml  这个WebPage，但是不会有 http://fo.ifeng.com/listpage/119/2/list.shtml  这个WebPage

Block包含如下字段：
| 字段名        | 字段类型           |说明 |
| ------------- |:-------------:| -----:|
| id      | Integer | id |
| tags      | List&lt;String&gt;      |   Block的标签，可以用来区分同一个WebPage的不同Block |
| lastVisitTime      | Date      |   上次更新时间，目前并没有使用 ，原来设计是更新页面到Block级别，不过现在只是到WebPage级别|
| lastFinishTime      | Date      |   上次“完成”时间，目前并没有使用 |
| links      | List&lt;Link&gt;      |   最重要的部分，就是链接 |

#### 2.1.3 Link
对应com.antbrains.sc.data.Link
Link那就是一个链接，它指向一个WebPage
它包含的字段是：

| 字段名        | 字段类型           |说明 |
| ------------- |:-------------:| -----:|
| webPage      | WebPage | 它指向的WebPage |
| linkText      | String      |   锚文本 |
| pos      | int      |   在这次抓取时它的顺序，注意，下次刷新时可能会有新的链接，这个时候下标还是从0开始的，所以一个Block的links里面可能有多个Link的id是相同的|
| lastFinishTime      | Date      |   上次“完成”时间，目前并没有使用 |
| linkAttrs      | Map&lt;String, String&gt;      |   链接的属性 |

### 2.2 SiteCrawler的存储
前面介绍了数据的逻辑结构，本质就是一颗树。而实际的数据是定义了Archiver接口，目前实现了mysql和hbase作为backend的存储引擎，一般如果一个网站的网页不太多，比如不到千万，那么建议使用mysql存储，特别大的网站可以考虑hbase存储。当然hbase存储的好处是后面的数据分析可以直接用mapreduce或者spark分析hbase表，而用mysql要跟hadoop/spark集成就麻烦一些，不过数据量不大的话直接dump出json的格式也就行了。
下面是接口的定义，如果不打算自己实现一个存储引擎，就不需要太关注其细节

```java
	public boolean insert2WebPage(WebPage webPage);

	public void saveUnFinishedWebPage(WebPage webPage, int failInc);

	public void updateWebPage(WebPage webPage);
	
	public void updateFinishTime(WebPage webPage, Date date);

	public void loadBlocks(WebPage webPage);

	public void insert2Block(Block block, WebPage webPage, int pos);

	public void updateBlock(Block block, WebPage webPage, int pos, boolean updateInfo);

	public void insert2Link(List<Integer> blockIds, List<Integer> parentIds, List<Integer> childIds,
			List<Integer> poses, List<String> linkTexts, List<Map<String, String>> attrsList);

	public void insert2Attr(WebPage webPage);

	public void saveLog(String taskId, String type, String msg, Date logTime, int level, String html);

	public void close();

	public void process404Url(String url);
```

#### 2.2.1 MysqlArchiver
MysqlArchiver实现了Mysql的backend，使用的话当然首先需要建一个数据库，然后提供用户名和密码，具体的使用细节后面会解释

#### 2.2.2 HbaseArchiver
使用Hbase作为backend，需要用com.antbrains.sc.tools.SCHbaseTool创建一些hbase表，一般来说如果Hbase已经安装好了话，只需要问管理员要hbase的zookeeper的ip和端口(如果不是默认的2181)就行

### 2.3 爬虫架构

#### 1. 抓取任务队列
这是一个消息队列，我们这里使用hornetq，后面会讲具体的安装和配置。简单来说就是我们会把需要抓取的url（当然还有一些其它信息）放到这个消息队列里面。但是不是所有的url都放到这里。后面的Frontier是存放新发现的url的地方（和一些分布式的全网爬虫不同，我们没有一个urldb）。

#### 2. 爬虫模块
可以在多个抓取节点上运行，它从消息队列里取url，然后抓取抽取最后存储。对于抽取的Block里的新发现的url，会放到Frontier里面。

#### 3. Frontier
存储新发现的url，目前我们主要使用FileFrontier，也就是把url直接存放的文件里。

#### 4. Scheduler
把Frontier里的url放到消息队列里。因为消息队列不能存放太多url，所以发送前会检查队列的大小，如果队列太多，就等等，等队列比较空的时候放入一些。

#### 5. 说明
消息队列全局唯一只有一个。爬虫模块可以多个。Frontier和Scheduler根据实现不同可能多个或者一个。比如MysqlFrontier或者HbaseFrontier会把所有的url放到mysql或者hbase里，所以只有一个，因此只需要启动一个Scheduler就够了。如果是FileFrontier，那么每个爬虫都会把新发现的url存放到本地。因此在这个节点需要启动一个Scheduler【否则这个节点里发现的url都没有办法放到抓取队列里面】


### 2.4 参考示例
下面我们用抓取凤凰网的佛教栏目为例介绍怎么使用SiteCrawler

#### 2.4.1 分析网站结构
在写代码前第一步就是分析要抓取的网站的结构，首先我们要把它分析成一棵树（如果是一个森林怎么办？很简单，创建一个fake的WebPage，它的链接包括这些森林的树根）。

1. 比如凤凰佛教的入口是 http://fo.ifeng.com
这个栏目首页内容很丰富，但是它的内容都是按栏目划分的，如下图所示包括“资讯”，“视频”等等子栏目。因此入口应该包含一个Block，这个Block就是这些子栏目。

2. 我们点开资讯子栏目 http://fo.ifeng.com/listpage/119/1/list.shtml
这是一个列表页，没有实质性的内容，我们需要抽取网页中间部分的所有的链接，它们组成了一个Block，而且注意这个页面有下一页，我们在抽取这个页面的Block时需要不断的翻页，直到满足一定条件。比如翻到最后一页，或者最大多少页。当然我们的目的是抓取2016年的内容，而且列表页是按照时间降序的，所以当我们看到2016年之前的文章就可以跳过去了。时间降序排列的列表也还有一个好处就是我们刷新列表页时如果记住了上一次最后的文章，那么增量更新时可以跳过以前抓取过的文章，这样可以减少很多不必要的翻页操作。

3. 在点进去一层就是 http://fo.ifeng.com/a/20161013/44468069_0.shtml
我们目前只是把内容页的HTML保存下来，供后续分析，当然我们也可以在抓取的过程

通过上面的分析，我们发现，凤凰佛教频道的结构其实是非常简单的3层结构。

#### 2.4.2 创建数据库和消息队列
我们这里的环境是ubuntu 14.04，如果是其它系统，请搜索安装的文档。
1.安装mysql
      sudo apt-get install mysql-server
      注意，默认的mysql的数据放在/var/lib下，阿里云的系统盘一般比较小，所以需要修改数据的位置/可以参考 https://joshuarogers.net/moving-mysql-data-directory-ubuntu/
      请记住mysql root的用户名和密码！！！
  
 2.创建数据库
``` 
DROP DATABASE IF EXISTS `ifeng`;
CREATE DATABASE `ifeng`;
CREATE USER 'ifeng'@'%' IDENTIFIED BY '12345';
GRANT ALL PRIVILEGES ON  ifeng.* TO 'ifeng'@'%';
FLUSH PRIVILEGES;
```
为了安全，可以修改密码，我这里为了演示用了12345作为密码

3.创建schema
```
USE `ifeng`;

CREATE TABLE `attr` (
  `pageId` int(11) NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `value` mediumtext,
  `src` tinyint(4) DEFAULT NULL,
  PRIMARY KEY (`pageId`,`name`),
  KEY `pageId` (`pageId`),
  KEY `srcIdx` (`src`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `linkattr` (
  `blockId` int(11) NOT NULL DEFAULT '0',
  `pageId` int(11) NOT NULL DEFAULT '0',
  `name` varchar(255) NOT NULL DEFAULT '',
  `value` mediumtext,
  PRIMARY KEY (`blockId`,`pageId`,`name`),
  KEY `linkIdx` (`blockId`,`pageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#
# Source for table block
#

CREATE TABLE `block` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `tags` varchar(255) DEFAULT NULL,
  `lastVisitTime` datetime DEFAULT NULL,
  `lastFinishTime` datetime DEFAULT NULL,
  `updateInfo` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


#
# Source for table link
#

CREATE TABLE `link` (
  `blockId` int(11) NOT NULL DEFAULT '0',
  `pageId` int(11) NOT NULL DEFAULT '0',
  `pos` int(11) NOT NULL DEFAULT '0',
  `linkText` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`blockId`,`pageId`),
  KEY `pageId` (`pageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#
# Source for table page_block
#

CREATE TABLE `page_block` (
  `pageId` int(11) NOT NULL DEFAULT '0',
  `blockId` int(11) NOT NULL DEFAULT '0',
  `pos` smallint(6) NOT NULL DEFAULT '0',
  PRIMARY KEY (`pageId`,`pos`,`blockId`),
  KEY `blockId` (`blockId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#
# Source for table todolist
#

CREATE TABLE `todolist` (
  `siteId` int(11) NOT NULL DEFAULT '0',
  `pageId` int(11) NOT NULL DEFAULT '0',
  `url` varchar(2048) NOT NULL DEFAULT '',
  PRIMARY KEY (`siteId`,`pageId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `unfinished` (
  `id` int(11) NOT NULL,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `depth` int NOT NULL,
  `priority` int NOT NULL DEFAULT '0',
  `failcount` int NOT NULL DEFAULT 0,
  `lastVisit` bigint,
  `redirectedUrl` varchar(1024),
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

#
# Source for table webpage
#

CREATE TABLE `bad_url` (
  `url` varchar(1024) NOT NULL DEFAULT '',
  `md5` char(32) NOT NULL,
  `lastUpdate` datetime DEFAULT NULL,
  PRIMARY KEY (`md5`)	
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

CREATE TABLE `webpage` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `redirectedUrl` varchar(1024) DEFAULT NULL,
  `title` varchar(255) DEFAULT NULL,
  `charSet` varchar(50) DEFAULT NULL,
  `tags` varchar(255) DEFAULT NULL,
  `depth` smallint(3) NOT NULL DEFAULT '0',
  `content` mediumblob,
  `lastVisitTime` datetime DEFAULT NULL,
  `lastFinishTime` datetime DEFAULT NULL,
  `type` smallint(6) DEFAULT NULL,
  `websiteId` int(11) NOT NULL DEFAULT '0',
  `md5` char(32) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `md5` (`md5`),
  KEY `url` (`url`(200)),
  KEY `redirectedUrl` (`redirectedUrl`(200)),
  KEY `type` (`type`),
  KEY `siteId` (`websiteId`),
  KEY `depth` (`depth`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8;

#
# Source for table website
#

CREATE TABLE `website` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `siteName` varchar(255) DEFAULT NULL,
  `tags` varchar(1024) DEFAULT NULL,
  `desc` text,
  `indexPageId` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE `pic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `data` MediumBlob,
  `lastModified` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `url` (`url`(200))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `todo_pic` (
  `url` varchar(1024) NOT NULL DEFAULT '',
  `failcount` int NOT NULL DEFAULT 0,
  UNIQUE KEY `url` (`url`(200))
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `crawl_log` (
  `id` bigint(11) NOT NULL AUTO_INCREMENT,
  `taskId` varchar(50) NOT NULL,
  `type` varchar(50) NOT NULL,
  `msg` text,
  `logTime` datetime,
  `level` tinyint,
  `content` mediumtext,
  PRIMARY KEY (`id`),
  KEY `taskId` (`taskId`),
  KEY `type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `cmt_url` (
  `md5` char(32) NOT NULL,
  `url` varchar(1024) NOT NULL DEFAULT '',
  `status` tinyint(4) NOT NULL DEFAULT 0,
  `total_count` int(11) NOT NULL DEFAULT 0,
  `good_count` int(11) NOT NULL DEFAULT 0,
  `bad_count` int(11) NOT NULL DEFAULT 0,
  `neutral_count` int(11) NOT NULL DEFAULT 0,
  `tags` varchar(1024) DEFAULT NULL,
  `last_visit_time` datetime DEFAULT NULL,
  `last_finish_time` datetime DEFAULT NULL,
  `last_publish_time` datetime DEFAULT NULL,
  PRIMARY KEY (`md5`),
  KEY `status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
	
CREATE TABLE `cmt_content` ( 
    `md5` char(32) NOT NULL,
	`user` varchar(255) NOT NULL, 
	`pub_time` bigint NOT NULL,
    `content` mediumtext,
	PRIMARY KEY (`md5`,`user`,`pub_time`), 
	KEY `md5` (`md5`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `component_status` ( 
    `host_name_port` varchar(255) NOT NULL,
	`status` varchar(255) NOT NULL, 
	`last_update` datetime DEFAULT NULL,
	 PRIMARY KEY (`host_name_port`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
	
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

insert into website(siteName,tags) values('凤凰网佛教频道','fojiao');
ALTER TABLE webpage AUTO_INCREMENT = 1;
insert into webpage(url,websiteId,md5) values('http://fo.ifeng.com',1,md5('http://fo.ifeng.com'));

update website set indexPageId=1 where id=1;

```

需要修改的地方包括第一句 use ifeng，如果你是别的数据库，请修改这里
还有就是最后几行

```
insert into website(siteName,tags) values('凤凰网佛教频道','fojiao');
ALTER TABLE webpage AUTO_INCREMENT = 1;
insert into webpage(url,websiteId,md5) values('http://fo.ifeng.com',1,md5('http://fo.ifeng.com'));
```
最后一个insert into非常重要，其中的url一定是这棵树的入口地址（一个字符都不能差，比如url后面加一个/都会有问题）

4.安装hornetq和配置
从 http://downloads.jboss.org/hornetq/hornetq-2.4.0.Final-bin.tar.gz 下载hornetq-2.4.0-Final
解压即可。
我们使用stand-alone的non-cluster就行。
```
cd hornetq-2.4.0.Final/config/stand-alone/non-clustered
```
接下来我们需要知道我们的服务器的内网ip地址，可以用ifconfig查看【我们会让hornetq监听在内网地址上，这样比较安全；但是不要监听在localhost上，因为我们的爬虫是分布式的，其它机器的爬虫需要能访问hornetq】
```
修改hornetq-beans.xml如下所示
      <property name="port">${jnp.port:1099}</property>
      <property name="bindAddress">${jnp.host:内网ip}</property>
      <property name="rmiPort">${jnp.rmiPort:1098}</property>
      <property name="rmiBindAddress">${jnp.host:内网ip}</property>
```

```
修改hornetq-configuration.xml，所以出现host和port的地方都需要修改，这里我只列举了一个！！
      <connector name="netty-throughput">
         <factory-class>org.hornetq.core.remoting.impl.netty.NettyConnectorFactory</factory-class>
         <param key="host"  value="${hornetq.remoting.netty.host:内网ip}"/>
         <param key="port"  value="${hornetq.remoting.netty.batch.port:5455}"/>
         <param key="batch-delay" value="50"/>
      </connector>
  
 需要修改<address-full-policy>PAGE</address-full-policy>
   <address-settings>
      <!--default for catch all-->
      <address-setting match="#">
         <dead-letter-address>jms.queue.DLQ</dead-letter-address>
         <expiry-address>jms.queue.ExpiryQueue</expiry-address>
         <redelivery-delay>0</redelivery-delay>
         <max-size-bytes>104857600</max-size-bytes>
         <message-counter-history-day-limit>10</message-counter-history-day-limit>
         <address-full-policy>PAGE</address-full-policy>
      </address-setting>
   </address-settings>

```

增加队列
```
在hornetq-jms.xml增加：
   <queue name="ifeng" >
      <entry name="queues/ifeng"/>
   </queue>
```
一个队列一般对于一个网站的爬虫，对应一个数据库，所以建议把数据库和队列的名字弄成一样，这样脚本比较好维护。这里我用了ifeng这个名字。
如果一个增加一个队列，就直接在hornetq-jms.xml里加就行，而且有个好处是保存后就生效了，不用重启服务器！！！这个功能非常有用，因为别的爬虫在工作，你重启hornetq会导致它们可能无法工作。

最后修改启动脚本，期待脚本在hornetq-2.4.0.Final/bin下
hornetq-2.4.0.Final/bin/run.sh
```
#export JVM_ARGS="$CLUSTER_PROPS -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms512M -Xmx1024M -Dhornetq.config.dir=$RESOLVED_CONFIG_DIR -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Dlogging.configuration=file://$RESOLVED_CONFIG_DIR/logging.properties -Djava.library.path=./lib/linux-i686:./lib/linux-x86_64"
export JVM_ARGS="$CLUSTER_PROPS -XX:+UseParallelGC -XX:+AggressiveOpts -XX:+UseFastAccessorMethods -Xms512M -Xmx2048M -Dhornetq.config.dir=$RESOLVED_CONFIG_DIR -Djava.util.logging.manager=org.jboss.logmanager.LogManager -Dlogging.configuration=file://$RESOLVED_CONFIG_DIR/logging.properties -Djava.library.path=./lib/linux-i686:./lib/linux-x86_64  -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=3333 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=内网ip"

```

启动hornetq
~/hornetq-2.4.0.Final/bin$ nohup ./run.sh &
使用netstat -lnpt看1099和3333端口是否监听成功了

关闭hornetq
nohup ./run.sh &


#### 2.4.3 编写代码
请参考ifeng_crawler项目，这是一个标准的maven项目，用eclipse或者intellij都可以很容易的导入。

##### 1. 代码结构
ifeng_crawler的代码非常简单，只有两个package：com.antbrains.ifengcrawler.extractor 和 com.antbrains.ifengcrawler.scheduler。extractor包是需要我们编写代码的主要地方，而scheduler包其实非常简单，只是用来驱动爬虫的两个模块——爬虫和Scheduler。

##### 2. UrlPatternExtractor 和 IfengExtractor 
对于每一个url（更加准确的说，每一类url），我们都需要一个extractor来抽取其中的属性和Block。那我们的爬虫怎么知道一个url用什么extractor呢？这就需要com.antbrains.sc.extractor.UrlPatternExtractor 这个类了。
这是一个抽象类，我们需要继承它，这里我们就是写了一个IfengExtractor来继承它。

```
	private Extractor[] extractors = new Extractor[] {
			new Level0Extractor(),
			new ListPageExtractor(),
			new DetailPageExtractor(),
	};

	@Override
	public Extractor getExtractor(String url, String redirectedUrl, int depth) {
		if (depth == 0)
			return extractors[0];
		else if(depth == 1)
			return extractors[1];
		else if(depth == 2)
			return extractors[2];
		return null;
	}
	
```
首先我们定义了一个数组Extractor[] extractors，后面我们会讲Extractor接口，但是现在我们理解为具体一个url的抽取器就行了。
UrlPatternExtractor顾名思义，就是根据不同的Url的Pattern来决定使用具体的哪个抽取器。
不过我们这个凤凰网很简单，我们甚至不用根据url，直接更加网页的depth就能判断应该使用哪个抽取器。但是有些复杂的网站，我们需要根据不同的url的pattern来写正则表达式来判断使用哪一个Extractor。

上面的代码的意思就是：如果网页的层次是0（就是入口http://fo.ifeng.com），那么使用Level0Extractor，如果层次是1（就是列表页，比如http://fo.ifeng.com/listpage/8537/1/list.shtml），那么我们就用ListPageExtractor，如果层次是2（就是新闻的内容页了）我们就用DetailPageExtractor	

##### 3.Extractor接口
上面的这3个Extractor都继承于IfengBasicInfoExtractor，IfengBasicInfoExtractor又继承于BasicInfoExtractor，而BasicInfoExtractor实现了Extractor接口。

```
public interface Extractor {
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId);

	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId);

	public void extractBasicInfo(WebPage webPage, String content, Archiver archiver, String taskId);

	public boolean needUpdate(WebPage webPage);

	public boolean needSaveHtml(WebPage webPage);

	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage);

}
```
3.1 extractProps方法
就是给定我们一个网页，我们需要用这个方法来抽取一些属性，具体可以参考DetailPageExtractor，我们在内容页抽取了新闻的title【前面也说了，如果空间足够，我们最好是先把网页保存下来，然后慢慢在来抽取，所以一般这个方法不太需要实现。除非是空间不够，我们不想保存原始网页，只保存从网页里抽取下来的我们感兴趣的属性(k-v对)】

3.2 extractBlock方法
给定一个网页，我们用这个方法来抽取Block（也就是链接），比如上面的Level0Extractor和ListPageExtractor都实现了这个方法，而DetailPageExtractor因为不要再发现新链接了【当然如果你想抓取评论，也可以从DetailPage里再发现新的url】

3.3 extractBlock方法
抽取网页的基础信息，如html的title标签（不是新闻的正文那个Titlle），字符集等等，一般我们不需要实现，BasicInfoExtractor这个抽象类已经帮我们这个事情了。

3.4 needUpdate方法
这个网页是否需要刷新，我们可以根据上次刷新的时间来判断是否需要更新。
比如DetailPageExtractor的实现：
```
	@Override
	public boolean needUpdate(WebPage webPage) {
		return super.needUpdate(webPage.getLastVisitTime(), 15, 0, 0, 0);
	}
```
它的意思是，如果当前时间离上次更新大于15天，那么就更新，否则不更新。
super.needUpdate是BasicInfoExtractor实现的一个帮助方法
```
	protected boolean needUpdate(Date lastUpdate, int days, int hours, int minutes, int seconds) {
		if (lastUpdate == null)
			return true;
		long interval = 1000L * (seconds + 60 * minutes + 3600 * hours + 24 * 3600 * days);
		if (interval <= 0) {
			interval = Long.MAX_VALUE;
		}
		return System.currentTimeMillis() - lastUpdate.getTime() > interval;
	}
```
3.5 needSaveHtml
是否要保存这个网页，默认是true，如果想节省空间，那么列表页一般可以不保存，因为列表页一般不包含有用的信息。另外如果内容页我们已经把感兴趣的内容抽取下来了，那么为了节省空间，我们也可以不保存。

3.6 needAddChildren2FrontierIfNotUpdate
如果当前网页没有更新，是否需要更新孩子节点。默认的BasicInfoExtractor的实现是true。
如果我们不需要更新内容页，那么我们可以设置为false。什么意思呢？比如当前爬虫正要抓取一个列表页（还没有抓取呢），根据上面的needUpdate，可能因为上次更新时间很近，觉得没有必要刷新这个列表页，那么如果这个方法返回false，那么这个列表页的出去的链接就不会加到Frontier里。如果是true，那么就会加进去（当然即使加进去，那么也可能因为更新时间的策略而跳过，但至少有这个机会来让内容页的抽取器有这个机会）

##### 4. BasicInfoExtractor
这是一个抽象类，实现了Extractor接口，我们一般不需要直接实现Extractor接口，只需要基础BasicInfoExtractor这个抽象类就行了。
首先，这个类实现了needSaveHtml, needAddChildren2FrontierIfNotUpdate和extractBasicInfo3个接口：
```
	@Override
	public boolean needSaveHtml(WebPage webPage) {
		return true;
	};

	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return true;
	}
	
	@Override
	public void extractBasicInfo(WebPage webPage, String content, Archiver archiver, String taskId) {
		...

	}
```
对于剩下的4个接口，BasicInfoExtractor认为需要有具体的Extractor来实现，尤其是extractProps和extractBlock方法，这个肯定得由这些具体的Extractor来实现。
但是它也提供了一下帮助的方法来让我们的工作更简单。这些方法都是Protected的方法，子类可以访问。

4.1 protected boolean needUpdate(Date lastUpdate, int days, int hours, int minutes, int seconds)
这个方法之前我们也提到了，我们经常需要这样的逻辑——如果一个网页的上次更新时间距离现在超过一定时间，那么需要更新，否则不更新。	

4.2 public void testUrl(String url) 这是一个public的方法，用来测试我们的Extractor。这个方法非常重要，我们写了这个抽取器时怎么看效果呢，可以用这个方法来测试是否真的抽取了我们想要的属性和Block。后面我们还会再提到。

4.3 addChild
```
	protected Block addChild(List<String> urls, List<String> anchors, int depth,
			List<Map<String, String>> attrsFromParent, boolean removeEmptyAnchor, boolean removeAnchorInUrl,
			boolean dedup, boolean encodeChinese) {
			
	}
```
我们在实现extractBlocks的时候经常需要很多重复性的工作，把一下url和anchor转换成一个Link，然后加到Block里。	后面的代码里也会有用法，这里不细讲

4.4 	normUrl
```
protected String normUrl(String url) {
		return url;
	}
```

addChild会调用这个方法来归一化url，如果我们需要统一对Link的url除了，可以在这里实现。

4.5 rewriteUrl
```
protected static String rewriteUrl(String oldUrl, String key, String value)
```
在除了翻页的时候，我们经常需要翻页。
比如第一页的url是 http://shouji.baidu.com/software/list?page=1&cid=506
我们向下翻页的时候需要变成 http://shouji.baidu.com/software/list?page=2&cid=506
当然我们可以用正则表达式来做，但是这里提供了 一个更简单的方法：
```
		String oldUrl = "http://shouji.baidu.com/software/list?page=1&cid=506";
		String newUrl = rewriteUrl(oldUrl, "page", "5");
```
这样就把page改成5了
		
##### 5. Level0Extractor		
com.antbrains.ifengcrawler.extractor.Level0Extractor
 接下来我们就来实现第0层也就是凤凰佛教的入口页面的抽取
 
```
 
 public class Level0Extractor extends IfengBasicInfoExtractor {
	protected static Logger logger = Logger.getLogger(Level0Extractor.class);

	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {

	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		List<Block> blocks = new ArrayList<>(1);
		List<String> urls = new ArrayList<>();
		List<String> anchors = new ArrayList<>();

		NodeList aList = parser.selectNodes("//DIV[@id='col_wbf']//LI/A");
		for (int i = 0; i < aList.getLength(); i++) {
			Node a = aList.item(i);
			String anchor = a.getTextContent().trim();
			String href = parser.getNodeText("./@href", a);
			String url = UrlUtils.getAbsoluteUrl(webPage.getUrl(), href);
			if (url.equals("http://fo.ifeng.com/"))
				continue;
			urls.add(url);
			anchors.add(anchor);
		}

		blocks.add(super.addChild(urls, anchors, webPage.getDepth() + 1));
		return blocks;
	}

	@Override
	public boolean needUpdate(WebPage webPage) {
		return true;
	}

	@Override
	public boolean needAddChildren2FrontierIfNotUpdate(WebPage webPage) {
		return true;
	}

	public static void main(String[] args) {
		String[] urls = new String[] { "http://fo.ifeng.com", };
		Level0Extractor ext = new Level0Extractor();
		for (String url : urls) {
			ext.testUrl(url);
		}
	}
	
```

代码比较简单。
因为不需要抽取属性，所以extractProps是个空的方法。
needUpdate和needAddChildren2FrontierIfNotUpdate总是true。
main函数里测试了入口的url，可以开发调试时使用，而核心就是extractBlock

```
		NodeList aList = parser.selectNodes("//DIV[@id='col_wbf']//LI/A");
		for (int i = 0; i < aList.getLength(); i++) {
			Node a = aList.item(i);
			String anchor = a.getTextContent().trim();
			String href = parser.getNodeText("./@href", a);
			String url = UrlUtils.getAbsoluteUrl(webPage.getUrl(), href);
			if (url.equals("http://fo.ifeng.com/"))
				continue;
			urls.add(url);
			anchors.add(anchor);
		}
 
```

最核心的代码其实只有一行: NodeList aList = parser.selectNodes("//DIV[@id='col_wbf']//LI/A");		使用xpath来抽取链接。

关于XPATH请参考 http://www.w3school.com.cn/xpath/
【注意：我们使用的XPATH工具是 NekoHtmlParser，它有一个要求就是Element必须大写。比如//DIV是可以的，但是//div就不行。】

关于XPATH调试建议使用firefox和XPath Checker(https://addons.mozilla.org/EN-us/firefox/addon/xpath-checker/)插件，装了这个插件后，直接点右键“View XPath"就可以在弹出的窗口调试XPATH

对于一个列表页，我们要做的事情就是通过XPATH抽取出所有的url和对应的anchor。当然有些列表页需要翻页，在下面的ListPageExtractor里我们会看到。

##### 6. ListPageExtractor	
和Level0Extractor一样，这个类也不需要抽取属性，所以extractProps是空的。
但是因为需要翻页，所以这个类的extractBlocks比较复杂一点。

6.1 extractItems
首先我们来看一个简单的方法，假设某一页已经下载好了，并且用NekoHtmlParser解析成DOM Tree了，我们怎么从中抽取url和anchor，代码和Level0Extractor类似，不过xpath有所不同而已，请用firefox和xpath checker插件检查xpath的正确性。抽取的结果是ListPageItem，这个类出来url和anchor之外，还有一个pubTime，也就是这个网页的发表时间，在列表页里可以看到。
 
```

	public List<ListPageItem> extractItems(String url, NekoHtmlParser parser, Archiver archiver, String taskId,
			String content) {
		List<ListPageItem> items = new ArrayList<>(10);
		NodeList divs = parser.selectNodes("//DIV[@class='col_L']/DIV[1]/DIV");
		if (divs == null) {
			archiver.saveLog(taskId, "ListPageExtractor.extractItems", "divs==null", new Date(),
					GlobalConstants.LOG_LEVEL_WARN, url + "##" + content);
		} else {
			for (int i = 0; i < divs.getLength(); i++) {
				Node div = divs.item(i);
				String href = parser.getNodeText("./H2/A/@href", div);
				String itemUrl = UrlUtils.getAbsoluteUrl(url, href);
				String title = parser.getNodeText("./H2/A", div);
				String pubTime = parser.getNodeText("./DIV[contains(@class,'box_txt')]/SPAN", div);
				ListPageItem listPageItem = new ListPageItem(title, itemUrl, pubTime);
				items.add(listPageItem);
			}
		}
		return items;
	}
	
```	

6.2 extractBlock
代码看起来比较长，但是更多的逻辑实在处理增量更新的问题。
我们来梳理一下我们的更新逻辑。我们的目标是抓取2015年1月1日之后的文章，另外我们需要实现增量更新。

WebPage类有一个lastFinishTime，用来记录最后更新的链接的日期。刚开始这个值是null，

```

	public static String pubTimeBound="2015-01-01";
	
		
		String timeBound=pubTimeBound;
		if(webPage.getLastFinishTime()!=null){
			timeBound = DateTimeTools.formatDate(webPage.getLastFinishTime());
			logger.info("useLastFinishTime: "+timeBound+" for: "+webPage.getUrl());
		}

```

如果是第一次抓取，那么我们的时间下界timeBound是是2015-01-01，如果之前抓取过一次了，那么就会用上一次的时间【以后也可以增加判断lastVisitTime，如果距离前一次全量更新比较久了，也可以来一次全量更新】

接下来的代码是parse ur，把 http://fo.ifeng.com/listpage/119/1/list.shtml 分割成两个字符串：
http://fo.ifeng.com/listpage/119/ 和 /list.shtml，这样我要抓取第50页就可以在字符串"50"的左右拼上这两个字符串就行了。

```
		//http://fo.ifeng.com/listpage/119/1/list.shtml
		String url=webPage.getUrl();
		String[] urlLeftRightPart=this.parseListUrl(url);
		if(urlLeftRightPart==null){
			archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "urlLeftRightPart", new Date(),
					GlobalConstants.LOG_LEVEL_WARN, url);
			return blocks;
		}
```
		
接下来抽取第一页，因为这个框架已经默认把第一页的内容抓取下来放到了参数content里，并且用NekoHtmlParser parse好了。

```
		//process firstPage
		List<ListPageItem> allItems=this.extractItems(url, parser, archiver, taskId, content);		
		
```

接下来就是从第2页开始往后翻页，由于列表页较多，我们这里使用了3个线程同时翻页。

```
		for(int i=2;i<=maxPage;i+=maxCrawlThread){
			int startPage=i;
			int endPage=Math.min(maxPage, i+maxCrawlThread);
			List<String> batchUrls=new ArrayList<>(maxCrawlThread);
			for(int pg=startPage;pg<endPage;pg++){
				String pageUrl=urlLeftRightPart[0]+pg+urlLeftRightPart[1];
				batchUrls.add(pageUrl);
			}
			List<String[]> batchResults=BatchCrawler.crawler(batchUrls, maxCrawlThread, fetcher, cpi);
			boolean needContinue=true;
			for(String[] pair:batchResults){
				if(pair[1]==null){
					archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "batchResults", new Date(),
							GlobalConstants.LOG_LEVEL_WARN, pair[0]);
					continue;
				}
				try {
					parser.load(pair[1], "UTF8");					
				} catch (Exception e) {
					archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "parser.load", new Date(),
							GlobalConstants.LOG_LEVEL_WARN, pair[0]);
					continue;
				}
				
				List<ListPageItem> items = this.extractItems(pair[0], parser, archiver, taskId, pair[1]);
				allItems.addAll(items);
				for(ListPageItem item:items){
					if(needContinue && item.getPubTime().compareTo(timeBound)<0){
						logger.info("skip older: "+item);
						needContinue=false;
					}
				}
			}
			
			if(!needContinue) break;
		}
		
```

第一次抓取2,3,4页，得到使用List<String[]> batchResults=BatchCrawler.crawler(batchUrls, maxCrawlThread, fetcher, cpi); 一次性抓取3页的内容。

```
			boolean needContinue=true;
			for(String[] pair:batchResults){
				if(pair[1]==null){
					archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "batchResults", new Date(),
							GlobalConstants.LOG_LEVEL_WARN, pair[0]);
					continue;
				}
				try {
					parser.load(pair[1], "UTF8");					
				} catch (Exception e) {
					archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "parser.load", new Date(),
							GlobalConstants.LOG_LEVEL_WARN, pair[0]);
					continue;
				}
				
				List<ListPageItem> items = this.extractItems(pair[0], parser, archiver, taskId, pair[1]);
				allItems.addAll(items);
				for(ListPageItem item:items){
					if(needContinue && item.getPubTime().compareTo(timeBound)<0){
						logger.info("skip older: "+item);
						needContinue=false;
					}
				}
			}
```
然后遍历这3个url的内容。
首先是parser.load()来parse这个html
然后是抽取，并加到allItems里。
```
List<ListPageItem> items = this.extractItems(pair[0], parser, archiver, taskId, pair[1]);
allItems.addAll(items);	
```	

最后遍历抽取的ListPageItem，因为凤凰的新闻是时间降序，如果有一个文章的时间小于2015-01-01（或者上一次更新时间），那么就不用再往后翻页了。另外我们的翻页代码有个maxPage=500，限制一下最大的翻页数。
```
public static int maxPage=500;
		for(int i=2;i<=maxPage;i+=maxCrawlThread){
			...
		}
```

最后的一段代码就是更新lastFinishTime
```
		Collections.sort(allItems, new Comparator<ListPageItem>(){
			@Override
			public int compare(ListPageItem o1, ListPageItem o2) {
				return o2.getPubTime().compareTo(o1.getPubTime());
			}	
		});
		for(ListPageItem item:allItems){
			urls.add(item.getUrl());
			anchors.add(item.getTitle());
		}
		//update finish time
		if(allItems.size()>0){
			String putTime=allItems.iterator().next().getPubTime();
			Date d=DateTimeTools.parseDate(putTime);
			if(d==null){
				archiver.saveLog(taskId, "ListPageExtractor.extractBlock", "parseDate", new Date(),
						GlobalConstants.LOG_LEVEL_WARN, url);
			}else{
				Calendar c = Calendar.getInstance();
				c.setTime(d);
				//minus one hour to avoid same time in ifeng(it's yyyy-MM-dd HH:mm)
				c.add(Calendar.HOUR_OF_DAY, -1);
				d=c.getTime();
				//webPage.setLastFinishTime(d);
				archiver.updateFinishTime(webPage, d);
				logger.info("setLastFinish: "+webPage.getUrl()+"\t"+DateTimeTools.formatDate(d));
			}
		}
```
因为我们从凤凰列表页得到的新闻的时间是2016-03-11，为了防止跳过，我们把减去一天。
然后用archiver.updateFinishTime(webPage, d);更新这个时间。

最后就是把抽取的Blocks加到WebPage里：
```
		blocks.add(super.addChild(urls, anchors, webPage.getDepth() + 1));
		return blocks;
```	

##### 6. DetailPageExtractor
```
	@Override
	public void extractProps(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		String title=parser.getNodeText("//H1");
		Map<String, String> attrs = webPage.getAttrs();
		if(attrs==null){
			attrs = new HashMap<>();
			webPage.setAttrs(attrs);
		}
		attrs.put("#title#", title);
	}

	@Override
	public List<Block> extractBlock(WebPage webPage, NekoHtmlParser parser, HttpClientFetcher fetcher, String content,
			Archiver archiver, String taskId) {
		return null;
	}
```
extractBlock直接返回null，Detail不需要抽取Link
为了演示，我们用XPATH 抽取了新闻的标题。当然我们后续会从网页里抽取更多内容。

接下来我们介绍我们的驱动代码。

##### 7. CrawlerDriver
这个类对于的就是爬虫模块。

```
public class CrawlerDriver {

	public static void main(String[] args) throws Exception {
		BasicCrawlerWithFileFrontier.main(args);
	}

}
```
代码非常简单，基本上复制过去就行

##### 8. SchedulerDriver
这个类对应的就是Scheduler
```
public class SchedulerDriver {
	public static void main(String[] args) throws Exception {
		PoolManager.StartPool("conf", "baidumusic");
		FileUnfinishedScheduler.main(args);
	}
}
```
我们这里使用了FileUnfinishedScheduler，一般也不需要修改。

##### 9. IfengScheduler
这个类是第一次的Scheduler，它的作用就是把入口url（WebPage里Id为1的）放到消息队列里，这样整个爬虫就能开始运作。
代码很多，但是几乎从来不需要修改。

#### 2.4.4 运行代码
可以参考script目录里的脚本
首先需要修改mysql的配置，在script/conf/dbConf.properties
```
把ai-dev改成你的mysql服务器的内网ip，用户名密码也要修改
MYSQL_URL=jdbc:mysql://ai-dev:3306/${db}?useUnicode=true&characterEncoding=utf-8
MYSQL_USER=ifeng
MYSQL_PASS=12345
```

##### 2.4.4.0 安装
mvn compile assembly:single
##### 2.4.4.1 init.sh
【更新】
现在的init.sh调用的是MysqlInit，这个脚本会定期的把入口url放到队列里，从而实现定期的刷新。新版的schedule和crawler会定期的把自己的状态更新到mysql服务器的表里，MysqlInit会根据这些信息来判断上一次抓起是否结束。
```
java -Dlog4j.configuration=log4j-init.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.ifengcrawler.scheduler.IfengInit  "jnp://ai-dev:1099" ai-dev:3333 ifeng 15m 5m &
```
这里有两个参数15m和5m，分别代表更新时间是15分钟，也就是最快15分钟更新一次【前提是一次刷新完成了】，一般可以改大一点，比如1d【一天】/12h【12小时】。另一个参数5m代表5分钟，表示多久没有新的任务产生就代表抓起完成了。

说明：对于一个分布式的系统，要判断一次抓取是否完成，其实并不容易。我们这里使用了比较简单的办法，每个模块定期的汇报自己的情况，如果所以模块在5分钟以上都没有任务，那么我们就认为抓取完成了。
为了支持这个功能，需要增加一个表
```
	CREATE TABLE `component_status` ( 
    `host_name_port` varchar(255) NOT NULL,
	`status` varchar(255) NOT NULL, 
	`last_update` datetime DEFAULT NULL,
	 PRIMARY KEY (`host_name_port`)
	) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```
	

~~其实就是调用IfengScheduler把入口url加到hornetq里。这个程序只需要运行一次。而后面的两个脚本如果出现问题，可以重启【这个程序重新运行意味着网站重新刷新一次】~~

```
~~需要把ai-dev改成你的服务器的内网ip！！！~~
~~  java -Dlog4j.configuration=log4j-init.properties -cp ".:./ifeng_crawler-1.0-jar-with-dependencies.jar" com.antbrains.ifengcrawler.scheduler.IfengScheduler  "jnp://ai-dev:1099" ai-dev:3333 ifeng~~
```

##### 2.4.4.2 schedule.sh
把新发现的url发送到hornetq里。
if [ -d "queuedata" ]
then
        echo "queuedata found."
else
        mkdir queuedata
fi
find queuedata -type f -exec rm {} \;

java -Dlog4j.configuration=log4j-sche.properties -cp ".:./ifeng_crawler-1.0-jar-with-dependencies.jar" com.antbrains.ifengcrawler.scheduler.SchedulerDriver --maxEntriesLocalHeap 1000000 --batchSize 1000 --maxQueueSize 50000  "jnp://ai-dev:1099" ai-dev:3333 ifeng queuedata 22345 &

##### 2.4.4.3 crawl.sh
```
同样需要修改ai-dev！
--workerNum=10 指定爬虫的抓取线程个数
com.antbrains.ifengcrawler.extractor.IfengExtractor 就是我们实现的UrlPatternExtractor接口的类的全限定路径名，告诉我们的框架应该怎么处理url

java  -Dlog4j.configuration=log4j-crawl.properties -cp ".:./ifeng_crawler-1.0-jar-with-dependencies.jar" com.antbrains.ifengcrawler.scheduler.CrawlerDriver --isUpdate true --workerNum 10 "jnp://ai-dev:1099" ai-dev:3333 ifeng com.antbrains.ifengcrawler.extractor.IfengExtractor queuedata &
```

##### 2.4.4.4 日志
 上面3个 init.log  sche.log crawl.log里有详细的日志，如果发现大量Exception，则可能出现问题。
 
##### 2.4.4.5 队列工具
在knowledge-distillation/bin/mqtools（不是在ifeng_crawler)目录下
queue_size.sh 查看队列大小
```
把ai-dev改成你的hornetq的名称，把ifeng改成你的队列名
java -cp ".:./mqtool-1.0-jar-with-dependencies.jar" com.antbrains.mqtool.test.QueueSize "jnp://ai-dev:1099" ai-dev:3333 ifeng
 
clear_queue.sh 清空队列
```
修改ai-dev
```
java -cp ".:./mqtool-1.0-jar-with-dependencies.jar" com.antbrains.mqtool.test.EmptyQueue "jnp://ai-dev:1099" ai-dev:3333 ifeng
```

##### 2.4.4.6 导出工具 scripts/dump.sh
如果想修改功能，比如只导出某个时间段的网页，请参考com.antbrains.ifengcrawler.dumper.DumpHtml

##### 2.4.4.7 在先查看抓取的url的工具【需要maven】

```
cd knowledge-distillation/scui
修改src/main/resources/cfg.properties里的mysql的服务器host，帐号和密码

mvn package
wget 'http://central.maven.org/maven2/org/eclipse/jetty/jetty-runner/9.3.13.v20161014/jetty-runner-9.3.13.v20161014.jar'

然后运行 java -jar jetty-runner-9.3.13.v20161014.jar target/scui.war
```

然后用浏览器【不是在Ubuntu里，而是我们开发的机器】打开http://服务器的外网ip:8080/viewWebPage.jsp?url=http%3A%2F%2Ffo.ifeng.com&dbName=ifeng




