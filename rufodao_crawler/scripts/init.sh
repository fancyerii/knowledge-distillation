  java -Dlog4j.configuration=log4j-init.properties -cp ".:./rufodao_crawler-1.0-SNAPSHOT-jar-with-dependencies.jar" com.antbrains.rufodao.scheduler.Init  "jnp://10.26.161.20:1099" 10.26.161.20:3333 rufodao 15m 5m &
