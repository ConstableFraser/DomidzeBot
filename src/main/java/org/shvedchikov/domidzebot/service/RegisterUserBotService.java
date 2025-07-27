package org.shvedchikov.domidzebot.service;

import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Pattern;
import org.shvedchikov.domidzebot.util.Status;

@Service
public class RegisterUserBotService {
    private static final String REG_TEXT = """
            [СТАРТ РЕГИСТРАЦИИ]

            Для регистрации пожалуйста укажите:
             - имя, фамилию и почту
             - номер дома
             - личный кабинет
             - реквизиты доступа к личному кабинету""";

    private final List<Map<String, String>> buttonsRegister = List.of(
            new LinkedHashMap<>() {
                {
                    put("1. имя", Status.NAME.name());
                    put("2. фамилия", Status.LASTNAME.name());
                    put("3. email", Status.EMAIL.name());
                }
            },
            new LinkedHashMap<>() {
                {
                    put("4. номер дома", Status.HOUSENUMBER.name());
                    put("5. сайт", Status.DOMAIN.name());
                }
            },
            new LinkedHashMap<>() {
                {
                    put("6. логин", Status.LOGIN.name());
                    put("7. пароль", Status.PASSWORD.name());
                }
            },
            new LinkedHashMap<>() {
                {
                    put("[завершить регистрацию]", Status.FINISHEDREGISTER.name());
                }
            }
    );

    private final SendMessage sendMessage = new SendMessage();
    private final EditMessageText editMessageText = new EditMessageText();
    private int sentMessage;
    private UserCreateDTO userCreateDTO = new UserCreateDTO();
    private HouseCreateDTO houseCreateDTO = new HouseCreateDTO();
    private CredentialCreateDTO credentialCreateDTO = new CredentialCreateDTO();
    private Domain domain = new Domain();
    private List<Domain> domains;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyboardBotService keyboardBotService;

    @Autowired
    private CredentialService credentialService;

    @Autowired
    private HouseService houseService;

    @Autowired
    private UserService userService;

