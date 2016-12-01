if [ -d "queuedata" ]
then
	echo "queuedata found."
else
	mkdir queuedata
fi
find queuedata -type f -exec rm {} \;

java -Dlog4j.configuration=log4j-sche.properties -cp ".:./saraxwzx_crawler-1.0-SNAPSHOT-jar-with-dependencies.jar" com.antbrains.saraxwzx.scheduler.SchedulerDriver --maxEntriesLocalHeap 1000000 --batchSize 1000 --maxQueueSize 50000  "jnp://10.26.161.20:1099" 10.26.161.20:3333 saraxwzx queuedata 22347 &
