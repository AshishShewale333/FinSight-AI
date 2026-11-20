package com.finsight.news.persistence;

import com.finsight.company.persistence.CompanyEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;


@Entity
@Table(name="financial_news")
public class FinancialNewsEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36,nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="company_id") private CompanyEntity company;
    @Column(nullable=false,length=100) private String source;
    @Column(name="external_id",nullable=false,length=200) private String externalId;
    @Column(nullable=false,length=500) private String title;
    @Column(nullable=false,length=1000) private String url;
    @Column(name="published_at") private Instant publishedAt;
    @Column(columnDefinition="MEDIUMTEXT") private String content;
    @Column(columnDefinition="MEDIUMTEXT") private String summary;
    @Column(name="summary_status",nullable=false,length=16) private String summaryStatus;
    @Column(name="summary_model",length=120) private String summaryModel;
    @Column(name="summary_generated_at") private Instant summaryGeneratedAt;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    protected FinancialNewsEntity() { }
    public FinancialNewsEntity(String id,CompanyEntity company,String source,String externalId,String title,
            String url,Instant publishedAt,String content,Instant createdAt){
        this.id=id;this.company=company;this.source=source;this.externalId=externalId;this.title=title;
        this.url=url;this.publishedAt=publishedAt;this.content=content;this.summaryStatus="PENDING";this.createdAt=createdAt;
    }
    public String getId(){return id;} public CompanyEntity getCompany(){return company;}
    public String getSource(){return source;} public String getExternalId(){return externalId;}
    public String getTitle(){return title;} public String getUrl(){return url;} public Instant getPublishedAt(){return publishedAt;}
    public String getContent(){return content;} public String getSummary(){return summary;} public String getSummaryStatus(){return summaryStatus;}
    public String getSummaryModel(){return summaryModel;} public Instant getSummaryGeneratedAt(){return summaryGeneratedAt;}
    public Instant getCreatedAt(){return createdAt;}
    public void requestSummary(){ if (!"READY".equals(summaryStatus)) summaryStatus="PENDING"; }
    public void setSummary(String summary,String model,Instant now){this.summary=summary;this.summaryModel=model;this.summaryStatus="READY";this.summaryGeneratedAt=now;}
    public void failSummary(){this.summaryStatus="FAILED";}
}


