package com.finsight.news.api;

import java.time.Instant;

public record FinancialNewsResponse(String id,String companyId,String title,String source,
        String url,Instant publishedAt,String summary,String summaryStatus,String summaryModel) { }
