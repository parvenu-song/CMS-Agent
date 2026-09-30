package com.cmsagent.content;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name="content_entry")
public class ContentEntity {
    @Id @Column(length=36) public String id;
    @Column(name="module_code", nullable=false, length=32) public String moduleCode;
    @Column(name="data_json", nullable=false, columnDefinition="text") public String dataJson;
    @Column(name="search_text", nullable=false, columnDefinition="text") public String searchText;
    @Version public Long version;
    @Column(name="created_at", nullable=false) public Instant createdAt;
    @Column(name="updated_at", nullable=false) public Instant updatedAt;
    protected ContentEntity() {}
    public ContentEntity(String moduleCode) { this.id=UUID.randomUUID().toString(); this.moduleCode=moduleCode; this.createdAt=Instant.now(); this.updatedAt=createdAt; }
}
