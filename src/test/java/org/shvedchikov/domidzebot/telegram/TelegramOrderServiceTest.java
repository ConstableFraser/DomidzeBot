package org.shvedchikov.domidzebot.telegram;

import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.BotInitializer;
import org.shvedchikov.domidzebot.component.RestRequestSender;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.domain.DomainCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.repository.BookingRepository;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.CommonCalendarService;
import org.shvedchikov.domidzebot.service.CredentialService;
import org.shvedchikov.domidzebot.service.DomainService;
import org.shvedchikov.domidzebot.service.HouseService;
import org.shvedchikov.domidzebot.service.OrderService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.util.Command;
import org.shvedchikov.domidzebot.util.ModelGenerator;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static util.Util.readFixture;

@SpringBootTest
public class TelegramOrderServiceTest {
    private static final String ABSOLUTE_PATH = "src/test/resources/fixtures/";
    private static final Long CHAT_ID = 100_001L;
    private static final int SUCCESS = 1;
    private static int AMOUNT_DAYS;
    private static final String SUCCESS_TEXT = "Инициализация выполнена";
    private Message message;
    private Update update;
    private Long userId;

    private static String fixtureHtml;

    @MockitoBean
    private BotInitializer botInitializer;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @MockitoSpyBean
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

    @MockitoSpyBean
    private OrderService orderService;

    @Autowired
    private ModelGenerator modelGenerator;
    @Autowired
    private CommonCalendarService commonCalendarService;

    @BeforeAll
    public static void setUpClass() {
        fixtureHtml = readFixture(ABSOLUTE_PATH + "responseBody.html");
    }

    @BeforeEach
    public void setUp() {
        AMOUNT_DAYS = LocalDate.MAX.lengthOfYear() * botConfig.getIndex();
        User tgUser = new User();
        Chat chat = new Chat();
        message = new Message();
        update = new Update();
        Long ADMIN_ID = botConfig.getIdAdmin();

        tgUser.setId(ADMIN_ID);
        chat.setId(CHAT_ID);
        message.setFrom(tgUser);
        message.setChat(chat);
        update.setMessage(message);

        var userModel = Instancio.of(modelGenerator.getUserModel()).create();
        userModel.setHouses(List.of());
        userModel.setUserTelegramId(ADMIN_ID);
        userModel.setEnable(false);
        userId = userRepository.save(userModel).getId();
    }

    @AfterEach
    public void cleanUp() {
        userRepository.deleteAll();
        domainRepository.deleteAll();
        credentialRepository.deleteAll();
        bookingRepository.deleteAll();
        houseRepository.deleteAll();
        commonCalendarService.getCommonCalendar().clear();
    }

