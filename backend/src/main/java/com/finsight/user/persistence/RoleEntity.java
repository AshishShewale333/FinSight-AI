package com.finsight.user.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "roles")
public class RoleEntity {

    @JdbcTypeCode(SqlTypes.CHAR)
    @Id
    @Column(length = 36, nullable = false)
    private String id;

    @Column(nullable = false, unique = true, length = 40)
    private String name;

    protected RoleEntity() { }

    public String getId() { return id; }
    public String getName() { return name; }
}


