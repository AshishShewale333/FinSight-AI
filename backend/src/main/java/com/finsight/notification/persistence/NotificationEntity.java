package com.finsight.notification.persistence;

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


@Entity
@Table(name="notification_events")
public class NotificationEntity {
    @JdbcTypeCode(SqlTypes.CHAR)
    @Id @Column(length=36, nullable=false) private String id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private UserEntity user;
    @Column(name="event_type", nullable=false, length=80) private String eventType;
    @Column(nullable=false, length=200) private String title;
    @Column(nullable=false, length=1000) private String body;
    @Column(name="dedupe_key", length=160) private String dedupeKey;
    @Column(name="read_at") private Instant readAt;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    protected NotificationEntity() { }
    public NotificationEntity(String id, UserEntity user, String eventType, String title,
                              String body, String dedupeKey, Instant createdAt) {
        this.id=id; this.user=user; this.eventType=eventType; this.title=title; this.body=body;
        this.dedupeKey=dedupeKey; this.createdAt=createdAt;
    }
    public String getId() { return id; }
    public UserEntity getUser() { return user; }
    public String getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getDedupeKey() { return dedupeKey; }
    public Instant getReadAt() { return readAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void markRead(Instant now) { if (readAt == null) readAt=now; }
}


