package org.shvedchikov.domidzebot.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.instancio.Instancio;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.BotInitializer;
import org.shvedchikov.domidzebot.dto.booking.BookingDTO;
import org.shvedchikov.domidzebot.exception.ResourceNotFoundException;
import org.shvedchikov.domidzebot.model.Booking;
import org.shvedchikov.domidzebot.model.Credential;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.model.House;
import org.shvedchikov.domidzebot.model.User;
import org.shvedchikov.domidzebot.repository.BookingRepository;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.BookingService;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
public class BookingsControllerTest {
    @MockitoBean
    private BotInitializer botInitializer;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MockMvc mockMvc;
    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor token;
    private static House testHouse;
    private static Booking testBooking;

    @BeforeAll
    public static void init(@Autowired UserRepository userRepository,
                            @Autowired ModelGenerator modelGenerator,
                            @Autowired HouseRepository houseRepository,
                            @Autowired DomainRepository domainRepository,
                            @Autowired CredentialRepository credentialRepository,
                            @Autowired BookingRepository bookingRepository) {
        User testUser;
        Domain testDomain;
        Credential testCredential;

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

        testBooking = new Booking();
        testBooking.setCheckIn(LocalDate.now());
        testBooking.setCheckOut(LocalDate.now().plusDays(2));
        testBooking.setCountGuests(5L);
        testBooking.setPrice(56400.00);
        testBooking.setMobileNumber("9019330101");
        testBooking.setHouse(testHouse);
        testBooking.setProgram("2203/26");
        bookingRepository.save(testBooking);
    }

    @AfterAll
    public static void cleanUp(@Autowired UserRepository userRepository,
                               @Autowired HouseRepository houseRepository,
                               @Autowired DomainRepository domainRepository,
                               @Autowired CredentialRepository credentialRepository,
                               @Autowired BookingRepository bookingRepository) {
        bookingRepository.deleteAll();
        houseRepository.deleteAll();
        userRepository.deleteAll();
        domainRepository.deleteAll();
        credentialRepository.deleteAll();
    }

    @Test
    public void testShow() throws Exception {
        var request = get("/api/bookings/{id}", testBooking.getId()).with(token);
        var result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn();
        var body = result.getResponse().getContentAsString();

        assertThatJson(body).and(
                v -> v.node("countGuests").isEqualTo(testBooking.getCountGuests()),
                v -> v.node("mobileNumber").asString().isEqualTo(testBooking.getMobileNumber()),
                v -> v.node("checkIn").isEqualTo(testBooking.getCheckIn()),
                v -> v.node("checkOut").isEqualTo(testBooking.getCheckOut()),
                v -> v.node("price").isEqualTo(testBooking.getPrice()),
                v -> v.node("program").isEqualTo(testBooking.getProgram())
        );
    }

    @Test
    public void testNotFoundIdInShowMethod(@Autowired BookingService bookingService) {
        final Throwable raisedException = catchThrowable(() -> bookingService.show(224020L));
        assertThat(raisedException).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    public void testIndex(@Autowired BookingRepository bookingRepository) throws Exception {
        final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        var booking = new Booking();
        booking.setCheckIn(LocalDate.now().plusDays(1));
        booking.setCheckOut(LocalDate.now().plusDays(3));
        booking.setCountGuests(5L);
        booking.setPrice(56400.00);
        booking.setMobileNumber("9019330101");
        booking.setHouse(testHouse);
        booking.setProgram("2203/26");
        bookingRepository.save(booking);

        var result = mockMvc.perform(get("/api/bookings").with(token)
                        .param("checkInAfter", LocalDate.parse(LocalDate.now().minusYears(1).format(DTF), DTF).format(DTF))
                        .param("checkInBefore", LocalDate.parse(LocalDate.now().plusYears(1).format(DTF), DTF).format(DTF))
                )
                .andExpect(status().isOk())
                .andReturn();

        var body = result.getResponse().getContentAsString();
        assertThatJson(body).isArray().hasSize(2);
    }

    @Test
    public void testCreate(@Autowired ModelGenerator modelGenerator) throws Exception {
        var createdBookingDTO = Instancio.of(modelGenerator.getBookingModel()).create();
        createdBookingDTO.setHouseId(testHouse.getId());

        var request = post("/api/bookings")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(createdBookingDTO));
        var result = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();

        var body = result.getResponse().getContentAsString();
        var createdBooking = om.readValue(body, BookingDTO.class);

        assertThatJson(body).and(
                v -> v.node("houseId").isNotNull(),
                v -> v.node("checkIn").isEqualTo(createdBooking.getCheckIn()),
                v -> v.node("checkOut").isEqualTo(createdBooking.getCheckOut()),
                v -> v.node("price").isEqualTo(createdBooking.getPrice()),
                v -> v.node("mobileNumber").isEqualTo(createdBooking.getMobileNumber()),
                v -> v.node("countGuests").isEqualTo(createdBooking.getCountGuests()),
                v -> v.node("houseId").isNotNull()
        );
    }

    @Test
    public void testUpdate(@Autowired BookingRepository bookingRepository,
                           @Autowired ModelGenerator modelGenerator) throws Exception {
        var data = new HashMap<>();
        data.put("mobileNumber", "89677477777");

        var createdBookingModel = Instancio.of(modelGenerator.getBookingModel()).create();
        createdBookingModel.setHouseId(testHouse.getId());

        var request = post("/api/bookings")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(createdBookingModel));
        var result = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();

        var body = result.getResponse().getContentAsString();
        var createdBookingDTO = om.readValue(body, BookingDTO.class);

        request = put("/api/bookings/" + createdBookingDTO.getId())
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isOk());

        var booking = bookingRepository.findById(createdBookingDTO.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        assertThat(booking.getMobileNumber()).isEqualTo(data.get("mobileNumber"));
        assertThat(booking.getId()).isEqualTo(createdBookingDTO.getId());
    }

    @Test
    public void testDelete(@Autowired ModelGenerator modelGenerator,
                           @Autowired BookingRepository bookingRepository) throws Exception {
        var createdBookingDTO = Instancio.of(modelGenerator.getBookingModel()).create();
        createdBookingDTO.setHouseId(testHouse.getId());

        var request = post("/api/bookings")
                .with(token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(createdBookingDTO));
        var result = mockMvc.perform(request).andExpect(status().isCreated()).andReturn();

        var body = result.getResponse().getContentAsString();
        var createdBooking = om.readValue(body, BookingDTO.class);

        var id  = createdBooking.getId();
        request = delete("/api/bookings/" + id)
                .with(token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(request)
                .andExpect(status().isNoContent());

        assertThat(bookingRepository.findById(id)).isNotPresent();
    }
}
