package com.cmsagent.security;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import java.util.Arrays;
@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder encoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(PasswordEncoder encoder, Environment env,
        @Value("${cms.security.admin-password}") String admin,
        @Value("${cms.security.editor-password}") String editor) {
        boolean prod = Arrays.asList(env.getActiveProfiles()).contains("prod");
        for (String password : new String[]{admin, editor}) {
            if (password.length() < 16 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72 || (prod && (password.contains("change-me") || password.startsWith("replace_"))))
                throw new IllegalStateException("配置至少 16 字符、最多 72 UTF-8 字节的独立密码；生产环境禁止开发密码或占位符");
        }
        if (prod && admin.equals(editor)) throw new IllegalStateException("管理员和编辑密码必须不同");
        return new InMemoryUserDetailsManager(
            User.withUsername("admin").password(encoder.encode(admin)).roles("ADMIN", "EDITOR").build(),
            User.withUsername("editor").password(encoder.encode(editor)).roles("EDITOR").build());
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(a -> a
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            .requestMatchers("/api/auth/csrf", "/api/auth/login", "/actuator/health").permitAll()
            .requestMatchers("/api/audit", "/api/agent/plan").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/modules").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/content/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.POST, "/api/content/**").hasAnyRole("ADMIN", "EDITOR")
            .requestMatchers(HttpMethod.PUT, "/api/content/**").hasAnyRole("ADMIN", "EDITOR")
            .anyRequest().authenticated())
          // Session-backed CSRF stays enabled, including login/logout.
          .formLogin(f -> f.loginProcessingUrl("/api/auth/login")
            .successHandler((req,res,auth) -> res.setStatus(204))
            .failureHandler((req,res,e) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"用户名或密码错误\"}"); }))
          .logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
          .exceptionHandling(e -> e
            .authenticationEntryPoint((req,res,ex) -> { res.setStatus(401); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"请先登录\"}"); })
            .accessDeniedHandler((req,res,ex) -> { res.setStatus(403); res.setContentType("application/json;charset=UTF-8"); res.getWriter().write("{\"message\":\"权限不足或安全令牌失效\"}"); }));
        return http.build();
    }
}
