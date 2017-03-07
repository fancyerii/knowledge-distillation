package com.github.wyxpku.searchapi.api;

import com.github.wyxpku.es_search.search.SearchResult;
import com.github.wyxpku.searchapi.searcher.Searcher;
import com.google.gson.Gson;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by wyx on 2017/2/24.
 */
public class RangeSearch extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected static Logger logger = Logger.getLogger(Search.class);
    private Gson gson = new Gson();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(this.doSearch(request)));
    }

    private String getParam(HttpServletRequest req, String name, String def) {
        String v = req.getParameter(name);
        if (v == null || v.trim().equals("")) return def;
        return v;
    }

    private SearchResult doSearch(HttpServletRequest request) {
        String start = request.getParameter("start");
        if (start == null || start.trim().equals("")) {
            return SearchResult.EMPTY;
        }

        String end = request.getParameter("end");

        if (end == null || end.trim().equals("")) {
            end = "";
        }

        int pageNo = 1;
        try {
            pageNo = Integer.valueOf(request.getParameter("page"));
        } catch (Exception e) {

        }

        SearchResult sr = Searcher.getInstance().range_search(start, end, pageNo);
        return sr;
    }
}
