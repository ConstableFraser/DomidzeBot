package org.shvedchikov.domidzebot.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@PropertySource(value = "classpath:application.properties")
public class WelcomePageTest {
    @Autowired
    private MockMvc mockMvc;

    @Value("${spring.application.name}")
    private String name;

    @Value("${spring.application.version}")
    private String version;

    @Test
    public void testWelcomePage() throws Exception {
        var expected = "\uD83C\uDFE0 Welcome to "
                + name + "! \uD83C\uDFE0"
                + "<div><a href=\"https://www.t.me/" + name + "\">" + "@" + name + "</a></div>"
                + "<br><br>version: " + version;

        var request = mockMvc.perform(get("/welcome").with(jwt()))
                .andExpect(status().isOk())
                .andReturn();
        var body = request.getResponse().getContentAsString();
        assertThat(body).isEqualTo(expected);

        request = mockMvc.perform(get("/").with(jwt()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        assertThat("/welcome").isEqualTo(request.getResponse().getHeaders("Location").get(0));
    }
}
