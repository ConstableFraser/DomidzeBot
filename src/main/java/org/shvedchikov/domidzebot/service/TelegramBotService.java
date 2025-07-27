package org.shvedchikov.domidzebot.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.util.Command;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.shvedchikov.domidzebot.util.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBotService {
    private final Map<Status, BiFunction<TelegramBotService, Update, Status>> mapFunc = new HashMap<>();
    private TelegramBot telegramBot;

    @Getter
    @Setter
    private Status status;

    @Autowired
    private RegisterUserBotService registerUserBotService;

    @Autowired
    private ActivateUserService activateUserService;

    @Autowired
    private ControlHashService controlHashService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyboardBotService keyboardBotService;

    @Autowired
    private CoderService coderService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private BotConfig botConfig;

    private static final String START_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            Добро пожаловать! Телеграм бот помогает собственникам следить за событиями по бронированию.
            Чтобы начать пользоваться, пройдите регистрацию.
            После чего будут доступны полезные функции.

            /register - регистрация владельца
            /help - справка
            """;

    private static final String HELP_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            Доступные функции:
            🔹регистрация собственника
            🔹информирование о бронированиях

            /register - регистрация владельца
            /monthminus - на месяц вперёд без денег
            /month - на месяц вперёд (+деньги)
            /halfyear - на полгода вперёд (+деньги)
            /monthprev - на месяц назад (+деньги)
            /halfyearprev - на полгода назад (+деньги)

            Контактная информация:
            🔮 @sanswed
            """;

    @PostConstruct
    private void setTelegramService() {
        mapFunc.put(Status.FINISHEDREGISTER, registerUserBotService::startCompleteRegister);
        mapFunc.put(Status.LASTNAME, registerUserBotService::getLastName);
        mapFunc.put(Status.HOUSENUMBER, registerUserBotService::getHouse);
        mapFunc.put(Status.PASSWORD, registerUserBotService::getPassword);
        mapFunc.put(Status.ETHNOMIR, registerUserBotService::setEthnomir);
        mapFunc.put(Status.ACCEPTUSER, registerUserBotService::onAccept);
        mapFunc.put(Status.REJECTUSER, registerUserBotService::onReject);
        mapFunc.put(Status.ACTIVATE, activateUserService::setUserById);
        mapFunc.put(Status.DOMAIN, registerUserBotService::getDomain);
        mapFunc.put(Status.ENCODESTRING, coderService::encodeString);
        mapFunc.put(Status.DECODESTRING, coderService::decodeString);
        mapFunc.put(Status.LOGIN, registerUserBotService::getLogin);
        mapFunc.put(Status.EMAIL, registerUserBotService::getEmail);
        mapFunc.put(Status.NAME, registerUserBotService::getName);
        mapFunc.put(Status.ENCODEPWD, coderService::encodePwd);
        mapFunc.put(Status.DECODEPWD, coderService::decodePwd);
        mapFunc.put(Status.HASH, controlHashService::setHash);
        mapFunc.put(Status.SETPERIOD, orderService::getDates);
    }

    private void prompt(Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not an Admin. Id: {}", idCurrent);
            return;
        }
        sendMessage(update.getMessage().getChatId(), "->>");
    }

    public void onGetListUsers(Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (!botConfig.isAdmin(idCurrent)) {
            log.warn("You are not an Admin. Id: {}", idCurrent);
            return;
        }
        var users = userRepository.findAll().stream()
                .map(user -> String.join(" | ",
                        user.getUsername(),
                        user.getFirstName(),
                        user.getLastName(),
                        String.valueOf(user.getUserTelegramId())))
                .collect(Collectors.joining("\n"));
        sendMessage(update.getMessage().getChatId(), users);
    }

    public void onSetPeriodByUser(Update update) {
        orderService.getDates(this, update);
    }

    public void onGetPeriod(Update update) {
        var user = userRepository.findByUserTelegramId(update.getMessage().getFrom().getId());
        if (user.isEmpty() || !user.get().isEnabled()) {
            log.warn("Attempt to request period: {}", update.getMessage().getFrom().getId());
            sendMessage(update.getMessage().getChatId(), "Требуется регистрация");
            return;
        }
        var command = update.getMessage().getText().replace("/", "").toUpperCase();
        var startDate = LocalDate.now();
        var countMonth = command.contains("MONTH") ? 1 : 6;
        var endDate = startDate.plusMonths(countMonth);

        if (List.of(Command.MONTHPREV, Command.HALFYEARPREV).contains(Command.valueOf(command))) {
            endDate = LocalDate.now();
            startDate = endDate.minusMonths(countMonth);
        }
        var withPrice = !Command.MONTHMINUS.equals(Command.valueOf(command));
        sendMessage(update.getMessage().getChatId(), orderService.getOrders(user.get(), startDate, endDate, withPrice));
    }

    public void onSetHash(Update update) {
        status = Status.HASH;
        prompt(update);
    }

    public void onGetHash(Update update) {
        controlHashService.getHash(this, update);
    }

    public void onEncodeString(Update update) {
        status = Status.ENCODESTRING;
        prompt(update);
    }

    public void onDecodeString(Update update) {
        status = Status.DECODESTRING;
        prompt(update);
    }

    public void onEncodePwd(Update update) {
        status = Status.ENCODEPWD;
        prompt(update);
    }

    public void onDecodePwd(Update update) {
        status = Status.DECODEPWD;
        prompt(update);
    }

    public void onActiveUser(Update update) {
        status = Status.ACTIVATE;
        activateUserService.getUserById(this, update);
    }

    public void setTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    protected void setFunc(Status key, BiFunction<TelegramBotService, Update, Status> value) {
        mapFunc.put(key, value);
    }

    public void onStartActionDoing(Update update) {
        var sendMessage = new SendMessage();
        sendMessage.setReplyMarkup(keyboardBotService.createMainKeyboard());
        sendMessage.setText(START_TEXT);
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage(sendMessage);
    }

    public void onHelpDoing(Update update) {
        sendMessage(update.getMessage().getChatId(), HELP_TEXT);
    }

    public void onRegisterActionDoing(Update update) {
        status = Status.DEFAULT;
        registerUserBotService.welcomeToRegister(this, update);
    }

    public void onUnknownActionDoing(Update update) {
        if (status == Status.DEFAULT) {
            log.warn("Unknown command: {}", update.getMessage().getText());
            sendMessage(update.getMessage().getChatId(), "нераспознанная команда");
            return;
        }
        status = mapFunc.getOrDefault(status, (t, e) -> {
            log.warn("{}: not found the method during setting the property", this.getClass().getSimpleName());
                    return Status.DEFAULT;
                }
        ).apply(this, update);
    }

    public void callBackQuery(Update update) {
        var id = update.getCallbackQuery().getData();
        status = Status.valueOf(id);

        this.status = mapFunc.getOrDefault(status, (t, e) -> {
            log.warn("{}: not found method on CallbackQuery", this.getClass().getSimpleName());
                    return Status.DEFAULT;
                }
        ).apply(this, update);
    }

    public int sendMessage(SendMessage sendMessage) {
        try {
            return telegramBot.execute(sendMessage).getMessageId();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public int sendMessage(Long chatId, String messageText) {
        var sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);
        return sendMessage(sendMessage);
    }

    public void sendMessage(EditMessageText editMessageText) {
        try {
            telegramBot.execute(editMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(DeleteMessage deleteMessageText) {
        try {
            telegramBot.execute(deleteMessageText);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
}
