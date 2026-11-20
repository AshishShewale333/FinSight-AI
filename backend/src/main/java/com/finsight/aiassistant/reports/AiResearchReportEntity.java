package com.finsight.aiassistant.reports;

import com.finsight.company.persistence.CompanyEntity;
import com.finsight.user.persistence.UserEntity;

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
import java.util.Map;


@Entity
@Table(name="ai_research_reports")
public class AiResearchReportEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36,nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="owner_id",nullable=false) private UserEntity owner;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="company_id",nullable=false) private CompanyEntity company;
    @Column(nullable=false,length=20) private String status;
    @Column(name="prompt_version",nullable=false,length=40) private String promptVersion;
    @Column(name="model_name",length=120) private String modelName;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="result_json",columnDefinition="json") private Map<String,Object> result;
    @Column(name="failure_reason",length=500) private String failureReason;
    @Column(name="idempotency_key",length=100) private String idempotencyKey;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="completed_at") private Instant completedAt;
    protected AiResearchReportEntity() { }
    public AiResearchReportEntity(String id,UserEntity owner,CompanyEntity company,String key,Instant now){
        this.id=id;this.owner=owner;this.company=company;this.idempotencyKey=key;this.status="QUEUED";this.promptVersion="1.0.0";this.createdAt=now;
    }
    public String getId(){return id;} public UserEntity getOwner(){return owner;} public CompanyEntity getCompany(){return company;}
    public String getStatus(){return status;} public String getPromptVersion(){return promptVersion;} public String getModelName(){return modelName;}
    public Map<String,Object> getResult(){return result;} public String getFailureReason(){return failureReason;}
    public String getIdempotencyKey(){return idempotencyKey;} public Instant getCreatedAt(){return createdAt;} public Instant getCompletedAt(){return completedAt;}
    public void complete(Map<String,Object> value,String model,Instant now){this.result=value;this.modelName=model;this.status="COMPLETED";this.completedAt=now;this.failureReason=null;}
    public void fail(String reason,Instant now){this.status="FAILED";this.failureReason=reason;this.completedAt=now;}
}


