/*package org.shvedchikov.domidzebot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.DomainService;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class DomainsControllerTest {
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token;
    private static Domain testDomain;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @BeforeAll
    public static void init(@Autowired ModelGenerator modelGenerator,
                            @Autowired DomainRepository domainRepository) {

        domainRepository.deleteAll();

        token = jwt().jwt(builder -> builder.subject("bot@domidze.ru"));
        testDomain = Instancio.of(modelGenerator.getDomainModel())
                .create();
        testDomain.setHouses(List.of());
        domainRepository.save(testDomain);
    }

    @AfterAll
    public static void cleanUp(@Autowired UserRepository userRepository,
                               @Autowired HouseRepository houseRepository,
                               @Autowired DomainRepository domainRepository) {
        houseRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();
    }

    @Test
    public void testShow() throws Exception {
        var request = get("/api/domains/{id}", testDomain.getId()).with(jwt());
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("domain").isEqualTo(testDomain.getDomain())
        );
    }

    @Test
    public void testNotFoundIdInShowMethod(@Autowired DomainService domainService) {
        final Throwable raisedException = catchThrowable(() -> domainService.show(224020L));
        assertThat(raisedException).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testIndex(@Autowired DomainRepository domainRepository) throws Exception {
        var newDomain = new Domain();
        newDomain.setDomain("https://www.anything91330.org");
        domainRepository.save(newDomain);

        var result = mockMvc.perform(get("/api/domains").with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray().hasSize(2);
    }

    @Test
    public void testCreate(@Autowired DomainRepository domainRepository,
                            @Autowired ModelGenerator modelGenerator) throws Exception {
        var data = Instancio.of(modelGenerator.getDomainModel()).create();

        var request = post("/api/domains")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isCreated());

        var domain = domainRepository.findDomainByDomain(data.getDomain());

        assertNotNull(domain);
        assertThat(domain.getDomain()).isEqualTo(data.getDomain());
    }

    @Test
    public void test2Delete(@Autowired DomainRepository domainRepository) throws Exception {
        var id = testDomain.getId();
        var request = delete("/api/domains/" + id)
                .with(token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertThat(domainRepository.findById(id)).isNotPresent();
    }

    @Test
    public void test1Update(@Autowired DomainRepository domainRepository) throws Exception {
        var data = new HashMap<>();
        data.put("domain", "wwww.pinky-wiky.cow");

        var request = put("/api/domains/" + testDomain.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        var domain = domainRepository.findById(testDomain.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
        assertThat(domain.getDomain()).isEqualTo(data.get("domain"));
    }
}
*/
