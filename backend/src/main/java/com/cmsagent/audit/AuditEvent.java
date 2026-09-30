package com.cmsagent.audit;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="audit_event")
public class AuditEvent {
    @Id @Column(length=36) public String id;
    @Column(nullable=false, length=100) public String actor;
    @Column(nullable=false, length=40) public String action;
    @Column(name="module_code", nullable=false, length=32) public String moduleCode;
    @Column(name="target_id", nullable=false, length=36) public String targetId;
    @Column(name="created_at", nullable=false) public Instant createdAt;
    protected AuditEvent() {}
    public AuditEvent(String actor, String action, String moduleCode, String targetId) {
        this.id=UUID.randomUUID().toString(); this.actor=actor; this.action=action; this.moduleCode=moduleCode; this.targetId=targetId; this.createdAt=Instant.now();
    }
}
