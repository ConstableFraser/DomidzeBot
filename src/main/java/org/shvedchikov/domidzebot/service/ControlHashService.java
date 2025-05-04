package org.shvedchikov.domidzebot.service;

import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.util.Status;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class ControlHashService {
    private TelegramBotService telegramBotService;

    @Autowired
    private BotConfig botConfig;

    protected void setTelegramBot(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    protected void getHash(Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (botConfig.isNoAdmin(idCurrent)) {
            log.warn("You are not a Admin. Id: " + idCurrent);
            return;
        }
        var hash = System.getProperty("DHASH", "null");
        hash = hash.equals("null") ? hash : CoderDecoder.decodeString(hash);
        var sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("HASH: " + hash);
        telegramBotService.sendMessage(sendMessage);
    }

    protected Status setHash(Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (botConfig.isNoAdmin(idCurrent)) {
            log.warn("You are not a Admin. Id: " + idCurrent);
            return Status.DEFAULT;
        }
        var userText = update.getMessage().getText();
        System.setProperty("DHASH", CoderDecoder.encodeString(userText));
        var sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("the hash is set");
        telegramBotService.sendMessage(sendMessage);
        return Status.DEFAULT;
    }
}
