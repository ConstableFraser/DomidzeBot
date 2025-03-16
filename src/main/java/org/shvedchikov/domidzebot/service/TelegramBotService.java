package org.shvedchikov.domidzebot.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class TelegramBotService {
    private final Map<Status, Function<Update, Status>> mapFunc = new HashMap<>();
    private final SendMessage sendMessage = new SendMessage();
    private TelegramBot telegramBot;

    @Getter
    @Setter
    private Status status;

    protected enum Status {
        NAME,
        LASTNAME,
        EMAIL,
        HOUSENUMBER,
        DOMAIN,
        LOGIN,
        PASSWORD,
        FINISHEDREGISTER,
        ACCEPTUSER,
        REJECTUSER,
        DEFAULT
    }

    @Autowired
    private RegisterUserBotService registerUserBotService;

    private static final String START_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            Добро пожаловать! Телеграм бот помогает собственникам следить за событиями по бронированию.
            Для этого нужно:
            1. зарегистрироваться
            2. настроить уведомления

            /register -- регистрация владельца
            /monthly -- список броней на месяц вперёд
            /notify -- настройка автоуведомлений
            /help -- получить справку""";

    private static final String HELP_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            бот умеет делать:
            1) 2) 3) 4)

            контактная информация:
            sanswed@gmail.com
            @sanswed
            © by Alexander Shvedchikov""";

    @PostConstruct
    private void setTelegramService() {
        registerUserBotService.setTelegramBot(this);
        mapFunc.put(Status.NAME, registerUserBotService::getName);
        mapFunc.put(Status.LASTNAME, registerUserBotService::getLastName);
        mapFunc.put(Status.EMAIL, registerUserBotService::getEmail);
        mapFunc.put(Status.HOUSENUMBER, registerUserBotService::getHouse);
        mapFunc.put(Status.DOMAIN, registerUserBotService::getDomain);
        mapFunc.put(Status.LOGIN, registerUserBotService::getLogin);
        mapFunc.put(Status.PASSWORD, registerUserBotService::getPassword);
        mapFunc.put(Status.FINISHEDREGISTER, registerUserBotService::startCompleteRegister);
        mapFunc.put(Status.ACCEPTUSER, registerUserBotService::onAccept);
        mapFunc.put(Status.REJECTUSER, registerUserBotService::onReject);
    }

    public void setTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    protected void setFunc(Status key, Function<Update, Status> value) {
        mapFunc.put(key, value);
    }

    public void onStartActionDoing(Long chatId) {
        sendMessage(chatId, START_TEXT);
    }

    public void onHelpDoing(Long chatId) {
        sendMessage(chatId, HELP_TEXT);
    }

    public void onRegisterActionDoing(Long chatId) {
        status = Status.DEFAULT;
        registerUserBotService.welcomeToRegister(chatId);
    }

    public void onUnknownActionDoing(Update update) {
        if (status == Status.DEFAULT) {
            log.warn("Unknown command: " + update.getMessage().getText());
            sendMessage(update.getMessage().getChatId(), "нераспознанная команда");
            return;
        }
        status = mapFunc.getOrDefault(status, (e) -> {
                    log.warn(
                            this.getClass().getSimpleName() + ": not found the method during setting the property");
                    return Status.DEFAULT;
                }
        ).apply(update);
    }

    public void callBackQuery(Update update) {
        var id = update.getCallbackQuery().getData();
        status = Status.valueOf(id);

        status = mapFunc.getOrDefault(status, (e) -> {
                    log.warn(
                            this.getClass().getSimpleName() + ": not found method on CallbackQuery");
                    return Status.DEFAULT;
                }
        ).apply(update);
    }

    public void sendMessage(SendMessage sendMessage) {
        try {
            telegramBot.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(Long chatId, String messageText) {
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);
        sendMessage(sendMessage);
    }
}
