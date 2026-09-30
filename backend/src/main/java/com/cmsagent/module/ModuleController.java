package com.cmsagent.module;
import com.cmsagent.schema.PageSchema;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/modules")
public class ModuleController {
    private final ModuleService service;
    public ModuleController(ModuleService service) { this.service=service; }
    @GetMapping List<PageSchema> list() { return service.list(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    PageSchema create(@RequestBody PageSchema schema, Principal user) { return service.create(schema,user.getName()); }
}
