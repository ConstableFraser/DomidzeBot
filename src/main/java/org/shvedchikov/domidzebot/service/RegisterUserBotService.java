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
import java.util.function.Function;
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

    private TelegramBotService telegramBotService;
    private final SendMessage sendMessage = new SendMessage();
    private final EditMessageText editMessageText = new EditMessageText();
    private int sentMessage;
    private UserCreateDTO userCreateDTO = new UserCreateDTO();
    private HouseCreateDTO houseCreateDTO = new HouseCreateDTO();
    private CredentialCreateDTO credentialCreateDTO = new CredentialCreateDTO();
    private Domain domain = new Domain();
    private List<Domain> domains;
    private long chatId;
    private long tgId;

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

    public void setTelegramBot(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    public void welcomeToRegister(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            chatId = update.getMessage().getChatId();
            tgId = update.getMessage().getFrom().getId();
        } else {
            chatId = update.getCallbackQuery().getMessage().getChatId();
            tgId = update.getCallbackQuery().getFrom().getId();
        }
        sendMessage.setChatId(chatId);
        if (userRepository.findByUserTelegramId(tgId).isPresent()) {
            sendMessage.setReplyMarkup(keyboardBotService.createInlineKeyboard());
            sendMessage.setText("Вы уже зарегистрированы");
            sentMessage = telegramBotService.sendMessage(sendMessage);
            return;
        }
        sendMessage.setText(REG_TEXT);
        sendMessage.setReplyMarkup(keyboardBotService.createInlineKeyboard(buttonsRegister));
        sentMessage = telegramBotService.sendMessage(sendMessage);
    }

    protected Status getName(Update update) {
        telegramBotService.setStatus(Status.NAME);
        displayPrompt(update, this::setName, "Шаг 1 из 7. Укажите имя: ");
        return Status.NAME;
    }

    protected Status setName(Update update) {
        telegramBotService.setStatus(Status.NAME);
        userCreateDTO.setFirstName(update.getMessage().getText());
        setter(update, this::getName, "Имя сохранено. Нажмите \"фамилия\"");
        return Status.DEFAULT;
    }

    protected Status getLastName(Update update) {
        telegramBotService.setStatus(Status.LASTNAME);
        displayPrompt(update, this::setLastName, "Шаг 2 из 7. Введите фамилию: ");
        return Status.LASTNAME;
    }

    protected Status setLastName(Update update) {
        telegramBotService.setStatus(Status.LASTNAME);
        userCreateDTO.setLastName(update.getMessage().getText());
        setter(update, this::getLastName, "Фамилия сохранена. Нажмите \"3. email\"");
        return Status.DEFAULT;
    }

    protected Status getEmail(Update update) {
        telegramBotService.setStatus(Status.EMAIL);
        displayPrompt(update, this::setEmail, "Шаг 3 из 7. Введите Email: ");
        return Status.EMAIL;
    }

    protected Status setEmail(Update update) {
        var emailregex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        var pattern = Pattern.compile(emailregex);
        var email = update.getMessage().getText();

        if (email.isEmpty() || !pattern.matcher(email).matches()) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(chatId);
            deleteMessage.setMessageId(update.getMessage().getMessageId());
            telegramBotService.sendMessage(deleteMessage);
            return Status.EMAIL;
        }
        telegramBotService.setStatus(Status.EMAIL);
        userCreateDTO.setEmail(update.getMessage().getText());
        setter(update, this::getEmail, "Email сохранён. Нажмите \"4. номер дома\"");
        return Status.DEFAULT;
    }

    protected Status getHouse(Update update) {
        telegramBotService.setStatus(Status.HOUSENUMBER);
        displayPrompt(update, this::setHouse, "Шаг 4 из 7. Введите номер Дома (только цифры): ");
        return Status.HOUSENUMBER;
    }

    protected Status setHouse(Update update) {
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
        setter(update, this::getHouse, "Номер дома сохранён. Нажмите \"5. сайт\"");
        return Status.DEFAULT;
    }

    protected Status getDomain(Update update) {
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

    protected void setDomain(Update update, String domainShortname) {
        telegramBotService.setStatus(Status.DOMAIN);
        var domainName = domains.stream()
                .map(Domain::getDomain)
                .filter(d -> d.contains(domainShortname.toLowerCase()))
                .findFirst()
                .orElse("");
        this.domain = domainRepository.findDomainByDomain(domainName);
        setter(update, this::getDomain, "Выбранный сайт сохранён. Нажмите \"6. логин\"");
    }

    protected Status setEthnomir(Update update) {
        setDomain(update, update.getCallbackQuery().getData());
        return Status.DOMAIN;
    }

    protected Status setBnovo(Update update) {
        setDomain(update, update.getCallbackQuery().getData());
        return Status.DOMAIN;
    }

    protected Status getLogin(Update update) {
        telegramBotService.setStatus(Status.LOGIN);
        displayPrompt(update, this::setLogin, "Шаг 6 из 7. Введите Логин: ");
        return Status.LOGIN;
    }

    protected Status setLogin(Update update) {
        telegramBotService.setStatus(Status.LOGIN);
        credentialCreateDTO.setLogin(update.getMessage().getText());
        setter(update, this::getLogin, "Логин сохранён. Нажмите \"7. пароль\"");
        return Status.DEFAULT;
    }

    protected Status getPassword(Update update) {
        telegramBotService.setStatus(Status.PASSWORD);
        displayPrompt(update, this::setPassword, "Шаг 6 из 7. Введите Пароль: ");
        return Status.PASSWORD;
    }

    protected Status setPassword(Update update) {
        telegramBotService.setStatus(Status.PASSWORD);
        credentialCreateDTO.setPassword(update.getMessage().getText());
        setter(update, this::getPassword, "Пароль сохранён. Нажмите \"завершить регистрацию\"");
        return Status.DEFAULT;
    }

    protected Status startCompleteRegister(Update update) {
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

    protected Status onAccept(Update update) {
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
            editMessageText.setText("Не хватает данных. Заполните все поля!\n\n");
            telegramBotService.sendMessage(editMessageText);
            welcomeToRegister(update);
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

    protected Status onReject(Update update) {
        telegramBotService.setStatus(Status.REJECTUSER);
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(chatId);
        deleteMessage.setMessageId(sentMessage);
        telegramBotService.sendMessage(deleteMessage);
        welcomeToRegister(update);
        return Status.DEFAULT;
    }

    private void displayPrompt(Update update, Function<Update, Status> consumer, String messageText) {
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

    private void setter(Update update, Function<Update, Status> consumer, String messageText) {
        telegramBotService.setFunc(telegramBotService.getStatus(), consumer);

        if (!update.hasCallbackQuery()) {
            DeleteMessage deleteMessage = new DeleteMessage();
            deleteMessage.setChatId(this.chatId);
            deleteMessage.setMessageId(update.getMessage().getMessageId());
            telegramBotService.sendMessage(deleteMessage);
        }

        editMessageText.setChatId(this.chatId);
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
        return userRepository.findByUserTelegramId(tgId).isPresent();
    }

    private boolean createProfile(Update update) {
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
}