    public void welcomeToRegister(TelegramBotService telegramBotService, Update update) {
        var chatId = getChatId(update);
        var tgId = getTgId(update);

        if (userRepository.findByUserTelegramId(tgId).isPresent()) {
            sendMessage.setReplyMarkup(keyboardBotService.createInlineKeyboard());
            sentMessage = telegramBotService.sendMessage(chatId, "Вы уже зарегистрированы");
            return;
        }
        sendMessage.setChatId(chatId);
        sendMessage.setText(REG_TEXT);
        sendMessage.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));
        sentMessage = telegramBotService.sendMessage(sendMessage);
    }

    protected Status getName(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.NAME);
        displayPrompt(telegramBotService, update, this::setName, "Шаг 1 из 7. Укажите имя: ");
        return Status.NAME;
    }

    protected Status setName(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.NAME);
        userCreateDTO.setFirstName(update.getMessage().getText());
        setter(telegramBotService, update, this::getName, "Имя сохранено. Нажмите \"фамилия\"");
        return Status.DEFAULT;
    }

    protected Status getLastName(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.LASTNAME);
        displayPrompt(telegramBotService, update, this::setLastName, "Шаг 2 из 7. Введите фамилию: ");
        return Status.LASTNAME;
    }

    protected Status setLastName(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.LASTNAME);
        userCreateDTO.setLastName(update.getMessage().getText());
        setter(telegramBotService, update, this::getLastName, "Фамилия сохранена. Нажмите \"3. email\"");
        return Status.DEFAULT;
    }

    protected Status getEmail(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.EMAIL);
        displayPrompt(telegramBotService, update, this::setEmail, "Шаг 3 из 7. Введите Email: ");
        return Status.EMAIL;
    }

    protected Status setEmail(TelegramBotService telegramBotService, Update update) {
        var emailregex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        var pattern = Pattern.compile(emailregex);
        var email = update.getMessage().getText();
        var chatId = getChatId(update);

        if (email.isEmpty() || !pattern.matcher(email).matches()) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(update.getMessage().getMessageId());
            telegramBotService.sendMessage(deleteMessage);
            return Status.EMAIL;
        }
        telegramBotService.setStatus(Status.EMAIL);
        userCreateDTO.setEmail(update.getMessage().getText());
        setter(telegramBotService, update, this::getEmail, "Email сохранён. Нажмите \"4. номер дома\"");
        return Status.DEFAULT;
    }

    protected Status getHouse(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.HOUSENUMBER);
        displayPrompt(telegramBotService, update, this::setHouse, "Шаг 4 из 7. Введите номер Дома (только цифры): ");
        return Status.HOUSENUMBER;
    }

    protected Status setHouse(TelegramBotService telegramBotService, Update update) {
        var chatId = getChatId(update);
        telegramBotService.setStatus(Status.HOUSENUMBER);
        var inputString = update.getMessage().getText().replaceAll("\\D*", "");

        if (inputString.isEmpty() || Integer.parseInt(inputString) == 0) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(update.getMessage().getMessageId());
            telegramBotService.sendMessage(deleteMessage);
            return Status.HOUSENUMBER;
        }
        houseCreateDTO.setNumber(Integer.parseInt(inputString));
        setter(telegramBotService, update, this::getHouse, "Номер дома сохранён. Нажмите \"5. сайт\"");
        return Status.DEFAULT;
    }

    protected Status getDomain(TelegramBotService telegramBotService, Update update) {
        var chatId = getChatId(update);
        telegramBotService.setStatus(Status.DOMAIN);
        domains = domainRepository.findAll();
        var domainList = domains.stream()
                .map(d -> d.getDomain().replaceAll("\\..*", "").toUpperCase().toUpperCase())
                .toList();
        final List<Map<String, String>> buttonsDomains = new ArrayList<>();
        for (String domain : domainList) {
            Map<String, String> map = new LinkedHashMap<>();
            map.put(domain, domain);
            buttonsDomains.add(map);
        }
        editMessageText.setChatId(chatId);
        editMessageText.setText("Шаг 5 из 7. Где ваш личный кабинет?");
        editMessageText.setMessageId(sentMessage);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsDomains));
        telegramBotService.sendMessage(editMessageText);
        return Status.DOMAIN;
    }

    protected void setDomain(TelegramBotService telegramBotService, Update update, String domainShortname) {
        telegramBotService.setStatus(Status.DOMAIN);
        var domainName = domains.stream()
                .map(Domain::getDomain)
                .filter(d -> d.contains(domainShortname.toLowerCase()))
                .findFirst()
                .orElse("");
        this.domain = domainRepository.findDomainByDomain(domainName);
        setter(telegramBotService, update, this::getDomain, "Выбранный сайт сохранён. Нажмите \"6. логин\"");
    }

    protected Status setEthnomir(TelegramBotService telegramBotService, Update update) {
        setDomain(telegramBotService, update, update.getCallbackQuery().getData());
        return Status.DEFAULT;
    }

    protected Status getLogin(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.LOGIN);
        displayPrompt(telegramBotService, update, this::setLogin, "Шаг 6 из 7. Введите Логин: ");
        return Status.LOGIN;
    }

    protected Status setLogin(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.LOGIN);
        credentialCreateDTO.setLogin(update.getMessage().getText());
        setter(telegramBotService, update, this::getLogin, "Логин сохранён. Нажмите \"7. пароль\"");
        return Status.DEFAULT;
    }

    protected Status getPassword(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.PASSWORD);
        displayPrompt(telegramBotService, update, this::setPassword, "Шаг 7 из 7. Введите Пароль: ");
        return Status.PASSWORD;
    }

    protected Status setPassword(TelegramBotService telegramBotService, Update update) {
        telegramBotService.setStatus(Status.PASSWORD);
        credentialCreateDTO.setPassword(update.getMessage().getText());
        setter(telegramBotService, update, this::getPassword, "Пароль сохранён. Нажмите \"завершить регистрацию\"");
        return Status.DEFAULT;
    }

    protected Status startCompleteRegister(TelegramBotService telegramBotService, Update update) {
        var chatId = getChatId(update);
        telegramBotService.setStatus(Status.FINISHEDREGISTER);
        final List<Map<String, String>> buttonsFinished = List.of(
                new LinkedHashMap<>() {
                    {
                        put("ДА", Status.ACCEPTUSER.name());
                        put("НЕТ", Status.REJECTUSER.name());
                    }
                }
        );
        String userInfo = String.format(
                "имя: %s\nфамилия: %s\nпочта: %s\nномер дома: %d\nсайт: %s\nлогин: %s\nпароль: %s",
                userCreateDTO.getFirstName(),
                userCreateDTO.getLastName(),
                userCreateDTO.getEmail(),
                houseCreateDTO.getNumber(),
                domain.getDomain(),
                credentialCreateDTO.getLogin(),
                credentialCreateDTO.getPassword());
        editMessageText.setChatId(chatId);
        editMessageText.setText("данные корректны?\n\n" + userInfo);
        editMessageText.setMessageId(sentMessage);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsFinished));
        telegramBotService.sendMessage(editMessageText);
        return Status.FINISHEDREGISTER;
    }

    protected Status onAccept(TelegramBotService telegramBotService, Update update) {
        var chatId = getChatId(update);
        var tgId = getTgId(update);
        telegramBotService.setStatus(Status.ACCEPTUSER);
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(sentMessage);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        if (checkingForExistUser(update)) {
            editMessageText.setText("Вы уже зарегистрированы");
            telegramBotService.sendMessage(editMessageText);
            return Status.DEFAULT;
        }

        if (!verifyData()) {
            editMessageText.setText("Не хватает данных. Заполните все поля!");
            telegramBotService.sendMessage(editMessageText);
            welcomeToRegister(telegramBotService, update);
            return Status.DEFAULT;
        }

        if (createProfile(update)) {
            var text = String.format("""
                    ✅ профиль успешно создан ✅
                    сообщите администратору
                    этот номер: %s""", tgId);
            editMessageText.setText(text);
        } else {
            editMessageText.setText("❌ ошибка, профиль не создан ❌\nобратитесь к администратору");
        }
        telegramBotService.sendMessage(editMessageText);
        userCreateDTO = new UserCreateDTO();
        houseCreateDTO = new HouseCreateDTO();
        credentialCreateDTO = new CredentialCreateDTO();
        return Status.DEFAULT;
    }

    protected Status onReject(TelegramBotService telegramBotService, Update update) {
        var chatId = getChatId(update);
        telegramBotService.setStatus(Status.REJECTUSER);
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(sentMessage);
        telegramBotService.sendMessage(deleteMessage);
        welcomeToRegister(telegramBotService, update);
        return Status.DEFAULT;
    }

    private void displayPrompt(TelegramBotService telegramBotService,
                               Update update,
                               BiFunction<TelegramBotService, Update, Status> consumer,
                               String messageText) {
        var editMessageText = new EditMessageText();
        var chatId = update.hasMessage() && update.getMessage().hasText()
                ? update.getMessage().getChatId() : update.getCallbackQuery().getMessage().getChatId();
        editMessageText.setChatId(chatId);
        editMessageText.setText(messageText);
        editMessageText.setMessageId(sentMessage);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard());

        telegramBotService.sendMessage(editMessageText);
        telegramBotService.setFunc(telegramBotService.getStatus(), consumer);
    }

    private void setter(TelegramBotService telegramBotService,
                        Update update,
                        BiFunction<TelegramBotService, Update, Status> consumer,
                        String messageText) {
        telegramBotService.setFunc(telegramBotService.getStatus(), consumer);
        var chatId = getChatId(update);

        if (!update.hasCallbackQuery()) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(update.getMessage().getMessageId());
            telegramBotService.sendMessage(deleteMessage);
        }

        editMessageText.setChatId(chatId);
        editMessageText.setText(messageText);
        editMessageText.setMessageId(sentMessage);
        editMessageText.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));
        telegramBotService.sendMessage(editMessageText);
    }

    private boolean verifyData() {
        return Objects.nonNull(userCreateDTO.getFirstName())
                & Objects.nonNull(userCreateDTO.getLastName())
                & Objects.nonNull(userCreateDTO.getEmail())
                & Objects.nonNull(houseCreateDTO.getNumber())
                & Objects.nonNull(domain.getDomain())
                & Objects.nonNull(credentialCreateDTO.getLogin())
                & Objects.nonNull(credentialCreateDTO.getPassword());
    }

    private boolean checkingForExistUser(Update update) {
        return userRepository.findByUserTelegramId(getTgId(update)).isPresent();
    }

    private boolean createProfile(Update update) {
        var tgId = getTgId(update);
        userCreateDTO.setUserTelegramId(tgId);
        var pwd = String.valueOf(Math.round(Math.random() * Integer.MAX_VALUE / 2)) + System.currentTimeMillis();
        userCreateDTO.setPassword(CoderDecoder.encodeString(pwd));
        userCreateDTO.setPassword(pwd);
        var user = userService.create(userCreateDTO);
        var credential = credentialService.create(credentialCreateDTO);
        houseCreateDTO.setOwnerId(user.getId());
        houseCreateDTO.setCredentialId(credential.getId());
        houseCreateDTO.setDomainId(domain.getId());
        var house = houseService.create(houseCreateDTO);

        return Objects.nonNull(house);
    }

    private long getChatId(Update update) {
        return update.hasMessage() && update.getMessage().hasText() ? update.getMessage().getChatId()
                : update.getCallbackQuery().getMessage().getChatId();
    }

    private long getTgId(Update update) {
        return update.hasMessage() && update.getMessage().hasText() ? update.getMessage().getFrom().getId()
                : update.getCallbackQuery().getFrom().getId();
    }
}
