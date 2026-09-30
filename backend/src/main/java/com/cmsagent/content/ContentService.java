package com.cmsagent.content;
import com.cmsagent.module.ModuleService;
import com.cmsagent.audit.*;
import com.cmsagent.common.ApiException;
import com.cmsagent.schema.PageSchema;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
@Service
public class ContentService {
    public record Entry(String id, Long version, Map<?,?> data, Instant createdAt, Instant updatedAt) {}
    public record Result(List<Entry> items, long total, int page, int size) {}
    public record Write(Map<String,Object> data, Long version) {}
    private final ContentRepository repository; private final ModuleService modules; private final AuditRepository audit; private final ObjectMapper json;
    public ContentService(ContentRepository repository, ModuleService modules, AuditRepository audit, ObjectMapper json) { this.repository=repository; this.modules=modules; this.audit=audit; this.json=json; }
    public Result list(String code,int page,int size,String q) {
        PageSchema.require(page>=0 && page<=100000 && size>=1 && size<=100 && q.length()<=100,"分页或搜索参数无效");
        modules.get(code);
        String pattern="%"+q.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
        var result=repository.search(code,pattern,PageRequest.of(page,size,Sort.by(Sort.Order.desc("updatedAt"),Sort.Order.asc("id"))));
        return new Result(result.stream().map(this::view).toList(),result.getTotalElements(),page,size);
    }
    public Entry get(String code,String id) { return view(find(code,id)); }
    @Transactional
    public Entry create(String code,Write request,String actor) {
        PageSchema.require(request.version()==null,"新建记录不能带 version");
        ContentEntity entity=new ContentEntity(code); fill(entity,request.data());
        entity=repository.saveAndFlush(entity);
        audit.save(new AuditEvent(actor,"CONTENT_CREATE",code,entity.id)); return view(entity);
    }
    @Transactional
    public Entry update(String code,String id,Write request,String actor) {
        ContentEntity entity=find(code,id); checkVersion(entity,request.version()); fill(entity,request.data());
        entity=repository.saveAndFlush(entity);
        audit.save(new AuditEvent(actor,"CONTENT_UPDATE",code,id)); return view(entity);
    }
    @Transactional
    public void delete(String code,String id,long version,String actor) {
        ContentEntity entity=find(code,id); checkVersion(entity,version); repository.delete(entity); repository.flush();
        audit.save(new AuditEvent(actor,"CONTENT_DELETE",code,id));
    }
    private ContentEntity find(String code,String id) { return repository.findByIdAndModuleCode(id,code).orElseThrow(ApiException::notFound); }
    private void checkVersion(ContentEntity entity,Long version) {
        PageSchema.require(version!=null && version>=0,"更新和删除必须带 version");
        if (!Objects.equals(entity.version,version)) throw ApiException.conflict("记录已被其他人修改，请刷新后重试");
    }
    private void fill(ContentEntity entity,Map<String,Object> raw) {
        Map<String,Object> data=modules.get(entity.moduleCode).validateData(raw);
        entity.dataJson=json.writeValueAsString(data);
        PageSchema.require(entity.dataJson.length()<=65536,"记录内容总长度不能超过 64 KiB 字符");
        entity.searchText=data.values().stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.joining(" ")).toLowerCase(Locale.ROOT);
        entity.updatedAt=Instant.now();
    }
    private Entry view(ContentEntity e) { return new Entry(e.id,e.version,json.readValue(e.dataJson,Map.class),e.createdAt,e.updatedAt); }
}
