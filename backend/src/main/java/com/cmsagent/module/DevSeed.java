package com.cmsagent.module;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
@Configuration
@Profile("dev")
public class DevSeed {
    @Bean ApplicationRunner sampleModule(ModuleService modules) {
        return args -> {
            try(var stream=new ClassPathResource("article.schema.json").getInputStream()) {
                modules.installIfAbsent(new String(stream.readAllBytes(),StandardCharsets.UTF_8));
            }
        };
    }
}
