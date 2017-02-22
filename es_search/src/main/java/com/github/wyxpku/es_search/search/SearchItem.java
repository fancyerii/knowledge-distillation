package com.github.wyxpku.es_search.search;

/**
 * Created by wyx on 2017/2/20.
 */

public class SearchItem {
    private String title;
    private String titleHighlight;
    private String content;
    private String contentHightlight;
    private String pubTime;
    private String url;
    private String src;
    private String mainImage;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleHighlight() {
        return titleHighlight;
    }

    public void setTitleHighlight(String titleHighlight) {
        this.titleHighlight = titleHighlight;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentHightlight() {
        return contentHightlight;
    }

    public void setContentHightlight(String contentHightlight) {
        this.contentHightlight = contentHightlight;
    }

    public String getPubTime() {
        return pubTime;
    }

    public void setPubTime(String pubTime) {
        this.pubTime = pubTime;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public String getMainImage() {
        return mainImage;
    }

    public void setMainImage(String figurl) {
        this.mainImage = figurl;
    }
}