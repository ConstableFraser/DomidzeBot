package org.shvedchikov.domidzebot.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.RestRequestSender;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.CredentialService;
import org.shvedchikov.domidzebot.service.DomainService;
import org.shvedchikov.domidzebot.service.HouseService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.service.UserService;
import org.shvedchikov.domidzebot.util.Command;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static util.Util.readFixture;

@SpringBootTest
@AutoConfigureMockMvc
public class TelegramOrderServiceTest {
    private static final String ABSOLUTE_PATH = "src/test/resources/fixtures/";
    private static final Long CHAT_ID = 100_001L;
    private static final Long USER_ID = 131_101L;
    private static final int SUCCESS = 1;

    private Message message;
    private Update update;
    private User user;

    private String fixtureHtml;
    private String resultMessage;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TelegramBot telegramBot;

    @Autowired
    private HouseService houseService;

    @MockitoSpyBean
    private TelegramBotService telegramBotService;

    @MockitoSpyBean
    private RestRequestSender restRequestSender;

    @Autowired
    private DomainService domainService;

    @Autowired
    private CredentialService credentialService;

    @BeforeEach
    public void setUp() {
        Chat chat = new Chat();
        user = new User();
        message = new Message();
        CallbackQuery callbackQuery = new CallbackQuery();
        update = new Update();

        user.setId(USER_ID);
        chat.setId(CHAT_ID);
        message.setChat(chat);
        callbackQuery.setFrom(user);
        callbackQuery.setMessage(message);
        update.setCallbackQuery(callbackQuery);
    }

    @AfterEach
    public void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    public void testForCheckingQueryBooking() {
        testForCheckingBookingMonth();
        testForCheckingBookingPeriodByUser();
    }

    private void testForCheckingBookingMonth() {
        user.setId(USER_ID);
        message.setFrom(user);
        message.setText(String.valueOf(Command.MONTH));
        update.setMessage(message);
        System.setProperty(
                "DHASH",
                "djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTEwUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=");

        var data = new UserCreateDTO();
        String name = "Alekhandro";
        data.setFirstName(name);
        String lastname = "Vivianto";
        data.setLastName(lastname);
        String email = "valid@email.com";
        data.setEmail(email);
        data.setUserTelegramId(USER_ID);
        String password = "_2wrg0923WOGIOWR(#@_@!";
        data.setPassword(password);
        var userDTO = userService.create(data);
        assertThat(userRepository.findByUserTelegramId(USER_ID).orElseThrow()).isNotNull();

        var domainDTO = new DomainCreateDTO();
        domainDTO.setDomain("domain.ololo");
        var domain = domainService.create(domainDTO);

        var credDTO = new CredentialCreateDTO();
        credDTO.setPassword(password);
        String login = "validolina";
        credDTO.setLogin(login);
        var credential = credentialService.create(credDTO);

        var house = new HouseCreateDTO();
        house.setNumber(755);
        house.setOwnerId(userDTO.getId());
        house.setCredentialId(credential.getId());
        house.setDomainId(domain.getId());
        houseService.create(house);

        var textError = "Требуется регистрация";
        resultMessage = """
                1 | 04.07.2025 | 19037/25 | 1 500 |\s
                2 | 05.07.2025 | 19037/25 | 3 000 |\s
                3 | 17.07.2025 | 11211/25 | 2 500 |\s
                4 | 18.07.2025 | 11211/25 | 1 000 |\s
                5 | 19.07.2025 | 11281/25 | 2 100 |\s
                6 | 25.07.2025 | 1146/25 | 2 300 |\s
                7 | 26.07.2025 | 1146/25 | 2 100 |\s

                Итого: | 14 500 руб. | Броней: 4""";

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, textError);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, textError);

        var testUser = userRepository.findById(userDTO.getId()).orElseThrow();
        testUser.setEnable(true);
        userRepository.save(testUser);

        fixtureHtml = readFixture(ABSOLUTE_PATH + "responseBody.html");
        String host = "https://ethnomir.ru/personal/owner/";
        restRequestSender.setHost(host);
        restRequestSender.setHeaders(login, password, LocalDate.now(), LocalDate.now().plusMonths(1));
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, resultMessage);
        doReturn(fixtureHtml).when(restRequestSender).sendRequest();
        telegramBot.onUpdateReceived(update);
        verify(restRequestSender, times(1)).sendRequest();
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, resultMessage);
    }

    private void testForCheckingBookingPeriodByUser() {
        final String errormessage = "В датах допущена ошибка.\nПопробуйте ещё раз";
        message.setText(String.valueOf(Command.PERIOD));
        update.setMessage(message);
        var prompt = "Укажите даты через дефис, например: 02.04.2025-02.05.2025";
        var periodWithError1 = "02.04.2025 02.05.2025";
        var periodWithError2 = "33.04.2025-02.05.2025";
        var validPeriod = "02.01.2025-02.02.2025";

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, prompt);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, prompt);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETPERIOD);

        message.setText(periodWithError1);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, errormessage);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, errormessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETPERIOD);

        message.setText(periodWithError2);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, errormessage);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(2)).sendMessage(CHAT_ID, errormessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETPERIOD);

        message.setText(validPeriod);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, resultMessage);
        doReturn(fixtureHtml).when(restRequestSender).sendRequest();
        telegramBot.onUpdateReceived(update);
        verify(restRequestSender, times(2)).sendRequest();
        verify(telegramBotService, times(2)).sendMessage(CHAT_ID, resultMessage);
    }
}
