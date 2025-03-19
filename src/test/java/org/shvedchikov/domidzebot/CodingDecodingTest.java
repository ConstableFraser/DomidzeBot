package org.shvedchikov.domidzebot;

import org.junit.jupiter.api.Test;
import org.shvedchikov.domidzebot.component.CoderDecoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.shvedchikov.domidzebot.component.CoderDecoder.encodeString;
import static org.shvedchikov.domidzebot.component.CoderDecoder.decodeString;


@SpringBootTest
@TestPropertySource(properties = "appproperties.hash=djM7MT82MzYxgkA9iYtLdzeFSESCpzCuOptoS0mLMjKRT1dLRTEwUDBINk5rSzVFQDVFP4kzNjE9OTM2MTQzlUg2M001RT42SFE=")
public class CodingDecodingTest {
    @Autowired
    private CoderDecoder coderDecoder;

    @Test
    public void testCodeDecodeString() {
        var source = "this is are string";

        var codedString = encodeString(source);
        assertThat(source).isEqualTo(decodeString(codedString));
    }

    @Test
    public void testCodeDecodeStringVariations() {
        var source = "1";

        var codedString = encodeString(source);
        assertThat(source).isEqualTo(decodeString(codedString));

        source = "";
        codedString = encodeString(source);
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
            encodedPwd = coderDecoder.encodePwd(encodeString(sourcePwd));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            result = decodeString(coderDecoder.decodePwd(encodedPwd));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        assertThat(result).isEqualTo(sourcePwd);
    }
}
