java  -Dlog4j.configuration=log4j-crawl.properties -cp ".:./fjnet_crawler-1.0-SNAPSHOT-jar-with-dependencies.jar" com.antbrains.fjnet_crawler.scheduler.CrawlerDriver --isUpdate true --workerNum 10 "jnp://ai-dev:1099" ai-dev:3333 ifeng com.antbrains.fjnet_crawler.extractor.FjnetExtractor queuedata &