package com.github.wyxpku.searchapi.searcher;

import com.github.wyxpku.es_search.search.ArticleSearcher;
import com.github.wyxpku.es_search.search.SearchResult;
import com.github.wyxpku.searchapi.api.ConfigReader;
import org.apache.log4j.Logger;

public class Searcher {
    protected static Logger logger = Logger.getLogger(Searcher.class);
    private static Searcher instance = new Searcher();
    private float titleBoost = 5.0f;
    private ArticleSearcher searcher;

    private Searcher() {
        String clusterName = ConfigReader.getProps().getProperty("es.clusterName");
        String hostName = ConfigReader.getProps().getProperty("es.hostName");
        String titleBoostStr = ConfigReader.getProps().getProperty("es.titleBoost");
        if (titleBoostStr != null && !titleBoostStr.isEmpty()) {
            titleBoost = Float.valueOf(titleBoostStr);
        }
        try {
            searcher = new ArticleSearcher(clusterName, hostName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    public static Searcher getInstance() {
        return instance;
    }

    public SearchResult search(String query, int page, String startTag, String endTag) {
        return searcher.search(query, titleBoost, page, startTag, endTag);
    }

    public SearchResult range_search(String startDate, String endDate, int pageNo) {
        return searcher.rangesearch(startDate, endDate, pageNo);
    }
}

