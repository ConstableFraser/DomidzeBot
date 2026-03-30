package org.shvedchikov.domidzebot.controllers;

import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.BotInitializer;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.dto.booking.BookingCreateDTO;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.repository.BookingRepository;
import org.shvedchikov.domidzebot.service.BookingService;
import org.shvedchikov.domidzebot.service.CommonCalendarService;
import org.shvedchikov.domidzebot.service.HouseService;
import org.shvedchikov.domidzebot.service.OrderService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.util.Command;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.mockito.Mockito.doReturn;

import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.Chat;

import org.shvedchikov.domidzebot.service.UserService;

@SpringBootTest
@AutoConfigureMockMvc
class RequestBookingControllerTest {
    private static final Long CHAT_ID = 100_001L;
    private static final int SUCCESS = 1;
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private String stringMessage;
    private Integer AMOUNT_DAYS;

    @MockitoBean
    private BotInitializer botInitializer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ModelGenerator modelGenerator;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HouseRepository houseRepository;

    @MockitoSpyBean
    private TelegramBotService telegramBotService;

    @MockitoSpyBean
    private TelegramBot telegramBot;

    @MockitoSpyBean
    OrderService orderService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private CommonCalendarService commonCalendarService;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    DomainRepository domainRepository;

    @Autowired
    CredentialRepository credentialRepository;

    @Autowired
    BookingRepository bookingRepository;

    @Autowired
    HouseService houseService;

    @BeforeEach
    void setUp() {
        AMOUNT_DAYS = LocalDate.MAX.lengthOfYear() * botConfig.getIndex();
        System.setProperty(
                "DHASH",
                "djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTEwUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=");

        var userCreateDTO = new UserCreateDTO();
        userCreateDTO.setFirstName("Alekhandro");
        userCreateDTO.setLastName("Vivianto");
        userCreateDTO.setEmail("valid@email.com");
        userCreateDTO.setUserTelegramId(botConfig.getIdAdmin());
        userCreateDTO.setChatId(CHAT_ID);
        userCreateDTO.setPassword("_2wrg0923WOGIOWR(#@_@!");
        userCreateDTO.setEnable(true);
        userService.create(userCreateDTO);

        assertThat(userRepository.findByUserTelegramId(botConfig.getIdAdmin()).orElseThrow()).isNotNull();

        stringMessage = """
                ⭐️ пришла бронь с сайта ⭐️

                ЗАЕЗД  |  ВЫЕЗД  | ГОСТЕЙ | ЦЕНА | ТЕЛЕФОН
                --------------------------------------------------------------
                """;
    }

    @AfterEach
    void tearDown() {
        commonCalendarService.getCommonCalendar().clear();
        bookingRepository.deleteAll();
        houseRepository.deleteAll();
        domainRepository.deleteAll();
        credentialRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testRequestForBookingWhereDatesIsOccupied() throws Exception {
        var domainModel = Instancio.of(modelGenerator.getDomainModel()).create();
        var credentialModel = Instancio.of(modelGenerator.getDomainCredential()).create();
        var houseModel = Instancio.of(modelGenerator.getHouseModel()).create();
        var bookingModel = Instancio.of(modelGenerator.getBookingModel()).create();
        var requestBookingDTO = Instancio.of(modelGenerator.getBookingModel()).create();
        String NO_ORDERS = "Информация о бронях отсутствует";

        domainModel.setHouses(List.of());
        var domain = domainRepository.save(domainModel);

        credentialModel.setHouses(List.of());
        var credential = credentialRepository.save(credentialModel);

        houseModel.setDomainId(domain.getId());
        houseModel.setCredentialId(credential.getId());
        var userId = userRepository.findByUserTelegramId(botConfig.getIdAdmin()).orElseThrow().getId();
        houseModel.setOwnerId(userId);
        var house = houseService.create(houseModel);

        bookingModel.setCheckIn(requestBookingDTO.getCheckIn());
        bookingModel.setHouseId(house.getId());
        bookingService.create(bookingModel);

        stringMessage += buildMessage(requestBookingDTO);
        var update = new Update();
        var message = new Message();
        var chat = new Chat();
        var user = new User();

        user.setId(botConfig.getIdAdmin());
        message.setFrom(user);
        chat.setId(CHAT_ID);
        message.setChat(chat);
        update.setMessage(message);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Инициализация выполнена");
        doReturn(NO_ORDERS).when(orderService).getOrders(userId,
                LocalDate.now().minusDays(AMOUNT_DAYS),
                LocalDate.now().plusDays(AMOUNT_DAYS));
        commonCalendarService.calculateCalendar(update);

        var request = post("/api/booking")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("checkIn", requestBookingDTO.getCheckIn().toString())
                .param("checkOut", requestBookingDTO.getCheckOut().toString())
                .param("price", requestBookingDTO.getPrice().toString())
                .param("countGuests", requestBookingDTO.getCountGuests().toString())
                .param("mobileNumber", requestBookingDTO.getMobileNumber())
                .param("program", requestBookingDTO.getProgram());

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, stringMessage);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#booking"));

        verify(telegramBotService, times(0)).sendMessage(CHAT_ID, stringMessage);
    }

