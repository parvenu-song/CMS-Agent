package com.cmsagent.audit;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
public class AuditController {
    private final AuditRepository repository;
    public AuditController(AuditRepository repository) { this.repository=repository; }
    @GetMapping("/api/audit") List<AuditEvent> list() { return repository.findTop100ByOrderByCreatedAtDesc(); }
}
