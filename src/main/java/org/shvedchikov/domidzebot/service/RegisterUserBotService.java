package org.shvedchikov.domidzebot.service;

import static org.shvedchikov.domidzebot.service.TelegramBotService.Status;
import org.shvedchikov.domidzebot.dto.credential.CredentialCreateDTO;
import org.shvedchikov.domidzebot.dto.house.HouseCreateDTO;
import org.shvedchikov.domidzebot.dto.user.UserCreateDTO;
import org.shvedchikov.domidzebot.model.Domain;
import org.shvedchikov.domidzebot.repository.DomainRepository;
import org.shvedchikov.domidzebot.util.CoderDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

@Service
public class RegisterUserBotService {
    private static final String REG_TEXT = """
            [СТАРТ РЕГИСТРАЦИИ]

            Для регистрации обязательно заполнить:
             - имя, фамилию и почту
             - номер дома
             - указать сайт
             - реквизиты доступа к ЛК""";

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
    private UserCreateDTO userCreateDTO = new UserCreateDTO();
    private HouseCreateDTO houseCreateDTO = new HouseCreateDTO();
    private CredentialCreateDTO credentialCreateDTO = new CredentialCreateDTO();
    private Domain domain = new Domain();
    private List<Domain> domains;

    @Autowired
    private DomainRepository domainRepository;

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

    public void welcomeToRegister(Long chatId) {
        sendMessage.setChatId(chatId);
        sendMessage.setText(REG_TEXT);
        sendMessage.setReplyMarkup(keyboardBotService.createKeyboard(buttonsRegister));
        telegramBotService.sendMessage(sendMessage);
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
        //TODO verify the email
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
        houseCreateDTO.setNumber(Integer.valueOf(update.getMessage().getText().replaceAll("\\D*", "")));
        setter(update, this::getHouse, "Номер дома сохранён. Нажмите \"5. сайт\"");
        return Status.DEFAULT;
    }

    protected Status getDomain(Update update) {
        telegramBotService.setStatus(Status.DOMAIN);
        domains = domainRepository.findAll();
        var domainList = IntStream.of(0, domains.size() - 1)
                .mapToObj(i -> i + ". " + domains.get(i).getDomain() + "\n")
                .toList();
        StringBuilder textMessage = new StringBuilder();
        domainList.forEach(textMessage::append);
        displayPrompt(update, this::setDomain,
                "Шаг 5 из 7. Укажите сайт (напишите номер пункта):\n\n" + textMessage);
        return Status.DOMAIN;
    }

    protected Status setDomain(Update update) {
        telegramBotService.setStatus(Status.DOMAIN);
        var number = Integer.parseInt(update.getMessage().getText().replaceAll("\\D*", ""));
        if (number >= domains.size()) {
            telegramBotService.sendMessage(
                    update.getMessage().getChatId(),
                    "указан неверный номер пункта. Попробуйте ещё раз");
            getDomain(update);
            return Status.DOMAIN;
        }
        this.domain = domainRepository.findDomainByDomain(domains.get(number).getDomain());
        setter(update, this::getDomain, "Выбранный сайт сохранён. Нажмите \"6. логин\"");
        return Status.DEFAULT;
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
        sendMessage.setChatId(update.getCallbackQuery().getMessage().getChatId());
        sendMessage.setText("убедитесь, что данные корректны. Всё ок?\n\n" + userInfo);
        sendMessage.setReplyMarkup(keyboardBotService.createKeyboard(buttonsFinished));
        telegramBotService.sendMessage(sendMessage);
        return Status.FINISHEDREGISTER;
    }

    protected Status onAccept(Update update) {
        telegramBotService.setStatus(Status.ACCEPTUSER);
        var chatId = update.getCallbackQuery().getMessage().getChatId();
        if (!verifyData()) {
            sendMessage.setChatId(chatId);
            sendMessage.setText("Не хватает данных. Заполните все поля!\n\n");
            keyboardBotService.createKeyboard();
            telegramBotService.sendMessage(sendMessage);
            welcomeToRegister(chatId);
            return Status.DEFAULT;
        }
        telegramBotService.sendMessage(sendMessage);
        if (createProfile(update)) {
            sendMessage.setChatId(chatId);
            sendMessage.setReplyMarkup(keyboardBotService.createKeyboard());
            sendMessage.setText("✅ профиль успешно создан ✅\nдля активации обратитесь к администратору");
        } else {
            sendMessage.setChatId(chatId);
            sendMessage.setReplyMarkup(keyboardBotService.createKeyboard());
            sendMessage.setText("❌ ошибка, профиль не создан ❌\nобратитесь к администратору");
        }
        telegramBotService.sendMessage(sendMessage);
        userCreateDTO = null;
        houseCreateDTO = null;
        credentialCreateDTO = null;

        return Status.DEFAULT;
    }

    protected Status onReject(Update update) {
        telegramBotService.setStatus(Status.REJECTUSER);
        welcomeToRegister(update.getCallbackQuery().getMessage().getChatId());
        return Status.REJECTUSER;
    }

    private void displayPrompt(Update update, Function<Update, Status> consumer, String messageText) {
        var chatId = update.hasMessage() && update.getMessage().hasText()
                ? update.getMessage().getChatId() : update.getCallbackQuery().getMessage().getChatId();
        sendMessage.setChatId(chatId);
        sendMessage.setText(messageText);
        sendMessage.setReplyMarkup(keyboardBotService.createKeyboard());
        telegramBotService.sendMessage(sendMessage);
        telegramBotService.setFunc(telegramBotService.getStatus(), consumer);
    }

    private void setter(Update update, Function<Update, Status> consumer, String messageText) {
        telegramBotService.setFunc(telegramBotService.getStatus(), consumer);
        sendMessage.setChatId(update.getMessage().getChatId());
        sendMessage.setText(messageText);
        sendMessage.setReplyMarkup(keyboardBotService.createKeyboard(buttonsRegister));
        telegramBotService.sendMessage(sendMessage);
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

    private boolean createProfile(Update update) {
        userCreateDTO.setUserTelegramId(update.getCallbackQuery().getFrom().getId());
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
