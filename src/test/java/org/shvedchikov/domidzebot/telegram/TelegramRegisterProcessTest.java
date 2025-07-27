package org.shvedchikov.domidzebot.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.KeyboardBotService;
import org.shvedchikov.domidzebot.service.RegisterUserBotService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.service.UserService;
import org.shvedchikov.domidzebot.util.Command;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
public class TelegramRegisterProcessTest {
    private static final String REG_TEXT = """
            [СТАРТ РЕГИСТРАЦИИ]

            Для регистрации пожалуйста укажите:
             - имя, фамилию и почту
             - номер дома
             - личный кабинет
             - реквизиты доступа к личному кабинету""";

    private final List<Map<String, String>> buttonsRegister = List.of(
            new LinkedHashMap<>() {
                {
                    put("1. имя", Status.NAME.name());
                    put("2. фамилия", Status.LASTNAME.name());
                    put("3. email", Status.EMAIL.name());
                }
            },
            new LinkedHashMap<>() {
                {
                    put("4. номер дома", Status.HOUSENUMBER.name());
                    put("5. сайт", Status.DOMAIN.name());
                }
            },
            new LinkedHashMap<>() {
                {
                    put("6. логин", Status.LOGIN.name());
                    put("7. пароль", Status.PASSWORD.name());
                }
            },
            new LinkedHashMap<>() {
                {
                    put("[завершить регистрацию]", Status.FINISHEDREGISTER.name());
                }
            }
    );

    private final List<Map<String, String>> buttonsFinished = List.of(
            new LinkedHashMap<>() {
                {
                    put("ДА", Status.ACCEPTUSER.name());
                    put("НЕТ", Status.REJECTUSER.name());
                }
            }
    );

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private KeyboardBotService keyboardBotService;

    @MockitoSpyBean
    private TelegramBotService telegramBotService;

    @MockitoSpyBean
    private TelegramBot telegramBot;

    @MockitoSpyBean
    private RegisterUserBotService registerUserBotService;

    private static final Long CHAT_ID = 100_001L;
    private static final Long USER_ID = 131_101L;
    private static final Long DUPLICATE_ID = 101L;
    private static final int SUCCESS = 1;

    private Message message;
    private CallbackQuery callbackQuery;
    private Update update;
    private User user;

    private final String name = "Alekhandro";
    private final String lastname = "Vivianto";
    private final String email = "valid@email.com";
    private final String login = "validolina_av";
    private final String password = "_2wrg0923WOGIOWR(#@_@!";
    private final int house = 755;

    private EditMessageText editMessageText = new  EditMessageText();
    private DeleteMessage deleteMessage = new DeleteMessage();

