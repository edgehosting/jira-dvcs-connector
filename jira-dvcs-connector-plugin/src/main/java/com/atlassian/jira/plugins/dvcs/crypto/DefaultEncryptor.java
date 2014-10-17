package com.atlassian.jira.plugins.dvcs.crypto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * An encryption service for storing passwords
 */
@Component
public class DefaultEncryptor implements Encryptor
{
    final Logger logger = LoggerFactory.getLogger(DefaultEncryptor.class);

    /**
     * Encrypt the input into a hex encoded string;
     *
     * @param input the input to encrypt
     * @param organizationName the project key
     * @param hostUrl the repository url
     * @return the encrypted string
     */
    @Override
    public String encrypt(String input, String organizationName, String hostUrl)
    {
        byte[] encrypted;
        try
        {
            byte[] key = (organizationName + hostUrl).getBytes("utf-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            // Generate the secret key specs.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);

            encrypted = cipher.doFinal((input).getBytes("utf-8"));
        }
        catch (Exception e)
        {
            logger.debug("error encrypting", e);
            encrypted = new byte[0];
        }

        BigInteger bi = new BigInteger(1, encrypted);
        return String.format("%0" + (encrypted.length << 1) + "X", bi);
    }

    private static byte[] hexStringToByteArray(String s)
    {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2)
        {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public String decrypt(String password, String organizationName, String hostUrl)
    {
        if (password == null)
        { return null; }

        try
        {
            byte[] ciphertext = DefaultEncryptor.hexStringToByteArray(password);

            // Get the Key
            byte[] key = (organizationName + hostUrl).getBytes("utf-8");
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // use only first 128 bit

            // Generate the secret key specs.
            SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");

            // Instantiate the cipher
            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] original = cipher.doFinal(ciphertext);
            String originalString = new String(original, "utf-8");
            //logger.debug("Original string: " + originalString + "\nOriginal string (Hex): " + original.toString());

//            logger.debug("decrypted password [ " + original+" ]");
            return originalString;

        }
        catch (Exception e)
        {
            logger.debug("error decrypting", e);
        }
        return "";
    }

}
