package org.shvedchikov.domidzebot.component;

import lombok.extern.slf4j.Slf4j;
import org.shvedchikov.domidzebot.config.AppProperties;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

@Slf4j
@Component
public final class CoderDecoder {
    private static final int DELTA = 48;
    private String hash;

    public CoderDecoder(AppProperties app) throws Exception {
        if (Objects.isNull(app.getHash())) {
            log.error("Static property 'hash' is not found");
            throw new Exception("Property 'hash' is not found");
        }
        hash = app.getHash();
    }

    public String encodePwd(String sourcePwd) throws Exception {
        hash = System.getProperty("DHASH", "null");
        if (hash.equals("null")) {
            log.error("Dynamic property 'dhash' is not found");
            throw new Exception("Dynamic property 'hash' is not found");
        }

        byte[] password = Base64.getDecoder().decode(encodeString(sourcePwd));
        byte[] prop = Base64.getDecoder().decode(hash);

        byte[] result = new byte[prop.length + 1];

        for (int i = 0, j = password.length - 1; i < prop.length;) {
            result[i] = (byte) (password[j--] ^ prop[i++]);
            j = j == -1 ? password.length - 1 : j;
        }
        result[result.length - 1] = (byte) (password.length ^ prop[0]);

        return Base64.getEncoder().encodeToString(result);
    }

    public String decodePwd(String encodedPwd) throws Exception {
        hash = System.getProperty("DHASH", "null");
        if (hash.equals("null")) {
            log.error("Dynamic property 'dhash' is not found");
            throw new Exception("Dynamic property 'hash' is not found");
        }

        byte[] password = Base64.getDecoder().decode(encodedPwd);
        byte[] prop = Base64.getDecoder().decode(hash);

        int pwdSize = password[password.length - 1] ^ prop[0];
        byte[] result = new byte[pwdSize];

        for (int i = 0; i < pwdSize; i++) {
            result[result.length - i - 1] = (byte) (password[i] ^ prop[i]);
        }

        return decodeString(Base64.getEncoder().encodeToString(result));
    }

    public static String encodeString(String pwd) {
        if (pwd == null) {
            throw new RuntimeException("Parameter not be NULL");
        }

        byte[] array = pwd.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[array.length];

        if (array.length < 2) {
            return array.length == 0
                    ? "" : Base64.getEncoder().encodeToString(new byte[]{(byte) ((array[0] + DELTA) ^ DELTA)});
        }

        for (int i = 0; i < array.length - 1; i++) {
            result[i] = (byte) ((array[i] ^ array[i + 1]) + DELTA);
        }
        result[array.length - 1] = (byte) ((array[array.length - 1] ^ result[0]) + DELTA);

        return Base64.getEncoder().encodeToString(result);
    }

    public static String decodeString(String pwd) {
        if (pwd == null) {
            throw new RuntimeException("Parameter not be NULL");
        }

        byte[] array = Base64.getDecoder().decode(pwd);
        byte[] result = new byte[array.length];

        if (array.length < 2) {
            return array.length == 0
                    ? "" : new String(new byte[]{(byte) ((array[0] ^ DELTA) - DELTA)}, StandardCharsets.UTF_8);
        }
        result[array.length - 1] = (byte) ((array[array.length - 1] - DELTA) ^ array[0]);

        for (int i = 0; i < array.length - 1; i++) {
            var charArray = array[array.length - 2 - i] - DELTA;
            var charResult = result[array.length - 1 - i];
            result[array.length - 2 - i] = (byte) (charArray ^ charResult);
        }

        return new String(result, StandardCharsets.UTF_8);
    }
}
