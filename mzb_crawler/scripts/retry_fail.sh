  java -Dlog4j.configuration=log4j-retry.properties -cp ".:./ifeng_crawler-1.0-jar-with-dependencies.jar" com.antbrains.sc.scheduler.UnfinishedScheduler2  "jnp://ai-dev:1099"  ai-dev:3333 ifeng &
