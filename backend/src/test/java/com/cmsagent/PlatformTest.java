package com.cmsagent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlatformTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    private String module() { return "test"+UUID.randomUUID().toString().replace("-", "").substring(0,12); }
    private String schema(String code) { return "{\"version\":1,\"code\":\""+code+"\",\"title\":\"测试模块\",\"fields\":[{\"name\":\"title\",\"label\":\"标题\",\"type\":\"text\",\"required\":true,\"list\":true}]}"; }
    private void createModule(String code) throws Exception {
        mvc.perform(post("/api/modules").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(schema(code))).andExpect(status().isCreated());
    }
    @Test void unauthenticatedApiIs401() throws Exception { mvc.perform(get("/api/modules")).andExpect(status().isUnauthorized()); }
    @Test void healthAndCsrfArePublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
    }
    @Test void csrfRequiredEvenForAdmin() throws Exception {
        mvc.perform(post("/api/modules").with(user("admin").roles("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(schema(module()))).andExpect(status().isForbidden());
    }
    @Test void editorCannotCreateModuleOrReadAudit() throws Exception {
        mvc.perform(post("/api/modules").with(user("editor").roles("EDITOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(schema(module()))).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").with(user("editor").roles("EDITOR"))).andExpect(status().isForbidden());
    }
    @Test void duplicateModuleIsConflict() throws Exception {
        String code=module();createModule(code);
        mvc.perform(post("/api/modules").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(schema(code))).andExpect(status().isConflict());
    }
    @Test void invalidSchemaAndUnknownPropertiesAreRejected() throws Exception {
        for (String data : new String[]{schema("../bad"),schema(module()).replace("\"required\":true,", ""),schema(module()).replace("\"version\":1", "\"version\":1,\"script\":\"bad\"")})
            mvc.perform(post("/api/modules").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(data)).andExpect(status().isBadRequest());
    }
    @Test void crudValidationSearchLockingIsolationAndAudit() throws Exception {
        String code=module(), other=module();createModule(code);createModule(other);
        for(String body:new String[]{"{\"data\":{\"title\":\" \"}}","{\"data\":{\"title\":\"x\",\"extra\":true}}"})
            mvc.perform(post("/api/content/"+code).with(user("editor").roles("EDITOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        String raw=mvc.perform(post("/api/content/"+code).with(user("editor").roles("EDITOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"data\":{\"title\":\"100% content\"}}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.version").value(0)).andReturn().getResponse().getContentAsString();
        String id=json.readValue(raw,Map.class).get("id").toString();
        mvc.perform(get("/api/content/"+other+"/"+id).with(user("editor").roles("EDITOR"))).andExpect(status().isNotFound());
        mvc.perform(get("/api/content/"+code).param("q","%").with(user("editor").roles("EDITOR"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1));
        mvc.perform(get("/api/content/"+code).param("size","101").with(user("editor").roles("EDITOR"))).andExpect(status().isBadRequest());
        String update="{\"data\":{\"title\":\"updated\"},\"version\":0}";
        mvc.perform(put("/api/content/"+code+"/"+id).with(user("editor").roles("EDITOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
        mvc.perform(put("/api/content/"+code+"/"+id).with(user("editor").roles("EDITOR")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isConflict());
        mvc.perform(delete("/api/content/"+code+"/"+id).param("version","1").with(user("editor").roles("EDITOR")).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(delete("/api/content/"+code+"/"+id).param("version","0").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isConflict());
        mvc.perform(delete("/api/content/"+code+"/"+id).param("version","1").with(user("admin").roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/content/"+code).with(user("editor").roles("EDITOR"))).andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        String events=mvc.perform(get("/api/audit").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertTrue(events.contains("CONTENT_DELETE"));assertFalse(events.contains("100% content"));
    }
    @Test void loginFailureAndSuccess() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin").param("password","wrong")).andExpect(status().isUnauthorized());
        var result=mvc.perform(post("/api/auth/login").with(csrf()).param("username","admin").param("password","test-admin-password-123")).andExpect(status().isNoContent()).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)result.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("admin"));
        mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
    }
    @Test void unconfiguredModelReturns503WithoutCreatingAnything() throws Exception {
        mvc.perform(post("/api/agent/plan").with(user("admin").roles("ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(Map.of("code","news","title","资讯","requirement","正文")))).andExpect(status().isServiceUnavailable());
    }
}
