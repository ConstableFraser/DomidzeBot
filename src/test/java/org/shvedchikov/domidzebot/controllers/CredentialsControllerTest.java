package org.shvedchikov.domidzebot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.Credential;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.service.CredentialService;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "file:/src/main/resources/application.properties")
public class CredentialsControllerTest {
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token;
    private static Credential testCredential;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private CoderDecoder coderDecoder;

    @BeforeAll
    public static void init(@Autowired ModelGenerator modelGenerator,
                            @Autowired CredentialRepository credentialRepository,
                            @Autowired CoderDecoder coderDecoder) {

        credentialRepository.deleteAll();

        token = jwt().jwt(builder -> builder.subject("bot@domidze.ru"));
        testCredential = Instancio.of(modelGenerator.getDomainCredential())
                .create();
        testCredential.setHouses(List.of());
        try {
            testCredential.setPassword(coderDecoder.encodePwd(testCredential.getPassword()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        credentialRepository.save(testCredential);
    }

    @AfterAll
    public static void cleanUp(@Autowired CredentialRepository credentialRepository) {
        credentialRepository.deleteAll();
    }

    @Test
    public void testShow() throws Exception {
        var request = get("/api/credentials/{id}", testCredential.getId()).with(jwt());
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("login").isEqualTo(testCredential.getLogin()),
                v -> {
                    try {
                        v.node("password").isEqualTo(coderDecoder.decodePwd(testCredential.getPassword()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    public void testNotFoundIdInShowMethod(@Autowired CredentialService credentialService) {
        final Throwable raisedException = catchThrowable(() -> credentialService.show(224020L));
        assertThat(raisedException).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testIndex(@Autowired CredentialRepository credentialRepository) throws Exception {
        var newCredential = new Credential();
        newCredential.setLogin("Volfmess");
        newCredential.setPassword(coderDecoder.encodePwd("921WW()@$@**"));
        credentialRepository.save(newCredential);

        var result = mockMvc.perform(get("/api/credentials").with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray().hasSize(2);

    }

    @Test
    public void testCreate(@Autowired CredentialRepository credentialRepository,
                           @Autowired ModelGenerator modelGenerator) throws Exception {
        var data = Instancio.of(modelGenerator.getDomainCredential()).create();

        var request = post("/api/credentials")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isCreated());

        var credential = credentialRepository.findByLogin(data.getLogin());

        assertThat(credential.isPresent()).isTrue();
        assertThat(credential.get().getLogin()).isEqualTo(data.getLogin());
        assertThat(credential.get().getPassword()).isNotEqualTo(data.getPassword());
    }

    @Test
    public void test1Update(@Autowired CredentialRepository credentialRepository) throws Exception {
        var data = new HashMap<>();
        data.put("login", "pinky-winky");

        var request = put("/api/credentials/" + testCredential.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        var credential = credentialRepository.findById(testCredential.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Credential not found"));
        assertThat(credential.getLogin()).isEqualTo(data.get("login"));
    }

    @Test
    public void test2Delete(@Autowired CredentialRepository credentialRepository) throws Exception {
        var id = testCredential.getId();
        var request = delete("/api/credentials/" + id)
                .with(token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertThat(credentialRepository.findById(id)).isNotPresent();
    }
}
