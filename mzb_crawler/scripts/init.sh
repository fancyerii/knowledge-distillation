  java -Dlog4j.configuration=log4j-init.properties -cp ".:./mzb_crawler-1.0-SNAPSHOT-jar-with-dependencies.jar" com.antbrains.mzb.scheduler.Init  "jnp://10.26.161.20:1099" 10.26.161.20:3333 mzb 15m 5m &