    @Test
    public void testForCheckingBookingMonth() {
        var user = new  User();
        user.setId(botConfig.getIdAdmin());
        message.setFrom(user);
        message.setText(String.valueOf(Command.MONTH));
        update.setMessage(message);
        System.setProperty(
                "DHASH",
                "djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTEwUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=");

        var domainDTO = new DomainCreateDTO();
        domainDTO.setDomain("domain.ololo");
        var domain = domainService.create(domainDTO);

        var credDTO = new CredentialCreateDTO();
        credDTO.setPassword("_2(*(@&*wrkgjwlkgjwgwrgwg");
        String login = "validolina";
        credDTO.setLogin(login);
        var credential = credentialService.create(credDTO);

        var testUser = userRepository.findByUserTelegramId(botConfig.getIdAdmin()).orElseThrow();
        var house = new HouseCreateDTO();
        house.setNumber(755);
        house.setOwnerId(testUser.getId());
        house.setCredentialId(credential.getId());
        house.setDomainId(domain.getId());
        houseService.create(house);

        var textError = "Требуется регистрация";
        String resultMessage = """
                04.07.2026|06.07.2026|||19037/25|1500|ethnomir|
                05.07.2026|06.07.2026|||19037/25|3000|ethnomir|
                17.07.2026|19.07.2026|||11211/25|2500|ethnomir|
                18.07.2026|19.07.2026|||11211/25|1000|ethnomir|
                19.07.2026|20.07.2026|||11281/25|2100|ethnomir|
                25.07.2026|27.07.2026|||1146/25|2300|ethnomir|
                26.07.2026|27.07.2026|||1146/25|2100|ethnomir|

                Кол-во броней этномира: 4
                Кол-во броней сайта: 0
                Сумма (этномир): 14500.0
                Сумма (сайт): 0.0""";

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, textError);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, textError);

        testUser.setEnable(true);
        userRepository.save(testUser);

        init();

        //test for report Ethnomir only
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, SUCCESS_TEXT);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, resultMessage);
        message.setText(Command.HALFYEAR.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, resultMessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        //test for report with Ethnomir and Site
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Заказ сохранён успешно!");
        message.setText("29.07.2026|2203/26|6|+79991112223|6300|755");
        update.setMessage(message);
        telegramBotService.setStatus(Status.SETBOOKING);
        telegramBot.onUpdateReceived(update);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Заказ сохранён успешно!");
        message.setText("30.07.2026|2203/26|16|+79931152223|3300|755");
        telegramBotService.setStatus(Status.SETBOOKING);
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);

        resultMessage = """
                04.07.2026|06.07.2026|||19037/25|1500|ethnomir|
                05.07.2026|06.07.2026|||19037/25|3000|ethnomir|
                17.07.2026|19.07.2026|||11211/25|2500|ethnomir|
                18.07.2026|19.07.2026|||11211/25|1000|ethnomir|
                19.07.2026|20.07.2026|||11281/25|2100|ethnomir|
                25.07.2026|27.07.2026|||1146/25|2300|ethnomir|
                26.07.2026|27.07.2026|||1146/25|2100|ethnomir|
                29.07.2026|30.07.2026|6|+79991112223|2203/26|6300.0|site|
                30.07.2026|31.07.2026|16|+79931152223|2203/26|3300.0|site|

                Кол-во броней этномира: 4
                Кол-во броней сайта: 1
                Сумма (этномир): 14500.0
                Сумма (сайт): 9600.0""";

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, resultMessage);
        message.setText(Command.HALFYEAR.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, resultMessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    @Test
    public void testGetReportOfBookingForPeriod() {
        var user = new  User();
        user.setId(botConfig.getIdAdmin());
        message.setFrom(user);
        message.setText(String.valueOf(Command.MONTH));
        update.setMessage(message);
        System.setProperty(
                "DHASH",
                "djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTEwUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=");

        var domainDTO = new DomainCreateDTO();
        domainDTO.setDomain("domain.ololo");
        var domain = domainService.create(domainDTO);

        var credDTO = new CredentialCreateDTO();
        credDTO.setPassword("_2(*(@&*wrkgjwlkgjwgwrgwg");
        String login = "validolina";
        credDTO.setLogin(login);
        var credential = credentialService.create(credDTO);

        var testUser = userRepository.findByUserTelegramId(botConfig.getIdAdmin()).orElseThrow();
        var house = new HouseCreateDTO();
        house.setNumber(755);
        house.setOwnerId(testUser.getId());
        house.setCredentialId(credential.getId());
        house.setDomainId(domain.getId());
        houseService.create(house);

        var textError = "Требуется регистрация";
        var textPrompt = "Укажите даты через дефис, например: 02.04.2025-02.05.2025";
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, textError);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, textError);

        testUser.setEnable(true);
        userRepository.save(testUser);

        init();

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Заказ сохранён успешно!");
        message.setText("29.07.2026|2203/26|6|+79991112223|6300|755");
        update.setMessage(message);
        telegramBotService.setStatus(Status.SETBOOKING);
        telegramBot.onUpdateReceived(update);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Заказ сохранён успешно!");
        message.setText("01.08.2026|2209/26|16|+79931152223|3300|755");
        telegramBotService.setStatus(Status.SETBOOKING);
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);

        var resultMessage = """
                25.07.2026|27.07.2026|||1146/25|2300|ethnomir|
                26.07.2026|27.07.2026|||1146/25|2100|ethnomir|
                29.07.2026|30.07.2026|6|+79991112223|2203/26|6300.0|site|
                01.08.2026|02.08.2026|16|+79931152223|2209/26|3300.0|site|

                Кол-во броней этномира: 1
                Кол-во броней сайта: 2
                Сумма (этномир): 4400.0
                Сумма (сайт): 9600.0""";

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, textPrompt);
        message.setText(Command.PERIOD.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, textPrompt);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETPERIOD);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, resultMessage);
        message.setText("25.07.2026-10.08.2026");
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, resultMessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    public void init() {
        var begin = LocalDate.now().minusDays(AMOUNT_DAYS);
        var end = LocalDate.now().plusDays(AMOUNT_DAYS);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, SUCCESS_TEXT);
        doReturn(fixtureHtml).when(orderService).connectedToDomain(userId, begin, end);

        message.setText(Command.INIT.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
    }
}
