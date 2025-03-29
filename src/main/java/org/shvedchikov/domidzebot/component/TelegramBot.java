package org.shvedchikov.domidzebot.component;

import jakarta.annotation.PostConstruct;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private TelegramBotService telegramBotService;

    private final BotConfig botConfig;

    public TelegramBot(BotConfig config) {
        super(config.getToken());
        botConfig = config;
        List<BotCommand> menuOfCommands = new ArrayList<>();
        menuOfCommands.add(new BotCommand("/start", "[запустить бота]"));
        menuOfCommands.add(new BotCommand("/register", "[зарегистрироваться]"));
        menuOfCommands.add(new BotCommand("/month", "[ближайшие брони]"));
        menuOfCommands.add(new BotCommand("/notify", "[автоуведомления]"));
        menuOfCommands.add(new BotCommand("/help", "[справка]"));
        menuOfCommands.add(new BotCommand("/whoami", "[кто я?]"));
        var myCommands = new SetMyCommands();
        myCommands.setCommands(menuOfCommands);

        try {
            execute(myCommands);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @PostConstruct
    private void setTelegramBot() {
        telegramBotService.setTelegramBot(this);
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var chatId = update.getMessage().getChatId();

            switch (update.getMessage().getText()) {
                case "/start":
                    telegramBotService.onStartActionDoing(chatId);
                    break;

                case "/register":
                    telegramBotService.onRegisterActionDoing(update);
                    break;

                case "/help":
                    telegramBotService.onHelpDoing(chatId);
                    break;

                case "/activate":
                    telegramBotService.onActiveUser(update);
                    break;

                case "/gethash":
                    telegramBotService.onGetHash(update);
                    break;

                case "/sethash":
                    telegramBotService.onSetHash(update);
                    break;

                case "/encodestring":
                    telegramBotService.onEncodeString(update);
                    break;

                case "/decodestring":
                    telegramBotService.onDecodeString(update);
                    break;

                case "/encodepwd":
                    telegramBotService.onEncodePwd(update);
                    break;

                case "/decodepwd":
                    telegramBotService.onDecodePwd(update);
                    break;

                default:
                    telegramBotService.onUnknownActionDoing(update);
                    break;
            }
        } else if (update.hasCallbackQuery()) {
            telegramBotService.callBackQuery(update);
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }
}
