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
    private TelegramBotService telegramBotService;

    @Autowired
    private CoderDecoder coderDecoder;

    protected void setTelegramBot(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    protected Status encodeString(Update update) {
        var source = update.getMessage().getText();
        var result = CoderDecoder.encodeString(source);
        telegramBotService.sendMessage(update.getMessage().getChatId(), result);
        return Status.DEFAULT;
    }

    protected Status decodeString(Update update) {
        var source = update.getMessage().getText();
        var result = CoderDecoder.decodeString(source);
        telegramBotService.sendMessage(update.getMessage().getChatId(), result);
        return Status.DEFAULT;
    }

    protected Status encodePwd(Update update) {
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

    protected Status decodePwd(Update update) {
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
