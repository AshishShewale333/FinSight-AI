package com.finsight.news.domain;

import java.time.Instant;
import java.util.List;

public interface FinancialNewsProvider {
    List<NewsItem> latestMarketNews();
    String name();
    record NewsItem(String source,String externalId,String title,String url,Instant publishedAt,String content) { }
}
