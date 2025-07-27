package org.shvedchikov.domidzebot.service;

import org.shvedchikov.domidzebot.util.Status;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class ActivateUserService {
    private Long chatId;
    private Long tgId;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BotConfig botConfig;

    protected void getUserById(TelegramBotService telegramBotService, Update update) {
        tgId = update.getMessage().getFrom().getId();
        if (!tgId.equals(botConfig.getIdAdmin())) {
            log.warn("You are not a Admin. Id: " + tgId);
            return;
        }
        chatId = update.getMessage().getChatId();
        telegramBotService.setStatus(Status.ACTIVATE);
        telegramBotService.sendMessage(chatId, "->>");
    }

    protected Status setUserById(TelegramBotService telegramBotService, Update update) {
        tgId = update.getMessage().getFrom().getId();
        if (!tgId.equals(botConfig.getIdAdmin())) {
            log.warn("You are not a Admin. Id: " + tgId);
            return Status.DEFAULT;
        }
        var userId = Long.valueOf(update.getMessage().getText());
        var user = userRepository.findByUserTelegramId(userId);
        if (user.isEmpty()) {
            telegramBotService.sendMessage(chatId, "user not found");
            return Status.DEFAULT;
        }
        user.get().setEnable(true);
        if (!userRepository.save(user.get()).isEnable()) {
            telegramBotService.sendMessage(chatId, "user wasn't activated");
            return Status.DEFAULT;
        }
        telegramBotService.sendMessage(chatId, "[+] success");
        return Status.DEFAULT;
    }
}
