package org.shvedchikov.domidzebot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class KeyboardBotService {
    public InlineKeyboardMarkup createKeyboard(List<Map<String, String>> buttonsList) {
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

    public InlineKeyboardMarkup createKeyboard() {
        return createKeyboard(List.of(Map.of()));
    }
}
