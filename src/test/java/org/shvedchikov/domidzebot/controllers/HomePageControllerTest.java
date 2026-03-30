package org.shvedchikov.domidzebot.controllers;

import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.BotInitializer;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Objects;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class HomePageControllerTest {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    @MockitoBean
    private BotInitializer botInitializer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BotConfig botConfig;


    @Test
    public void testHomePageController() throws Exception {
        int amountDays = LocalDate.MAX.lengthOfYear() * botConfig.getIndex();
        var request = get("/");
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();

        var model = Objects.requireNonNull(result.getModelAndView()).getModel();

        assertThat(model).containsKey("calendar");
        assertThat(model).containsKey("nextMonthDate");
        assertThat(model.get("nextMonthDate")).isEqualTo(LocalDate.now().plusMonths(1).format(DTF));

        var calendar = (LinkedList<LinkedHashMap<String, String>>) model.get("calendar");
        var firstElement = LocalDate.now().getDayOfWeek() == DayOfWeek.MONDAY
                ? LocalDate.now() : LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
        var lastElement = firstElement.plusDays(amountDays);

        assertThat(calendar.get(0).containsKey(firstElement.format(DTF))).isTrue();
        assertThat(calendar.get(0).containsKey(firstElement.minusDays(1).format(DTF))).isFalse();
        assertThat(calendar.get(calendar.size() - 1).containsKey(lastElement.format(DTF))).isTrue();
        assertThat(calendar.get(calendar.size() - 1).containsKey(lastElement.plusDays(1).format(DTF))).isFalse();
    }
}
