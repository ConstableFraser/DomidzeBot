package org.shvedchikov.domidzebot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.User;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.UserService;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
public class UsersControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private ModelGenerator modelGenerator;

    @Autowired
    private ObjectMapper om;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token;

    private User testUser;
    private User testUserForUpdateDelete;

    @BeforeEach
    public void setUp() {
        token = jwt().jwt(builder -> builder.subject("bot@domidze.ru"));
        testUser = Instancio.of(modelGenerator.getUserModel())
                .create();
        userRepository.save(testUser);

        testUserForUpdateDelete = new User();
        testUserForUpdateDelete.setFirstName("mail@mail.com");
        testUserForUpdateDelete.setEmail("mail@mail.com");
        testUserForUpdateDelete.setPasswordDigest("qwerty");
        userRepository.save(testUserForUpdateDelete);
    }

    @AfterEach
    public void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    public void testShow() throws Exception {
        var request = get("/api/users/{id}", testUser.getId()).with(jwt());
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("email").isEqualTo(testUser.getEmail()),
                v -> v.node("firstName").isEqualTo(testUser.getFirstName()),
                v -> v.node("lastName").isEqualTo(testUser.getLastName())
        );
    }

    @Test
    public void testNotFoundIdInShowMethod() {
        final Throwable raisedException = catchThrowable(() -> userService.show(224020L));
        assertThat(raisedException).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testIndex() throws Exception {
        var result = mockMvc.perform(get("/api/users").with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray();
    }

    @Test
    void testCreate() throws Exception {
        var data = Instancio.of(modelGenerator.getUserModel())
                .create();

        var request = post("/api/users")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail(data.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        assertNotNull(user);
        assertThat(user.getFirstName()).isEqualTo(data.getFirstName());
        assertThat(user.getLastName()).isEqualTo(data.getLastName());
    }

    @Test
    @WithMockUser(username = "mail@mail.com", password = "qwerty", roles = "USER")
    public void testUpdate() throws Exception {
        var data = new HashMap<>();
        data.put("firstName", "noname.user");

        var request = put("/api/users/" + testUserForUpdateDelete.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        var user = userRepository.findById(testUserForUpdateDelete.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        assertThat(user.getFirstName()).isEqualTo(data.get("firstName"));
        assertThat(user.getLastName()).isEqualTo(testUserForUpdateDelete.getLastName());
    }

    @Test
    @WithMockUser(username = "mail@mail.com", password = "qwerty", roles = "USER")
    public void testDelete() throws Exception {
        var id = testUserForUpdateDelete.getId();
        var request = delete("/api/users/" + testUserForUpdateDelete.getId())
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(id)).isNotPresent();
    }

    @Test
    public void testIndexWithoutAuth() throws Exception {
        userRepository.save(testUser);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

    }

    @Test
    public void testShowWithoutAuth() throws Exception {
        userRepository.save(testUser);
        var request = get("/api/users/{id}", testUser.getId());

        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }
}
