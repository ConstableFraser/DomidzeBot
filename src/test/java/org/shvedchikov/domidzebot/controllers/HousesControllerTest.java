package org.shvedchikov.domidzebot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.Credential;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.model.House;
import org.shvedchikov.domidzebot.model.User;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.HouseService;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class HousesControllerTest {

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc mockMvc;
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token;
    private static User testUser;
    private static House testHouse;
    private static Domain testDomain;
    private static Credential testCredential;


    @BeforeAll
    public static void init(@Autowired UserRepository userRepository,
                            @Autowired ModelGenerator modelGenerator,
                            @Autowired HouseRepository houseRepository,
                            @Autowired DomainRepository domainRepository,
                            @Autowired CredentialRepository credentialRepository) {

        houseRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();

        token = jwt().jwt(builder -> builder.subject("bot@domidze.ru"));
        testUser = Instancio.of(modelGenerator.getUserModel())
                .create();
        testUser.setHouses(List.of());
        userRepository.save(testUser);

        testDomain = Instancio.of(modelGenerator.getDomainModel())
                .create();
        testDomain.setHouses(List.of());
        domainRepository.save(testDomain);

        testCredential = Instancio.of(modelGenerator.getDomainCredential())
                .create();
        testCredential.setHouses(List.of());
        credentialRepository.save(testCredential);

        testHouse = new House();
        testHouse.setNumber(new Random().nextInt(1, 1001));
        testHouse.setOwner(testUser);
        testHouse.setDomain(testDomain);
        testHouse.setCredential(testCredential);
        houseRepository.save(testHouse);
    }

    @AfterAll
    public static void cleanUp(@Autowired UserRepository userRepository,
                               @Autowired HouseRepository houseRepository,
                               @Autowired DomainRepository domainRepository,
                               @Autowired CredentialRepository credentialRepository) {
        houseRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();
        credentialRepository.deleteAll();
    }

    @Test
    public void testShow() throws Exception {
        var request = get("/api/houses/{id}", testHouse.getId()).with(jwt());
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("number").isEqualTo(testHouse.getNumber()),
                v -> v.node("owner_id").isEqualTo(testUser.getId()),
                v -> v.node("credential_id").isEqualTo(testCredential.getId())
        );
    }

    @Test
    public void test2NotFoundIdInShowMethod(@Autowired HouseService houseService) {
        final Throwable raisedException = catchThrowable(() -> houseService.show(224020L));
        assertThat(raisedException).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void test3Index(@Autowired HouseRepository houseRepository) throws Exception {
        var newHouse = new House();
        newHouse.setNumber(new Random().nextInt(1002));
        newHouse.setOwner(testUser);
        houseRepository.save(newHouse);

        var result = mockMvc.perform(get("/api/houses").with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray().hasSize(3);
    }

    @Test
    public void test4Create(@Autowired HouseRepository houseRepository,
                    @Autowired ModelGenerator modelGenerator) throws Exception {
        var data = Instancio.of(modelGenerator.getHouseModel()).create();
        data.setOwnerId(testUser.getId());
        data.setDomainId(testDomain.getId());
        data.setCredentialId(testCredential.getId());

        var request = post("/api/houses")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));
        mockMvc.perform(request)
                .andExpect(status().isCreated());

        var house = houseRepository.findHouseByOwnerIdAndNumber(data.getOwnerId(), data.getNumber())
                .orElseThrow(() -> new ResourceNotFoundException("House not found"));

        assertNotNull(house);
        assertThat(house.getOwner().getId()).isEqualTo(data.getOwnerId());
        assertThat(house.getNumber()).isEqualTo(data.getNumber());
    }

    @Test
    public void test5ShowByOwnerIdAndNumberHouse(@Autowired ModelGenerator modelGenerator) throws Exception {
        var data = Instancio.of(modelGenerator.getHouseModel()).create();
        data.setOwnerId(new User().getId());

        var result = mockMvc.perform(get("/api/houses/filter?owner=" + testUser.getId()).with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray().hasSize(1);

        result = mockMvc.perform(get("/api/houses/filter?owner=" + testUser.getId()
                        + "&humber=" + testHouse.getNumber()).with(jwt()))
                .andExpect(status().isOk())
                .andReturn();

        body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray().hasSize(1);
    }

    @Test
    public void test6Update(@Autowired HouseRepository houseRepository) throws Exception {
        var data = new HashMap<>();
        data.put("number", 99999);

        var request = put("/api/houses/" + testHouse.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request)
                .andExpect(status().isOk());

        var house = houseRepository.findById(testHouse.getId())
                .orElseThrow(() -> new ResourceNotFoundException("House not found"));
        assertThat(house.getNumber()).isEqualTo(data.get("number"));
        assertThat(house.getOwner().getId()).isEqualTo(testUser.getId());
    }

    @Test
    public void test7Delete(@Autowired HouseRepository houseRepository) throws Exception {
        var id = testHouse.getId();
        var request = delete("/api/houses/" + id)
                .with(token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertThat(houseRepository.findById(id)).isNotPresent();
    }
}
