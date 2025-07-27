package org.shvedchikov.domidzebot.telegram;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.service.UserService;
import org.shvedchikov.domidzebot.util.Command;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@AutoConfigureMockMvc
public class TelegramAdminCommandTest {
    @Autowired
    private BotConfig botConfig;

    @MockitoSpyBean
    private TelegramBotService telegramBotService;

    @MockitoSpyBean
    private TelegramBot telegramBot;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    private static final Long CHAT_ID = 100_001L;
    private static final int SUCCESS = 1;

    private Message message;
    private Update update;
    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        Chat chat = new Chat();
        message = new Message();
        update = new Update();

        user.setId(botConfig.getIdAdmin());
        chat.setId(CHAT_ID);
        message.setFrom(user);
        message.setChat(chat);
        update.setMessage(message);
    }

    @AfterEach
    public void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    public void testHash() {
        message.setText(String.valueOf(Command.SETHASH));
        update.setMessage(message);

        //test for prompt to sethash
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "->>");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).onSetHash(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "->>");

        //test for success setting of new hash
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "the hash is set");
        message.setText("TDFKg3lKg3FDR3V4OUJLeIM3Nks3OVs=");
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "the hash is set");

        //test for checking valid new hash
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "HASH: TDFKg3lKg3FDR3V4OUJLeIM3Nks3OVs=");
        message.setText(String.valueOf(Command.GETHASH));
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "HASH: TDFKg3lKg3FDR3V4OUJLeIM3Nks3OVs=");
        verify(telegramBot, times(3)).onUpdateReceived(update);
    }

    @Test
    public void testEncodeDecode() {
        var sourceString = "when i win wenna wenna win";
        var encodingString = "Tz07fnl5h043fodCOzA/cYdCOzA/cYdON1E=";
        var sourcePwd = "very strong password string!";
        var encodingPwd = "0kQEa8+5YN/w438OpaXQ6so4D2gRApku3HM379sjR5pc";

        message.setText(String.valueOf(Command.ENCODESTRING));
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "->>");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).onEncodeString(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "->>");
        message.setText(sourceString);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, encodingString);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, encodingString);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        message.setText(String.valueOf(Command.DECODESTRING));
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).onDecodeString(update);
        verify(telegramBotService, times(2)).sendMessage(CHAT_ID, "->>");
        message.setText(encodingString);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, sourceString);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, sourceString);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        message.setText(String.valueOf(Command.ENCODEPWD));
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).onEncodePwd(update);
        verify(telegramBotService, times(3)).sendMessage(CHAT_ID, "->>");
        message.setText(sourcePwd);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, encodingPwd);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, encodingPwd);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);

        message.setText(String.valueOf(Command.DECODEPWD));
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).onDecodePwd(update);
        verify(telegramBotService, times(4)).sendMessage(CHAT_ID, "->>");
        message.setText(encodingPwd);
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, sourcePwd);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, sourcePwd);
        assertThat(telegramBotService.getStatus()).isEqualTo(Status.DEFAULT);
    }

    @Test
    public void testActivateUser() {
        // test for display the prompt. No admin
        user.setId(101L);
        message.setFrom(user);
        message.setText(String.valueOf(Command.ACTIVATE));
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "->>");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(0)).sendMessage(CHAT_ID, "->>");
        // test for display the prompt. Admin
        user.setId(botConfig.getIdAdmin());
        message.setFrom(user);
        Long telegramId = 123_456L;
        message.setText(String.valueOf(Command.ACTIVATE));
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "->>");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "->>");

        // test for activate the user
        var data = new UserCreateDTO();
        data.setFirstName("User1");
        data.setLastName("Userovich");
        data.setEmail("data1@wgw3ag.com");
        data.setUserTelegramId(telegramId);
        data.setPassword("password");
        userService.create(data);
        // positive activate
        message.setText(String.valueOf(telegramId));
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "[+] success");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "[+] success");
        // negative activate, because empty user
        message.setText(String.valueOf(999L));
        update.setMessage(message);
        telegramBotService.setStatus(Status.ACTIVATE);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "user not found");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "user not found");
        // negative activate, because not an admin
        user.setId(101L);
        message.setFrom(user);
        update.setMessage(message);
        telegramBotService.setStatus(Status.ACTIVATE);
        message.setText(String.valueOf(telegramId));
        update.setMessage(message);
        var testUser = userRepository.findByUserTelegramId(telegramId).orElseThrow();
        testUser.setEnable(false);
        userRepository.save(testUser);
        telegramBot.onUpdateReceived(update);
        AtomicBoolean isEnable = new AtomicBoolean(false);
        userRepository.findByUserTelegramId(telegramId).
                ifPresentOrElse(u -> isEnable.set(u.isEnable()), () -> isEnable.set(false));
        assertThat(isEnable.get()).isFalse();
    }

    @Test
    public void testGetListUsers() {
        // test by no Admin
        user.setId(101L);
        message.setFrom(user);
        message.setText(String.valueOf(Command.USERS));
        update.setMessage(message);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "");
        telegramBotService.setStatus(Status.DEFAULT);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(0)).sendMessage(CHAT_ID, "");
        // test by Admin, empty list
        user.setId(botConfig.getIdAdmin());
        message.setFrom(user);
        message.setText(String.valueOf(Command.USERS));
        update.setMessage(message);
        telegramBotService.setStatus(Status.DEFAULT);
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, "");
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, "");
        // test by Admin, user list
        var data = new UserCreateDTO();
        data.setFirstName("Alina");
        data.setLastName("Cute");
        data.setEmail("alya@wgw3ag.com");
        data.setUserTelegramId(31313L);
        data.setPassword("password");
        userService.create(data);
        data.setFirstName("Jack");
        data.setLastName("Jordon");
        data.setEmail("JJ99@wg09sg.ru");
        data.setUserTelegramId(404040L);
        data.setPassword("password");
        userService.create(data);
        var listUsers = """
                alya@wgw3ag.com | Alina | Cute | 31313
                JJ99@wg09sg.ru | Jack | Jordon | 404040""";
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, listUsers);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, listUsers);
    }
}
