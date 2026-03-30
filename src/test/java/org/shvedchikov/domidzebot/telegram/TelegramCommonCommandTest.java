package org.shvedchikov.domidzebot.telegram;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.BotInitializer;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.service.KeyboardBotService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.util.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class TelegramCommonCommandTest {
    @MockitoBean
    private BotInitializer botInitializer;

    @Autowired
    private KeyboardBotService keyboardBotService;

    @MockitoSpyBean
    private TelegramBotService telegramBotService;

    @MockitoSpyBean
    private TelegramBot telegramBot;

    private static final Long CHAT_ID = 100_001L;
    private static final Long USER_ID = 131_101L;
    private static final int SUCCESS = 1;
    private static final String START_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            Добро пожаловать! Телеграм бот помогает собственникам следить за событиями по бронированию.
            Чтобы начать пользоваться, пройдите регистрацию.
            После чего будут доступны полезные функции.

            /register - регистрация владельца
            /help - справка
            """;

    private static final String HELP_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            Доступные функции:
            🔹регистрация собственника
            🔹информирование о бронированиях

            /register - регистрация владельца
            /monthminus - на месяц вперёд без денег
            /month - на месяц вперёд (+деньги)
            /halfyear - на полгода вперёд (+деньги)
            /monthprev - на месяц назад (+деньги)
            /halfyearprev - на полгода назад (+деньги)

            Контактная информация:
            🔮 @sanswed
            """;

    private Message message;
    private Update update;

    @BeforeEach
    public void setUp() {
        User user = new User();
        Chat chat = new Chat();
        message = new Message();
        update = new Update();

        user.setId(USER_ID);
        chat.setId(CHAT_ID);
        message.setFrom(user);
        message.setChat(chat);
        update.setMessage(message);
    }

    @Test
    public void testCommandStart() {
        var sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(keyboardBotService.createMainKeyboard());
        sendMessage.setText(START_TEXT);
        sendMessage.setChatId(CHAT_ID);

        doReturn(SUCCESS).when(telegramBotService).sendMessage(sendMessage);
        message.setText(String.valueOf(Command.START));
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(sendMessage);
    }

    @Test
    public void testCommandHelp() {
        doReturn(SUCCESS).when(telegramBotService).sendMessage(CHAT_ID, HELP_TEXT);
        message.setText(String.valueOf(Command.HELP));
        update.setMessage(message);
        telegramBot.onUpdateReceived(update);
        verify(telegramBotService, times(1)).sendMessage(CHAT_ID, HELP_TEXT);
    }
}
