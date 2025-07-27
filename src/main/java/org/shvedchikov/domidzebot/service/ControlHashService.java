package org.shvedchikov.domidzebot.service;

import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.util.Status;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class ControlHashService {
    @Autowired
    private BotConfig botConfig;

    protected void getHash(TelegramBotService telegramBotService, Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not a Admin. Id: " + idCurrent);
            return;
        }
        var hash = System.getProperty("DHASH", "null");
        hash = hash.equals("null") ? hash : CoderDecoder.decodeString(hash);
        telegramBotService.sendMessage(update.getMessage().getChatId(), "HASH: " + hash);
    }

    protected Status setHash(TelegramBotService telegramBotService, Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not a Admin. Id: " + idCurrent);
            return Status.DEFAULT;
        }
        var userText = update.getMessage().getText();
        System.setProperty("DHASH", CoderDecoder.encodeString(userText));
        telegramBotService.sendMessage(update.getMessage().getChatId(), "the hash is set");
        return Status.DEFAULT;
    }
}
