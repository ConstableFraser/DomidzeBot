package org.shvedchikov.domidzebot.service;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.component.TelegramBot;
import org.shvedchikov.domidzebot.config.BotConfig;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.function.Function;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TelegramBotService {
    private final Map<Status, Function<Update, Status>> mapFunc = new HashMap<>();
    private final SendMessage sendMessage = new SendMessage();
    private TelegramBot telegramBot;

    @Getter
    @Setter
    private Status status;

    protected enum Status {
        NAME,
        LASTNAME,
        EMAIL,
        HOUSENUMBER,
        DOMAIN,
        LOGIN,
        PASSWORD,
        FINISHEDREGISTER,
        ACCEPTUSER,
        REJECTUSER,
        ETHNOMIR,
        BNOVO,
        ACTIVATE,
        HASH,
        ENCODESTRING,
        DECODESTRING,
        ENCODEPWD,
        DECODEPWD,
        SETPERIOD,
        DEFAULT
    }

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
            Для этого нужно:
            1. зарегистрироваться
            2. настроить уведомления

            /register - регистрация владельца
            /monthminus - на месяц вперёд без денег
            /month - на месяц вперёд (+деньги)
            /halfyear - на полгода вперёд (+деньги)
            /monthprev - на месяц назад (+деньги)
            /halfyearprev - на полгода назад (+деньги)
            /help - справка""";

    private static final String HELP_TEXT = """
            [СЕРВИС УВЕДОМЛЕНИЙ О БРОНИРОВАНИЯХ]

            Доступные функции:
            🔹регистрация собственника
            🔹информирование о бронированиях

            Контактная информация:
            🔮 @sanswed
            """;

    @PostConstruct
    private void setTelegramService() {
        registerUserBotService.setTelegramBot(this);
        activateUserService.setTelegramBot(this);
        controlHashService.setTelegramBot(this);
        coderService.setTelegramBot(this);
        orderService.setTelegramBot(this);
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
        mapFunc.put(Status.BNOVO, registerUserBotService::setBnovo);
        mapFunc.put(Status.NAME, registerUserBotService::getName);
        mapFunc.put(Status.ENCODEPWD, coderService::encodePwd);
        mapFunc.put(Status.DECODEPWD, coderService::decodePwd);
        mapFunc.put(Status.HASH, controlHashService::setHash);
        mapFunc.put(Status.SETPERIOD, orderService::getDates);
    }

    private void prompt(Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (botConfig.isNoAdmin(idCurrent)) {
            log.warn("You are not an Admin. Id: " + idCurrent);
            return;
        }
        var sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText("->>");
        sendMessage(sendMessage);
    }

    public void onGetListUsers(Update update) {
        var idCurrent = update.getMessage().getFrom().getId();
        if (botConfig.isNoAdmin(idCurrent)) {
            log.warn("You are not an Admin. Id: " + idCurrent);
            return;
        }
        var users = userRepository.findAll().stream()
                .map(user -> user.getUsername() + " | " + user.getFirstName() + " | " +  user.getLastName())
                .collect(Collectors.joining("\n"));

        var sendMessage = new SendMessage();
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText(users);
        sendMessage(sendMessage);
    }

    public void onSetPeriodByUser(Update update) {
        orderService.getDates(update);
    }

    public void onGetPeriod(Update update, LocalDate startDate, LocalDate endDate, Boolean withPrice) {
        var user = userRepository.findByUserTelegramId(update.getMessage().getFrom().getId());
        if (user.isEmpty() || !user.get().isEnabled()) {
            log.warn("Attempt to request period: " + update.getMessage().getFrom().getId());
            sendMessage.setChatId(update.getMessage().getChatId());
            sendMessage.setText("Требуется регистрация");
            sendMessage(sendMessage);
            return;
        }
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText(orderService.getInfoOrders(user.get(), startDate, endDate, withPrice));
        sendMessage(sendMessage);
    }

    public void onSetHash(Update update) {
        status = Status.HASH;
        prompt(update);
    }

    public void onGetHash(Update update) {
        controlHashService.getHash(update);
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
        activateUserService.getUserById(update);
    }

    public void setTelegramBot(TelegramBot telegramBot) {
        this.telegramBot = telegramBot;
    }

    protected void setFunc(Status key, Function<Update, Status> value) {
        mapFunc.put(key, value);
    }

    public void onStartActionDoing(Long chatId) {
        sendMessage.setReplyMarkup(keyboardBotService.createMainKeyboard());
        sendMessage(chatId, START_TEXT);
    }

    public void onHelpDoing(Long chatId) {
        sendMessage(chatId, HELP_TEXT);
    }

    public void onRegisterActionDoing(Update update) {
        status = Status.DEFAULT;
        registerUserBotService.welcomeToRegister(update);
    }

    public void onUnknownActionDoing(Update update) {
        if (status == Status.DEFAULT) {
            log.warn("Unknown command: " + update.getMessage().getText());
            sendMessage(update.getMessage().getChatId(), "нераспознанная команда");
            return;
        }
        status = mapFunc.getOrDefault(status, (e) -> {
                    log.warn(
                            this.getClass().getSimpleName() + ": not found the method during setting the property");
                    return Status.DEFAULT;
                }
        ).apply(update);
    }

    public void callBackQuery(Update update) {
        var id = update.getCallbackQuery().getData();
        status = Status.valueOf(id);

        this.status = mapFunc.getOrDefault(status, (e) -> {
                    log.warn(
                            this.getClass().getSimpleName() + ": not found method on CallbackQuery");
                    return Status.DEFAULT;
                }
        ).apply(update);
    }

    public int sendMessage(SendMessage sendMessage) {
        try {
            return telegramBot.execute(sendMessage).getMessageId();
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
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

    public void sendMessage(Long chatId, String messageText) {
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);
        sendMessage(sendMessage);
    }
}
