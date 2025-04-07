package org.shvedchikov.domidzebot.service;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.shvedchikov.domidzebot.component.RestRequestSender;
import org.shvedchikov.domidzebot.repository.HouseRepository;
import org.shvedchikov.domidzebot.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Period;

@Slf4j
@Service
public class OrderService {
    @Autowired
    private HouseRepository houseRepository;

    @Autowired
    private RestRequestSender restRequestSender;

    @Autowired
    private CoderDecoder coderDecoder;

    // private String domain;
    private String login;
    private byte[] password;

    protected String getInfoOrders(User user, Period period) {
        var result = houseRepository.findAllByOwner(user.getId());
        // domain = String.valueOf(result.get(0).getOrDefault("domain", "null"));
        login = String.valueOf(result.get(0).getOrDefault("login", "null"));
        var pwd = String.valueOf(result.get(0).getOrDefault("pwd", "null"));

        try {
            password = coderDecoder.decodePwd(pwd).getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return parserHtml(connectedToDomain(period));
    }

    private String connectedToDomain(Period period) {
        restRequestSender.setHost("https://ethnomir.ru/personal/owner/");
        restRequestSender.setHeaders(login, new String(password, StandardCharsets.UTF_8), period);
        return restRequestSender.sendRequest();
    }

    private String parserHtml(String body) {
        var regStr1 = "\\* Цена до вычета услуг управляющего агента. \\| ";
        var regStr2 = "Дома \\| Барн Хаус \\d+ \\| ";
        StringBuilder result = new StringBuilder();
        Document document = Jsoup.parse(body);
        Element table = document.getElementById("sortable");
        Elements rows = table.select("tr");
        rows.remove(0);

        for (Element row : rows) {
            Elements cells = row.select("th, td");
            for (Element cell : cells) {
                String text = cell.text();
                result.append(text).append(" | ");
            }
            result.append("\n");
        }
        return result.toString().replaceAll(regStr1, "\n").replaceAll(regStr2, "");
    }
}
