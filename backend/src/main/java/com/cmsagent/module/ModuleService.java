package com.cmsagent.module;
import com.cmsagent.schema.PageSchema;
import com.cmsagent.common.ApiException;
import com.cmsagent.audit.*;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service
public class ModuleService {
    private final ModuleRepository repository; private final AuditRepository audit; private final ObjectMapper json;
    public ModuleService(ModuleRepository repository, AuditRepository audit, ObjectMapper json) { this.repository=repository; this.audit=audit; this.json=json; }
    public List<PageSchema> list() { return repository.findAll(Sort.by("code")).stream().map(m -> json.readValue(m.schemaJson,PageSchema.class)).toList(); }
    public PageSchema get(String code) { return json.readValue(repository.findById(code).orElseThrow(ApiException::notFound).schemaJson, PageSchema.class); }
    @Transactional
    public PageSchema create(PageSchema schema, String actor) {
        if (repository.existsById(schema.code())) throw ApiException.conflict("模块标识已存在；第一版不允许覆盖已有 Schema");
        repository.saveAndFlush(new ModuleEntity(schema.code(),json.writeValueAsString(schema)));
        audit.save(new AuditEvent(actor,"MODULE_CREATE",schema.code(),schema.code()));
        return schema;
    }
    /** Used by generated registrations; never changes existing data or schema. */
    @Transactional
    public void installIfAbsent(String raw) {
        PageSchema schema=json.readValue(raw,PageSchema.class);
        if (repository.existsById(schema.code())) {
            if (!get(schema.code()).equals(schema)) throw new IllegalStateException("Generated schema conflicts with existing module: " + schema.code());
            return;
        }
        create(schema,"system");
    }
}
