package org.shvedchikov.domidzebot.component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Slf4j
@Component
@ConditionalOnProperty(
        name = "bot.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class BotInitializer {
    @Autowired
    private TelegramBot telegramBot;

    @EventListener(ApplicationReadyEvent.class)
    public void init() throws TelegramApiException {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBot);
            log.warn("✅ Telegram bot registered successfully");
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot", e);
        }
    }
}
