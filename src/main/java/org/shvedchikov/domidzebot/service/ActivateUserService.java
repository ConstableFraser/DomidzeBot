package org.shvedchikov.domidzebot.service;

import static org.shvedchikov.domidzebot.service.TelegramBotService.Status;

import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@Slf4j
@Service
public class ActivateUserService {
    private TelegramBotService telegramBotService;
    //private Long idAdmin;
    private Long chatId;
    private Long tgId;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BotConfig botConfig;

    protected void getUserById(Update update) {
        tgId = update.getMessage().getFrom().getId();
        if (!tgId.equals(botConfig.getIdAdmin())) {
            log.warn("You are not a Admin. Id: " + tgId);
            return;
        }
        SendMessage sendMessage = new SendMessage();
        chatId = update.getMessage().getChatId();
        telegramBotService.setStatus(Status.ACTIVATE);
        sendMessage.setChatId(chatId);
        sendMessage.setText("->>");
        telegramBotService.sendMessage(sendMessage);
    }

    protected Status setUserById(Update update) {
        var userId = Long.valueOf(update.getMessage().getText());
        if (!tgId.equals(botConfig.getIdAdmin())) {
            log.warn("You are not a Admin. Id: " + tgId);
            return Status.DEFAULT;
        }
        SendMessage sendMessage = new SendMessage();
        var user = userRepository.findByUserTelegramId(userId);
        if (user.isEmpty()) {
            sendMessage.setChatId(chatId);
            sendMessage.setText("user not found");
            telegramBotService.sendMessage(sendMessage);
            return Status.DEFAULT;
        }

        user.get().setEnable(true);
        if (!userRepository.save(user.get()).isEnable()) {
            sendMessage.setChatId(chatId);
            sendMessage.setText("user wasn't activated");
            telegramBotService.sendMessage(sendMessage);
            return Status.DEFAULT;
        }

        sendMessage.setChatId(chatId);
        sendMessage.setText("[+] success");
        telegramBotService.sendMessage(sendMessage);
        return Status.DEFAULT;
    }

    protected void setTelegramBot(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
        //this.idAdmin = id;
    }
}
