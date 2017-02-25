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


public class Search extends HttpServlet {
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
        String query = request.getParameter("q");
        if (query == null || query.trim().equals("")) {
            return SearchResult.EMPTY;
        }

        int pageNo = 1;
        try {
            pageNo = Integer.valueOf(request.getParameter("page"));
        } catch (Exception e) {

        }

        String startTag = this.getParam(request, "startTag", "<span class='hl'>");
        String endTag = this.getParam(request, "endTag", "</span>");

        SearchResult sr = Searcher.getInstance().search(query, pageNo, startTag, endTag);
        return sr;
    }
}
