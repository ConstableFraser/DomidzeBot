package org.shvedchikov.domidzebot.component;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.service.BookingService;
import org.shvedchikov.domidzebot.service.CommonCalendarService;
import org.shvedchikov.domidzebot.service.TelegramBotService;
import org.shvedchikov.domidzebot.util.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private TelegramBotService telegramBotService;

    private final Map<Command, Consumer<Update>> mapFunc = new HashMap<>();
    private final BotConfig botConfig;

    @Autowired
    private CommonCalendarService commonCalendarService;

    @Autowired
    private BookingService bookingService;

    public TelegramBot(BotConfig config) {
        super(config.getToken());
        this.botConfig = config;
    }

    @PostConstruct
    private void init() {
        telegramBotService.setTelegramBot(this);
        mapFunc.put(Command.START, telegramBotService::onStartActionDoing);
        mapFunc.put(Command.REGISTER, telegramBotService::onRegisterActionDoing);
        mapFunc.put(Command.HELP, telegramBotService::onHelpDoing);
        mapFunc.put(Command.ACTIVATE, telegramBotService::onActiveUser);
        mapFunc.put(Command.GETHASH, telegramBotService::onGetHash);
        mapFunc.put(Command.SETHASH, telegramBotService::onSetHash);
        mapFunc.put(Command.ENCODESTRING, telegramBotService::onEncodeString);
        mapFunc.put(Command.DECODESTRING, telegramBotService::onDecodeString);
        mapFunc.put(Command.ENCODEPWD, telegramBotService::onEncodePwd);
        mapFunc.put(Command.DECODEPWD, telegramBotService::onDecodePwd);
        mapFunc.put(Command.PERIOD, telegramBotService::onSetPeriodByUser);
        mapFunc.put(Command.MONTHMINUS, telegramBotService::onGetPeriod);
        mapFunc.put(Command.MONTH, telegramBotService::onGetPeriod);
        mapFunc.put(Command.HALFYEAR, telegramBotService::onGetPeriod);
        mapFunc.put(Command.MONTHPREV, telegramBotService::onGetPeriod);
        mapFunc.put(Command.HALFYEARPREV, telegramBotService::onGetPeriod);
        mapFunc.put(Command.USERS, telegramBotService::onGetListUsers);
        mapFunc.put(Command.INIT, commonCalendarService::calculateCalendar);
        mapFunc.put(Command.BOOKING, bookingService::onSetBooking);
        mapFunc.put(Command.SETPRICE, commonCalendarService::onSetPrice);
        mapFunc.put(Command.UPDATE, commonCalendarService::update);
    }

    @Bean
    @ConditionalOnProperty(name = "bot.enabled", havingValue = "true")
    public SetMyCommands setCommands() {
        List<BotCommand> menuOfCommands = new ArrayList<>();
        menuOfCommands.add(new BotCommand("/" + Command.START.name().toLowerCase(), "[запустить бота]"));
        menuOfCommands.add(new BotCommand("/" + Command.REGISTER.name().toLowerCase(), "[зарегистрироваться]"));
        menuOfCommands.add(new BotCommand("/" + Command.MONTHMINUS.name().toLowerCase(), "[+1 месяц БЕЗ денег]"));
        menuOfCommands.add(new BotCommand("/" + Command.MONTH.name().toLowerCase(), "[+1 месяц]"));
        menuOfCommands.add(new BotCommand("/" + Command.HALFYEAR.name().toLowerCase(), "[+6 месяцев]"));
        menuOfCommands.add(new BotCommand("/" + Command.MONTHPREV.name().toLowerCase(), "[-1 месяц]"));
        menuOfCommands.add(new BotCommand("/" + Command.HALFYEARPREV.name().toLowerCase(), "[-6 месяцев]"));
        menuOfCommands.add(new BotCommand("/" + Command.PERIOD.name().toLowerCase(), "[указать вручную]"));
        menuOfCommands.add(new BotCommand("/" + Command.HELP.name().toLowerCase(), "[справка]"));
        var myCommands = new SetMyCommands();
        myCommands.setCommands(menuOfCommands);

        try {
            execute(myCommands);
            log.info("✅ Bot commands set successfully");
        } catch (TelegramApiException e) {
            log.warn("⚠️ Bot commands not set: {}", e.getMessage());
            log.debug("Full error:", e);
        }
        return myCommands;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var commandText = update.getMessage().getText();
            var command = Command.getCommand(commandText.replace("/", "").toUpperCase()).orElse(Command.NOTEXIST);
            mapFunc.getOrDefault(command, telegramBotService::onUnknownActionDoing).accept(update);
        } else if (update.hasCallbackQuery()) {
            telegramBotService.callBackQuery(update);
        } else if (update.getMessage().hasDocument() || update.getMessage().hasVideo()
                || update.getMessage().hasPhoto() || update.getMessage().hasAudio()
                || update.getMessage().hasSticker() || update.getMessage().hasVoice()) {
            log.warn("Received a command to send media/file/audio/sticker. User: {}",
                    update.getMessage().getFrom().getId());
        }
    }

    @Override
    public String getBotUsername() {
        return botConfig.getName();
    }
}
