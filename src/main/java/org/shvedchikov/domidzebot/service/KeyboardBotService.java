package org.shvedchikov.domidzebot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

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
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardFirstRow = new KeyboardRow();
        keyboardFirstRow.add("/register");
        keyboardFirstRow.add("/month");
        keyboardFirstRow.add("/help");

        KeyboardRow keyboardSecondRow = new KeyboardRow();
        keyboardSecondRow.add("/halfyear");
        keyboardSecondRow.add("/monthprev");
        keyboardSecondRow.add("/halfyearprev");

        keyboard.add(keyboardFirstRow);
        keyboard.add(keyboardSecondRow);
        replyKeyboardMarkup.setKeyboard(keyboard);

        return replyKeyboardMarkup;
    }
}
