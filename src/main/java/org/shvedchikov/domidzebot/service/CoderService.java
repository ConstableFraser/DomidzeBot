package org.shvedchikov.domidzebot.service;

import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class CoderService {
    @Autowired
    private CoderDecoder coderDecoder;

    protected Status encodeString(TelegramBotService telegramBotService, Update update) {
        var source = update.getMessage().getText();
        var result = CoderDecoder.encodeString(source);
        telegramBotService.sendMessage(update.getMessage().getChatId(), result);
        return Status.DEFAULT;
    }

    protected Status decodeString(TelegramBotService telegramBotService, Update update) {
        var source = update.getMessage().getText();
        var result = CoderDecoder.decodeString(source);
        telegramBotService.sendMessage(update.getMessage().getChatId(), result);
        return Status.DEFAULT;
    }

    protected Status encodePwd(TelegramBotService telegramBotService, Update update) {
        var source = update.getMessage().getText();
        String result;
        try {
            result = coderDecoder.encodePwd(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        telegramBotService.sendMessage(update.getMessage().getChatId(), result);
        return Status.DEFAULT;
    }

    protected Status decodePwd(TelegramBotService telegramBotService, Update update) {
        var source = update.getMessage().getText();
        String result;
        try {
            result = coderDecoder.decodePwd(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        telegramBotService.sendMessage(update.getMessage().getChatId(), result);
        return Status.DEFAULT;
    }
}
