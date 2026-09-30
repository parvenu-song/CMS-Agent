package com.cmsagent.security;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/csrf") Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken(), "headerName", token.getHeaderName()); }
    @GetMapping("/me") Map<String, Object> me(Authentication auth) {
        return Map.of("username", auth.getName(), "roles", auth.getAuthorities().stream().map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).sorted().toList());
    }
}