    @Test
    public void testRequestForBookingWhereWrongTelephoneNumber() throws Exception {
        var requestBookingDTO = Instancio.of(modelGenerator.getBookingModel()).create();
        requestBookingDTO.setMobileNumber("12345567");
        stringMessage += buildMessage(requestBookingDTO);

        var request = post("/api/booking")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("checkIn", requestBookingDTO.getCheckIn().toString())
                .param("checkOut", requestBookingDTO.getCheckOut().toString())
                .param("price", requestBookingDTO.getPrice().toString())
                .param("countGuests", requestBookingDTO.getCountGuests().toString())
                .param("mobileNumber", requestBookingDTO.getMobileNumber())
                .param("program", requestBookingDTO.getProgram());

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, stringMessage);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#booking"));

        verify(telegramBotService, times(0)).sendMessage(CHAT_ID, stringMessage);
    }

    @Test
    public void testRequestForBookingWhereWrongDates() throws Exception {
        var requestBookingDTO = Instancio.of(modelGenerator.getBookingModel()).create();
        requestBookingDTO.setCheckOut(LocalDate.now().minusDays(1));
        stringMessage += buildMessage(requestBookingDTO);

        var request = post("/api/booking")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("checkIn", requestBookingDTO.getCheckIn().toString())
                .param("checkOut", requestBookingDTO.getCheckOut().toString())
                .param("price", requestBookingDTO.getPrice().toString())
                .param("countGuests", requestBookingDTO.getCountGuests().toString())
                .param("mobileNumber", requestBookingDTO.getMobileNumber())
                .param("program", requestBookingDTO.getProgram());

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, stringMessage);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#booking"));

        verify(telegramBotService, times(0)).sendMessage(CHAT_ID, stringMessage);
    }

    @Test
    public void testRequestForBookingSuccess() throws Exception {
        var requestBookingDTO = Instancio.of(modelGenerator.getBookingModel()).create();
        String NO_ORDERS = "Информация о бронях отсутствует";
        stringMessage = """
                ⭐️ пришла бронь с сайта ⭐️

                ЗАЕЗД  |  ВЫЕЗД  | ГОСТЕЙ | ЦЕНА | ТЕЛЕФОН
                --------------------------------------------------------------
                """;
        stringMessage += buildMessage(requestBookingDTO);

        var update = new Update();
        var message = new Message();
        var chat = new Chat();
        var user = new User();

        user.setId(botConfig.getIdAdmin());
        message.setFrom(user);
        chat.setId(CHAT_ID);
        message.setChat(chat);
        update.setMessage(message);

        var userId = userRepository.findByUserTelegramId(botConfig.getIdAdmin()).orElseThrow().getId();

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Инициализация выполнена");
        doReturn(NO_ORDERS).when(orderService).getOrders(userId,
                LocalDate.now().minusDays(AMOUNT_DAYS),
                LocalDate.now().plusDays(AMOUNT_DAYS));
        commonCalendarService.calculateCalendar(update);

        var PROMPT_TEXT = """
                Установка стоимость брони. Одну или несколько.
                Укажите в формате:

                <дата>|<цена>

                пример:
                12.01.2026|26600
                12.02.2026|31200
                """;
        var CORRECT_PRICE = String.join(
                "\n",
                LocalDate.now().plusDays(1).format(DTF) + "|" + requestBookingDTO.getPrice(),
                LocalDate.now().plusDays(2).format(DTF) + "|" + requestBookingDTO.getPrice()
        );
        var SUCCESS_MESSAGE = "Цены установлены";

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, PROMPT_TEXT);
        message.setText(Command.SETPRICE.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, PROMPT_TEXT);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETPRICE);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, SUCCESS_MESSAGE);
        message.setText(CORRECT_PRICE);
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, SUCCESS_MESSAGE);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        var request = post("/api/booking")
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("checkIn", requestBookingDTO.getCheckIn().toString())
                .param("checkOut", requestBookingDTO.getCheckOut().toString())
                .param("price", requestBookingDTO.getPrice().toString())
                .param("countGuests", requestBookingDTO.getCountGuests().toString())
                .param("mobileNumber", requestBookingDTO.getMobileNumber())
                .param("program", requestBookingDTO.getProgram());

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, stringMessage);

        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/#booking"));

        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, stringMessage);
    }

    public String buildMessage(BookingCreateDTO bookingCreateDTO) {
        return bookingCreateDTO.getCheckIn().format(DTF)
                + " | "
                + bookingCreateDTO.getCheckOut().format(DTF)
                + " | "
                + bookingCreateDTO.getCountGuests()
                + " | "
                + bookingCreateDTO.getPrice()
                + " | "
                + bookingCreateDTO.getMobileNumber();
    }
}
