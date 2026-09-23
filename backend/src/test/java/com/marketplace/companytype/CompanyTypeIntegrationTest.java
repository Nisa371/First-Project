package com.marketplace.companytype;

import com.marketplace.auth.JwtService;
import com.marketplace.employer.*;
import com.marketplace.user.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test") @SpringBootTest @AutoConfigureMockMvc
class CompanyTypeIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired CompanyTypeRepository types;
    @Autowired EmployerProfileRepository employers;
    @Autowired UserRepository users;
    @Autowired JwtService jwt;
    @Autowired CompanyTypeCatalog catalog;
    Long normal() { return types.findByNormalizedName("pharmaceuticals").orElseThrow().getId(); }
    Long other() { return types.findByCode("OTHER").orElseThrow().getId(); }
    User user(Role role) {
        var user = new User(); user.setEmail(UUID.randomUUID()+"@example.test"); user.setPasswordHash("test-only-hash"); user.setRole(role);
        return users.saveAndFlush(user);
    }
    String auth(Role role) { return "Bearer "+jwt.issue(user(role).getId()); }
    ResultActions register(Long type, String custom) throws Exception {
        var body = new HashMap<String,Object>(Map.of("email", UUID.randomUUID()+"@example.test", "password", "SafePass123!", "accountType", "EMPLOYER", "companyName", "Test Company"));
        body.put("companyTypeId", type); body.put("customCompanyType", custom);
        return mvc.perform(post("/api/auth/register").contentType("application/json").content(mapper.writeValueAsString(body)));
    }
    String employer(Long type, String custom) throws Exception {
        var result = register(type, custom).andExpect(status().isCreated()).andReturn();
        return "Bearer "+mapper.readTree(result.getResponse().getContentAsString()).get("token").asText();
    }
    ResultActions update(String token, Long type, String custom) throws Exception {
        var body = new HashMap<String,Object>(Map.of("companyName", "Updated", "companyTypeId", type)); body.put("customCompanyType", custom);
        return mvc.perform(put("/api/employers/me").header("Authorization", token).contentType("application/json").content(mapper.writeValueAsString(body)));
    }
    Long create(String admin, String name) throws Exception {
        var result = mvc.perform(post("/api/admin/company-types").header("Authorization", admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("name",name,"active",true))))
            .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
    @Test void registrationValidationIsAtomicAndProfileTransitionsWork() throws Exception {
        long count = users.count();
        register(other(), "  ").andExpect(status().isBadRequest());
        register(normal(), "Untrusted custom").andExpect(status().isBadRequest());
        register(Long.MAX_VALUE, null).andExpect(status().isBadRequest());
        register(null, null).andExpect(status().isBadRequest());
        register(other(), "x".repeat(121)).andExpect(status().isBadRequest());
        assertThat(users.count()).isEqualTo(count);
        String token = employer(normal(), null);
        mvc.perform(get("/api/employers/me").header("Authorization",token)).andExpect(status().isOk())
            .andExpect(jsonPath("$.companyTypeId").value(normal())).andExpect(jsonPath("$.companyTypeName").value("Pharmaceuticals"));
        update(token, other(), " Architecture Consultancy ").andExpect(status().isOk()).andExpect(jsonPath("$.customCompanyType").value("Architecture Consultancy"));
        update(token, other(), "Design Studio").andExpect(status().isOk()).andExpect(jsonPath("$.customCompanyType").value("Design Studio"));
        update(token, normal(), "stale").andExpect(status().isBadRequest());
        update(token, other(), " ").andExpect(status().isBadRequest());
        update(token, Long.MAX_VALUE, null).andExpect(status().isBadRequest());
        update(token, normal(), null).andExpect(status().isOk()).andExpect(jsonPath("$.customCompanyType").isEmpty());
        update(token, types.findByNormalizedName("fmcg").orElseThrow().getId(), null).andExpect(status().isOk());
        String customToken = employer(other(), "Custom business");
        mvc.perform(get("/api/employers/me").header("Authorization",customToken)).andExpect(jsonPath("$.companyTypeOther").value(true)).andExpect(jsonPath("$.customCompanyType").value("Custom business"));
    }
    @Test void adminLifecyclePreservesReferencesAndOther() throws Exception {
        String admin = auth(Role.ADMIN), name = "Catalog " + UUID.randomUUID();
        Long id = create(admin, "  "+name+"  ");
        mvc.perform(post("/api/admin/company-types").header("Authorization",admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("name",name.toUpperCase(),"active",true)))).andExpect(status().isConflict());
        String token = employer(id, null);
        mvc.perform(put("/api/admin/company-types/"+id).header("Authorization",admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("name",name+" renamed","active",false)))).andExpect(status().isOk());
        register(id,null).andExpect(status().isBadRequest());
        update(employer(normal(),null),id,null).andExpect(status().isBadRequest());
        update(token,id,null).andExpect(status().isOk()).andExpect(jsonPath("$.companyTypeActive").value(false));
        mvc.perform(get("/api/company-types")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.id == "+id+")]").isEmpty());
        mvc.perform(put("/api/admin/company-types/"+id).header("Authorization",admin).contentType("application/json").content(mapper.writeValueAsString(Map.of("name",name+" renamed","active",true)))).andExpect(status().isOk());
        mvc.perform(delete("/api/admin/company-types/"+id).header("Authorization",admin)).andExpect(status().isNoContent());
        assertThat(types.findById(id).orElseThrow().isActive()).isFalse();
        mvc.perform(get("/api/employers/me").header("Authorization",token)).andExpect(jsonPath("$.companyTypeName").value(name+" renamed"));
        Long unused = create(admin,"Unused "+UUID.randomUUID());
        mvc.perform(delete("/api/admin/company-types/"+unused).header("Authorization",admin)).andExpect(status().isNoContent());
        assertThat(types.findById(unused)).isEmpty();
        for (var input : List.of(Map.of("name","Other","active",false), Map.of("name","Renamed Other","active",true)))
            mvc.perform(put("/api/admin/company-types/"+other()).header("Authorization",admin).contentType("application/json").content(mapper.writeValueAsString(input))).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/admin/company-types/"+other()).header("Authorization",admin)).andExpect(status().isBadRequest());
        catalog.run(null);
        assertThat(types.findById(unused)).isEmpty();
        assertThat(types.findById(id).orElseThrow().isActive()).isFalse();
    }
    @Test void managementRequiresAdminAndTextCannotBypassCatalog() throws Exception {
        mvc.perform(get("/api/company-types")).andExpect(status().isOk()).andExpect(jsonPath("$[?(@.other == true)].name").value(org.hamcrest.Matchers.hasItem("Other")));
        mvc.perform(get("/api/admin/company-types")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/company-types").header("Authorization",auth(Role.ADMIN))).andExpect(status().isOk());
        for (Role role : List.of(Role.CANDIDATE, Role.EMPLOYER, Role.EVALUATOR)) {
            String token = auth(role);
            mvc.perform(get("/api/admin/company-types").header("Authorization",token)).andExpect(status().isForbidden());
            mvc.perform(post("/api/admin/company-types").header("Authorization",token).contentType("application/json").content("{\"name\":\"Forbidden\",\"active\":true}")).andExpect(status().isForbidden());
            mvc.perform(put("/api/admin/company-types/"+normal()).header("Authorization",token).contentType("application/json").content("{\"name\":\"Forbidden\",\"active\":false}")).andExpect(status().isForbidden());
            mvc.perform(delete("/api/admin/company-types/"+normal()).header("Authorization",token)).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/auth/register").contentType("application/json").content("{\"email\":\"test@example.test\",\"password\":\"SafePass123!\",\"accountType\":\"EMPLOYER\",\"companyName\":\"Test\",\"companyTypeName\":\"Made up\"}")).andExpect(status().isBadRequest());
    }
    @Test void legacyMigrationIsLosslessAndIdempotent() throws Exception {
        for (String industry : List.of("  PHARMACEUTICALS  ", "Unknown original industry", "")) {
            var employer = new EmployerProfile(); employer.setUser(user(Role.EMPLOYER)); employer.setCompanyName("Legacy"); employer.setIndustry(industry); employers.saveAndFlush(employer);
            catalog.run(null); catalog.run(null);
            var response = mvc.perform(get("/api/employers/me").header("Authorization","Bearer "+jwt.issue(employer.getUser().getId()))).andExpect(status().isOk());
            if (industry.isBlank()) response.andExpect(jsonPath("$.companyTypeId").isEmpty());
            else if (industry.contains("PHARMA")) response.andExpect(jsonPath("$.companyTypeId").value(normal())).andExpect(jsonPath("$.customCompanyType").isEmpty());
            else response.andExpect(jsonPath("$.companyTypeId").value(other())).andExpect(jsonPath("$.customCompanyType").value(industry));
            assertThat(employers.findById(employer.getId()).orElseThrow().getIndustry()).isEqualTo(industry);
        }
    }
}
