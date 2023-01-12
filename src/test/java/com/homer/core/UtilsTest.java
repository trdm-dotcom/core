package com.homer.core;

import com.homer.core.utils.Utils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

@SpringBootTest
public class UtilsTest {
    @Test
    public void testAesDecryption() throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        String hash = "u1lQqNp0Q6W553iTPv7JZo/2Gv25a8imh6KxZJw+z8/9rSJn195bVl9xpZg5786To2Jru2iQ2bNeqW6RnH0aMA==";
        Map<String, String> hashObject = Utils.AesDecryptionHash(hash);
        System.out.println(hashObject);
    }
}
