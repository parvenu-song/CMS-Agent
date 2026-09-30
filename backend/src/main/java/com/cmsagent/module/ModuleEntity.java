package com.cmsagent.module;
import jakarta.persistence.*;
@Entity
@Table(name="cms_module")
public class ModuleEntity {
    @Id @Column(length=32) public String code;
    @Version public Long revision;
    @Column(name="schema_json", nullable=false, columnDefinition="text") public String schemaJson;
    protected ModuleEntity() {}
    public ModuleEntity(String code, String schemaJson) { this.code=code; this.schemaJson=schemaJson; }
}
