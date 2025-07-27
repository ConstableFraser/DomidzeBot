package org.shvedchikov.domidzebot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.shvedchikov.domidzebot.util.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KeyboardBotService {
    public InlineKeyboardMarkup createInlineKeyboard(List<Map<String, String>> buttonsList) {
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (Map<String, String> map : buttonsList) {
            List<InlineKeyboardButton> row = new ArrayList<>();
            for (var entry : map.entrySet()) {
                var button = new InlineKeyboardButton();
                button.setText(entry.getKey());
                button.setCallbackData(entry.getValue());
                row.add(button);
            }
            rowsInline.add(row);
        }
        markupInline.setKeyboard(rowsInline);
        return markupInline;
    }

    public InlineKeyboardMarkup createInlineKeyboard() {
        return createInlineKeyboard(List.of(Map.of()));
    }

    public ReplyKeyboardMarkup createMainKeyboard() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setSelective(false);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();
        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add("/" + Command.MONTH.name().toLowerCase());
        keyboardFirstRow.add("/" + Command.HALFYEAR.name().toLowerCase());
        keyboardFirstRow.add("/" + Command.PERIOD.name().toLowerCase());

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add("/" + Command.MONTHPREV.name().toLowerCase());
        keyboardSecondRow.add("/" + Command.HALFYEARPREV.name().toLowerCase());
        keyboardSecondRow.add("/" + Command.REGISTER.name().toLowerCase());

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }
}
