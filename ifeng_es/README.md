 # indexer

## 1. ElasticSearch安装和配置
下载 ES 2.4.1，解压。
### 修改config/elasticsearch.yml文件
主要修改的地方如下：
```
cluster.name: lili-cluster

path.data: /path/to/data # 存放索引的目录，需要比较大的空间
path.logs: /path/to/logs # 存放log的目录
network.host: _eth0_ # 监听的ip，这里用eth0，一般建议用内网地址【外网地址会很危险，localhost的话只能本机访问】

#discovery.zen.ping.unicast.hosts: ["ai-hz1-spark1"] # 广播的服务器，如果是一台机器，不需要配置，注释掉就行。
```
### 创建索引和mapping
```
运行./scripts/create_index.sh
运行前请修改common.sh的这一行
export MY_ES_SERVER=localhost:9200
如果ES服务器是localhost就不需要修改了，否则改成ES服务器的ip地址
另外请注意运行这个脚本会清掉索引，所以需要重新建索引。
```

## 2. dump
```
java  -Dlog4j.configuration=log4j-dump.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.ifengcrawler.dumper.DumpHtml ifeng ifeng_dump 2h false &
```
这个脚本把数据库里的detail页面dump成json文件。

参数1: ifeng 数据库名
参数2: ifeng_dump 输出目录
参数3: 2h 两小时dump一次
参数4: false 是否dump全量数据，如果不是，那么程序会记下上一次的时间，只是增量dump

## 3. preprocess

```
java  -Dlog4j.configuration=log4j-prep.properties -cp ".:../ifeng_es-1.0-jar-with-dependencies.jar" com.antbrains.ifeng_es.preprocess.PreprocessArticle ifeng_dump ifeng_prep 10m false &
```
这个脚本对上一个脚本的输出进行预处理，包括分词，然后生成适合检索引的格式

参数1: ifeng_dump 输入目录，来自上一步的输出
参数2: ifeng_prep 输出目录
参数3: 10m 10m检查一次输入目录
参数4: false 是否删除输入文件。如果false，那么不会删除，内存会记录处理过的，但是重启程序后输入目录的所有文件都会重新处理一般【因此调试的时候适合false】

## 4. index
````
java  -Dlog4j.configuration=log4j-index.properties -cp ".:../ifeng_es-1.0-jar-with-dependencies.jar" com.antbrains.ifeng_es.index.ArticleIndexer  ifeng_prep 10m false lili-cluster ai-hz1-spark1 &
```
这个脚本对上一个脚本的输出建立索引

参数1: ifeng_prep 输入目录，来自上一遍的输出
参数2: 10m 10分钟检查一次
参数3: false 是否删除处理过的文件
参数4: lili-cluster elasticsearch的cluster.name
参数5: ai-hz1-spark1 elasticsearch的hostname


