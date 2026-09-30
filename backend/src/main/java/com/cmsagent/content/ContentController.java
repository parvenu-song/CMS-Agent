package com.cmsagent.content;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/content/{code}")
public class ContentController {
    private final ContentService service;
    public ContentController(ContentService service) { this.service=service; }
    @GetMapping ContentService.Result list(@PathVariable String code,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,@RequestParam(defaultValue="") String q) { return service.list(code,page,size,q); }
    @GetMapping("/{id}") ContentService.Entry get(@PathVariable String code,@PathVariable String id) { return service.get(code,id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ContentService.Entry create(@PathVariable String code,@RequestBody ContentService.Write request,Principal user) { return service.create(code,request,user.getName()); }
    @PutMapping("/{id}") ContentService.Entry update(@PathVariable String code,@PathVariable String id,@RequestBody ContentService.Write request,Principal user) { return service.update(code,id,request,user.getName()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable String code,@PathVariable String id,@RequestParam long version,Principal user) { service.delete(code,id,version,user.getName()); }
}
