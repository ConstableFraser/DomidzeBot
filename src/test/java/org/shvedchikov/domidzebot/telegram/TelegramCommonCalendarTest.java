package org.shvedchikov.domidzebot.telegram;

import org.instancio.Instancio;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.BotInitializer;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.model.House;
import org.shvedchikov.domidzebot.repository.CredentialRepository;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.CommonCalendarService;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static util.Util.readFixture;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class TelegramCommonCalendarTest {
    private static final Long CHAT_ID = 100_001L;
    private static final Long USER_ID = 131_101L;
    private static final int SUCCESS = 1;
    private static final String ABSOLUTE_PATH = "src/test/resources/fixtures/";
    private static int amountDays;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String SUCCESS_TEXT = "Инициализация выполнена";
    private static String fixtureHtml = "";
    private static final String PROMPT_TEXT = """
                Установка брони с сайта. Укажите в формате:

                <дата заезда>|<программа>|<гостей>|<телефон>|<цена>|<номер дома>

                пример:
                12.01.2026|2203/26|5|+79991112223|2600|755
                13.01.2026|2203/26|5|+79991112223|1600|755""";

    private Message message;
    private Update update;
    private Long userId;

    @MockitoBean
    private BotInitializer botInitializer;

    @MockitoSpyBean
    private TelegramBotService telegramBotService;

    @MockitoSpyBean
    private TelegramBot telegramBot;

    @MockitoSpyBean
    private OrderService orderService;

    @Autowired
    private BotConfig botConfig;

    @Autowired
    private ModelGenerator modelGenerator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CommonCalendarService commonCalendarService;

    @BeforeAll
    public static void setUpClass() {
        fixtureHtml = readFixture(ABSOLUTE_PATH + "responseBody.html");
    }

    @BeforeEach
    public void setUp() {
        amountDays = LocalDate.MAX.lengthOfYear() * botConfig.getIndex();
        User tgUser = new User();
        Chat chat = new Chat();
        message = new Message();
        update = new Update();
        Long adminId = botConfig.getIdAdmin();

        tgUser.setId(adminId);
        chat.setId(CHAT_ID);
        message.setFrom(tgUser);
        message.setChat(chat);
        update.setMessage(message);

        var userModel = Instancio.of(modelGenerator.getUserModel()).create();
        userModel.setHouses(List.of());
        userModel.setUserTelegramId(adminId);
        userId = userRepository.save(userModel).getId();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        commonCalendarService.getCommonCalendar().clear();
    }

    @Test
    public void testCommandInitCalendarSuccess() {
        var calendar = commonCalendarService.getCommonCalendar();
        var begin = LocalDate.now().minusDays(amountDays);
        var end = LocalDate.now().plusDays(amountDays);

        init();

        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, SUCCESS_TEXT);
        assertThat(calendar.size()).isEqualTo(amountDays * 2 + 1);
        assertThat(calendar.containsKey(begin.format(dtf))).isTrue();
        assertThat(calendar.containsKey(begin.minusDays(1).format(dtf))).isFalse();
        assertThat(calendar.containsKey(end.format(dtf))).isTrue();
        assertThat(calendar.containsKey(end.plusDays(1).format(dtf))).isFalse();
        assertThat(commonCalendarService.getCountOfBookings(begin, end)).isEqualTo(7);
    }

    @Test
    public void testCommandSetPrice() {
        var promptText = """
                Установка стоимость брони. Одну или несколько.
                Укажите в формате:

                <дата>|<цена>

                пример:
                12.01.2026|26600
                12.02.2026|31200
                """;
        var correctPrice = """
                29.04.2026|1700
                01.06.2026|3200
                31.05.2026|2900
                01.12.2026|1700
                10.05.2026|11200
                02.05.2026|290000
                """;
        var successMessage = "Цены установлены";

        //test: initialize and test correct set price
        init();

        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, SUCCESS_TEXT);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, promptText);
        message.setText(Command.SETPRICE.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, promptText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETPRICE);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, successMessage);
        message.setText(correctPrice);
        update.setMessage(message);
        telegramBot.onUpdateReceived(update); //down test
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, successMessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        //test: incorrect date set price
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, promptText);
        message.setText(Command.SETPRICE.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        var incorrectData = """
                29.04.2026|1700
                23.01.2026|3200
                31.05.2026|2900
                """;
        message.setText(incorrectData);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService)
                .sendMessage(CHAT_ID, "Ошибка в данных: 23.01.2026|3200\nзапустите команду заново");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1))
                .sendMessage(CHAT_ID, "Ошибка в данных: 23.01.2026|3200\nзапустите команду заново");
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        //test: incorrect date set price
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, promptText);
        message.setText(Command.SETPRICE.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        incorrectData = """
                29.12.2036|1700
                23.11.2026|3200
                31.05.2026|2900
                """;
        message.setText(incorrectData);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService)
                .sendMessage(CHAT_ID, "В календаре нет даты: 29.12.2036|1700\nзапустите команду заново");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1))
                .sendMessage(CHAT_ID, "В календаре нет даты: 29.12.2036|1700\nзапустите команду заново");
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        //test: already exist booking
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, promptText);
        message.setText(Command.SETPRICE.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        incorrectData = """
                29.08.2026|1700
                25.07.2026|3200
                31.05.2026|2900
                """;
        message.setText(incorrectData);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService)
                .sendMessage(CHAT_ID, "На дату уже есть бронь: 25.07.2026|3200\nзапустите команду заново");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1))
                .sendMessage(CHAT_ID, "На дату уже есть бронь: 25.07.2026|3200\nзапустите команду заново");
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        //test: error in price
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, promptText);
        message.setText(Command.SETPRICE.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        incorrectData = """
                29.08.2026|wgwgr
                25.07.2026|3200
                31.05.2026|2900
                """;
        message.setText(incorrectData);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService)
                .sendMessage(CHAT_ID, "Ошибка в цене: 29.08.2026|wgwgr\nзапустите команду заново");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1))
                .sendMessage(CHAT_ID, "Ошибка в цене: 29.08.2026|wgwgr\nзапустите команду заново");
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    @Test
    public void testForCheckingSecurity() {
        var calendar = commonCalendarService.getCommonCalendar();
        User tgUser = new User();
        Chat chat = new Chat();
        message = new Message();
        update = new Update();
        tgUser.setId(USER_ID);
        chat.setId(CHAT_ID); // not admin
        message.setFrom(tgUser);
        message.setChat(chat);
        update.setMessage(message);

        init();
        assertThat(calendar.size()).isEqualTo(0);
    }

    @Test
    public void testSetBooking(@Autowired DomainRepository domainRepository,
                               @Autowired CredentialRepository credentialRepository,
                               @Autowired HouseRepository houseRepository) {
        var houseNumber = 755;

        init();

        var testUser = Instancio.of(modelGenerator.getUserModel())
                .create();
        testUser.setHouses(List.of());
        userRepository.save(testUser);

        var testDomain = Instancio.of(modelGenerator.getDomainModel())
                .create();
        testDomain.setHouses(List.of());
        domainRepository.save(testDomain);

        var testCredential = Instancio.of(modelGenerator.getDomainCredential())
                .create();
        testCredential.setHouses(List.of());
        credentialRepository.save(testCredential);

        var testHouse = new House();
        testHouse.setNumber(houseNumber);
        testHouse.setOwner(testUser);
        testHouse.setDomain(testDomain);
        testHouse.setCredential(testCredential);
        houseRepository.save(testHouse);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, PROMPT_TEXT);
        message.setText(Command.BOOKING.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, PROMPT_TEXT);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.SETBOOKING);

        // test success setting of booking
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Заказ сохранён успешно!");
        message.setText("""
        08.08.2026|12/26|6|+79991112223|1200|755|
        09.08.2026|12/26|6|+79991112223|1600|755|
        19.08.2026|11/26|2|+79991125200|2600|755|""");
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(3)).sendMessage(CHAT_ID, "Заказ сохранён успешно!");
        assertThat(commonCalendarService.getCommonCalendar().get("08.08.2026").get("channel")).isEqualTo("site");
    }

    @Test
    public void testSetBookingWrongDate(@Autowired DomainRepository domainRepository,
                                        @Autowired CredentialRepository credentialRepository,
                                        @Autowired HouseRepository houseRepository) {
        var houseNumber = 755;

        init();

        var testUser = Instancio.of(modelGenerator.getUserModel())
                .create();
        testUser.setHouses(List.of());
        userRepository.save(testUser);

        var testDomain = Instancio.of(modelGenerator.getDomainModel())
                .create();
        testDomain.setHouses(List.of());
        domainRepository.save(testDomain);

        var testCredential = Instancio.of(modelGenerator.getDomainCredential())
                .create();
        testCredential.setHouses(List.of());
        credentialRepository.save(testCredential);

        var testHouse = new House();
        testHouse.setNumber(houseNumber);
        testHouse.setOwner(testUser);
        testHouse.setDomain(testDomain);
        testHouse.setCredential(testCredential);
        houseRepository.save(testHouse);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, PROMPT_TEXT);
        message.setText(Command.BOOKING.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);

        // test message warning of setting of booking
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "05.07.2026 - дата уже занята");
        message.setText("05.07.2026|232/26|6|+79991112223|166000|755");
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "05.07.2026 - дата уже занята");
    }

    public void init() {
        var begin = LocalDate.now().minusDays(amountDays);
        var end = LocalDate.now().plusDays(amountDays);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, SUCCESS_TEXT);
        doReturn(fixtureHtml).when(orderService).connectedToDomain(userId, begin, end);

        message.setText(Command.INIT.toString());
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
    }
}
