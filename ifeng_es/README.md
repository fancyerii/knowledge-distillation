# indexer

## 1. dump
```
java  -Dlog4j.configuration=log4j-dump.properties -cp ".:../ifeng_crawler-1.0.1-jar-with-dependencies.jar" com.antbrains.ifengcrawler.dumper.DumpHtml ifeng ifeng_dump 2h false &
```
这个脚本把数据库里的detail页面dump成json文件。

参数1: ifeng 数据库名
参数2: ifeng_dump 输出目录
参数3: 2h 两小时dump一次
参数4: false 是否dump全量数据，如果不是，那么程序会记下上一次的时间，只是增量dump

## 2. preprocess

```
java  -Dlog4j.configuration=log4j-prep.properties -cp ".:../ifeng_es-1.0-jar-with-dependencies.jar" com.antbrains.ifeng_es.preprocess.PreprocessArticle ifeng_dump ifeng_prep 10m false &
```
这个脚本对上一个脚本的输出进行预处理，包括分词，然后生成适合检索引的格式

参数1: ifeng_dump 输入目录，来自上一步的输出
参数2: ifeng_prep 输出目录
参数3: 10m 10m检查一次输入目录
参数4: false 是否删除输入文件。如果false，那么不会删除，内存会记录处理过的，但是重启程序后输入目录的所有文件都会重新处理一般【因此调试的时候适合false】

## 3. index
````
java  -Dlog4j.configuration=log4j-index.properties -cp ".:../ifeng_es-1.0-jar-with-dependencies.jar" com.antbrains.ifeng_es.index.ArticleIndexer  ifeng_prep 10m false lili-cluster ai-hz1-spark1 &
```
这个脚本对上一个脚本的输出建立索引

参数1: ifeng_prep 输入目录，来自上一遍的输出
参数2: 10m 10分钟检查一次
参数3: false 是否删除处理过的文件
参数4: lili-cluster elasticsearch的cluster.name
参数5: ai-hz1-spark1 elasticsearch的hostname


