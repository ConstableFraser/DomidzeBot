package org.shvedchikov.domidzebot;

import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.util.CoderDecoder;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.shvedchikov.domidzebot.util.CoderDecoder.encodeString;
import static org.shvedchikov.domidzebot.util.CoderDecoder.decodeString;


@SpringBootTest
public class CodingDecodingTest {
    @Test
    public void testCodeDecodeString() {
        var source = "this is are string";

        var codedString = encodeString(source);
        assertThat(source).isEqualTo(decodeString(codedString));
    }

    @Test
    public void testCodeDecodeStringVariations() {
        var source = "1";

        var codedString = CoderDecoder.encodeString(source);
        assertThat(source).isEqualTo(decodeString(codedString));

        source = "";
        codedString = CoderDecoder.encodeString(source);
        assertThat(codedString).isEqualTo("");
        assertThat(source).isEqualTo(decodeString(codedString));

        source = "Neque porro quisquam est qui dolorem ipsum quia dolor sit amet";
        codedString = encodeString(source);
        assertThat(source).isEqualTo(decodeString(codedString));
    }

    @Test
    public void testCodeDecodePwd() {
        var sourcePwd = "source of password";
        String encodedPwd;
        String result;

        try {
            encodedPwd = CoderDecoder.getCoderDecoder().encodePwd(encodeString(sourcePwd));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            result = decodeString(CoderDecoder.getCoderDecoder().decodePwd(encodedPwd));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(result).isEqualTo(sourcePwd);
    }
}
