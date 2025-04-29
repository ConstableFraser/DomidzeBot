package org.shvedchikov.domidzebot.component;

import lombok.Getter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collections;

@Component
public class RestRequestSender {
    private String host;
    private final RestTemplate restTemplate;
    private HttpHeaderCreator httpHeaderCreator;

    public RestRequestSender(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void setHeaders(String username, String password, LocalDate startDate, LocalDate endDate) {
        this.httpHeaderCreator = new HttpHeaderCreator(username, password, startDate, endDate);
    }

    public void setHost(String host) {
        this.host = host;
    }

    @Getter
    private static final class HttpHeaderCreator {
        private final HttpEntity<MultiValueMap<String, String>> httpEntity;

        private HttpHeaderCreator(String username, String password, LocalDate startDate, LocalDate endDate) {
            this.httpEntity = createHeader(username, password, startDate, endDate);
        }

        private HttpEntity<MultiValueMap<String, String>> createHeader(String username,
                                                                       String pwd,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-type", "application/x-www-form-urlencoded;charset=UTF-8");
            MultiValueMap<String, String> dataPayload = new LinkedMultiValueMap<>();
            dataPayload.put("DATE_FROM", Collections.singletonList(startDate.toString()));
            dataPayload.put("DATE_TO", Collections.singletonList(endDate.toString()));
            dataPayload.put("submit", Collections.singletonList("Показать отчёт"));
            String auth = username + ":" + pwd;
            byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes(StandardCharsets.UTF_8));
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
            return new HttpEntity<>(dataPayload, headers);
        }
    }

    public String sendRequest() {
        ResponseEntity<String> response = restTemplate.exchange(
                host,
                HttpMethod.POST,
                httpHeaderCreator.getHttpEntity(),
                String.class);

        return response.getBody();
    }
}
