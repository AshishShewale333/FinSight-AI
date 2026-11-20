package com.finsight.aiassistant.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.Map;


@Entity
@Table(name="chat_messages", uniqueConstraints=@UniqueConstraint(name="uq_chat_message_order", columnNames={"session_id", "sequence_no"}))
public class ChatMessageEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="session_id", nullable=false) private ChatSessionEntity session;
    @Column(name="sequence_no", nullable=false) private long sequenceNo;
    @Column(nullable=false, length=16) private String speaker;
    @Column(nullable=false, columnDefinition="MEDIUMTEXT") private String content;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="json") private List<Map<String,Object>> citations;
    @Column(name="model_name", length=120) private String modelName;
    @Column(name="prompt_version", length=40) private String promptVersion;
    @Column(name="token_count") private Integer tokenCount;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    protected ChatMessageEntity() { }
    public ChatMessageEntity(String id, ChatSessionEntity session, long sequenceNo, String speaker,
                             String content, List<Map<String,Object>> citations, String modelName,
                             String promptVersion, Instant createdAt) {
        this.id=id; this.session=session; this.sequenceNo=sequenceNo; this.speaker=speaker;
        this.content=content; this.citations=citations; this.modelName=modelName;
        this.promptVersion=promptVersion; this.createdAt=createdAt;
    }
    public String getId() { return id; }
    public ChatSessionEntity getSession() { return session; }
    public long getSequenceNo() { return sequenceNo; }
    public String getSpeaker() { return speaker; }
    public String getContent() { return content; }
    public List<Map<String,Object>> getCitations() { return citations; }
    public String getModelName() { return modelName; }
    public String getPromptVersion() { return promptVersion; }
    public Integer getTokenCount() { return tokenCount; }
    public Instant getCreatedAt() { return createdAt; }
}