    @BeforeEach
    public void setUp() {
        Chat chat = new Chat();
        user = new  User();
        message = new Message();
        callbackQuery = new CallbackQuery();
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
    public void testRegisterUser() {
        testForUserAlreadyExist();
        testForWelcomeRegisterProcess();
        testForDisplayPromptGettingName();
        testForSettingName();
        testForDisplayPromptGettingLastName();
        testForSettingLastName();
        testForDisplayPromptGettingEmail();
        testForSettingWrongEmail();
        testForSettingValidEmail();
        testForDisplayHouseNumber();
        testForSettingWrongHouseNumber();
        testForSettingValidNumberHouse();
        testForDisplayPromptGettingDomain();
        testForSettingDomain();
        testForDisplayPromptGettingLogin();
        testForSettingValidLogin();
        testForDisplayPromptGettingPassword();
        testForSettingValidPassword();
        testForDisplayFinishingProcessRegister();
        testForAcceptanceFinishingProcessRegisterUserAlreadyExists();
        testForAcceptanceFinishingProcessRegisterUserReject();
        testForAcceptanceFinishingProcessRegisterUserSuccess();
    }

    private void testForUserAlreadyExist() {
        user.setId(DUPLICATE_ID);
        message.setFrom(user);
        message.setText(String.valueOf(Command.REGISTER));
        update.setMessage(message);

        var data = new UserCreateDTO();
        data.setFirstName("Alina");
        data.setLastName("Cute");
        data.setEmail("alya@wgw3ag.com");
        data.setUserTelegramId(DUPLICATE_ID);
        data.setPassword("password");
        userService.create(data);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "Вы уже зарегистрированы");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "Вы уже зарегистрированы");

    }

    private void testForWelcomeRegisterProcess() {
        user.setId(USER_ID);
        message.setFrom(user);
        update.setMessage(message);
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(CHAT_ID);
        sendMessage.setText(REG_TEXT);
        sendMessage.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doReturn(SUCCESS).when(telegramBotService).sendMessage(sendMessage);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(sendMessage);
    }

    private void testForDisplayPromptGettingName() {
        callbackQuery.setData(String.valueOf(Status.NAME));
        update.setMessage(null);
        update.setCallbackQuery(callbackQuery);

        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 1 из 7. Укажите имя: ");
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.NAME);
    }

    private void testForSettingName() {
        message.setText(name);
        update.setMessage(message);
        editMessageText.setText("Имя сохранено. Нажмите \"фамилия\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayPromptGettingLastName() {
        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.LASTNAME));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 2 из 7. Введите фамилию: ");
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.LASTNAME);
    }

    private void testForSettingLastName() {
        user.setId(USER_ID);
        message.setFrom(user);
        message.setText(lastname);
        update.setMessage(message);
        editMessageText.setText("Фамилия сохранена. Нажмите \"3. email\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(2)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayPromptGettingEmail() {
        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.EMAIL));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 3 из 7. Введите Email: ");
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.EMAIL);
    }

    private void testForSettingWrongEmail() {
        message.setText("wrongemail@");
        message.setMessageId(1);
        update.setMessage(message);
        deleteMessage.setChatId(CHAT_ID);
        deleteMessage.setMessageId(update.getMessage().getMessageId());

        doNothing().when(telegramBotService).sendMessage(deleteMessage);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(deleteMessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.EMAIL);
    }

    private void testForSettingValidEmail() {
        message.setText(email);
        update.setMessage(message);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setMessageId(1);
        editMessageText.setText("Email сохранён. Нажмите \"4. номер дома\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(3)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayHouseNumber() {
        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.HOUSENUMBER));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 4 из 7. Введите номер Дома (только цифры): ");
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.HOUSENUMBER);


    }

    private void testForSettingWrongHouseNumber() {
        message.setText("error number");
        update.setMessage(message);
        deleteMessage.setChatId(CHAT_ID);
        deleteMessage.setMessageId(update.getMessage().getMessageId());

        doNothing().when(telegramBotService).sendMessage(deleteMessage);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(2)).sendMessage(deleteMessage);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.HOUSENUMBER);
    }

    private void testForSettingValidNumberHouse() {
        message.setText(String.valueOf(house));
        update.setMessage(message);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setMessageId(1);
        editMessageText.setText("Номер дома сохранён. Нажмите \"5. сайт\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(4)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayPromptGettingDomain() {
        callbackQuery.setData(String.valueOf(Status.DOMAIN));
        callbackQuery.setMessage(message);
        update.setMessage(null);
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 5 из 7. Где ваш личный кабинет?");
        editMessageText.setMessageId(1);
        var domains = domainRepository.findAll();
        var domainList = domains.stream()
                .map(d -> d.getDomain().replaceAll("\\..*", "").toUpperCase().toUpperCase())
                .toList();

        final List<Map<String, String>> buttonsDomains = new ArrayList<>();
        for (String domain : domainList) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put(domain, domain);
            buttonsDomains.add(map);
        }
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsDomains));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(5)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DOMAIN);
    }

    private void testForSettingDomain() {
        callbackQuery.setFrom(user);
        callbackQuery.setData(String.valueOf(Status.ETHNOMIR));
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setMessageId(1);
        editMessageText.setText("Выбранный сайт сохранён. Нажмите \"6. логин\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(6)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayPromptGettingLogin() {
        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.LOGIN));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 6 из 7. Введите Логин: ");
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.LOGIN);
    }

    private void testForSettingValidLogin() {
        message.setText(login);
        update.setMessage(message);
        editMessageText = new EditMessageText();
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setMessageId(1);
        editMessageText.setText("Логин сохранён. Нажмите \"7. пароль\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(7)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayPromptGettingPassword() {
        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.PASSWORD));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("Шаг 7 из 7. Введите Пароль: ");
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.PASSWORD);
    }

    private void testForSettingValidPassword() {
        message.setText(password);
        update.setMessage(message);
        editMessageText = new EditMessageText();
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setMessageId(1);
        editMessageText.setText("Пароль сохранён. Нажмите \"завершить регистрацию\"");
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(8)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForDisplayFinishingProcessRegister() {
        String domainName = "ethnomir.ru";
        String userInfo = String.format(
                "имя: %s\nфамилия: %s\nпочта: %s\nномер дома: %d\nсайт: %s\nлогин: %s\nпароль: %s",
                name, lastname, email, house, domainName, login, password);

        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.FINISHEDREGISTER));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText("данные корректны?\n\n" + userInfo);
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsFinished));

        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(9)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.FINISHEDREGISTER);
    }

    private void testForAcceptanceFinishingProcessRegisterUserAlreadyExists() {
        var textAlreadyExist = "Вы уже зарегистрированы";
        user.setId(DUPLICATE_ID);
        message.setFrom(user);
        message.setText(null);
        update.setMessage(message);
        callbackQuery.setData(String.valueOf(Status.ACCEPTUSER));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText(textAlreadyExist);
        editMessageText.setMessageId(1);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());
        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(10)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForAcceptanceFinishingProcessRegisterUserReject() {
        user.setId(USER_ID);
        update.setMessage(null);
        callbackQuery.setData(String.valueOf(Status.REJECTUSER));
        update.setCallbackQuery(callbackQuery);
        deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(CHAT_ID);
        deleteMessage.setMessageId(1);
        doNothing().when(telegramBotService).sendMessage(deleteMessage);
        doNothing().when(registerUserBotService).welcomeToRegister(telegramBotService, update);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(3)).sendMessage(deleteMessage);
        verify(registerUserBotService, times(3)).welcomeToRegister(telegramBotService, update);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    private void testForAcceptanceFinishingProcessRegisterUserSuccess() {
        var textSuccess = String.format("""
                    ✅ профиль успешно создан ✅
                    сообщите администратору
                    этот номер: %s""", USER_ID);
        user.setId(USER_ID);
        update.setMessage(null);
        System.setProperty(
                "DHASH",
                "djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTEwUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=");
        callbackQuery.setData(String.valueOf(Status.ACCEPTUSER));
        update.setCallbackQuery(callbackQuery);
        editMessageText.setChatId(CHAT_ID);
        editMessageText.setText(textSuccess);
        editMessageText.setMessageId(1);
        doNothing().when(telegramBotService).sendMessage(editMessageText);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(11)).sendMessage(editMessageText);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
        var user = userRepository.findByUserTelegramId(USER_ID).orElseThrow();
        assertThat(user.getUserTelegramId()).isEqualTo(USER_ID);
    }
}
