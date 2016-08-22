if [ -d "queuedata" ]
then
	echo "queuedata found."
else
	mkdir queuedata
fi
find queuedata -type f -exec rm {} \;

java -Dlog4j.configuration=log4j-sche.properties -cp ".:./ifeng_crawler-1.0-jar-with-dependencies.jar" com.antbrains.ifengcrawler.scheduler.SchedulerDriver --maxEntriesLocalHeap 1000000 --batchSize 1000 --maxQueueSize 50000  "jnp://ai-dev:1099" ai-dev:3333 ifeng queuedata 22345 &
