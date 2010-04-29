package org.elacin.extract.builder;

import org.elacin.extract.Loggers;
import org.spaceroots.jarmor.Base16Decoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public final class Base16Converter {

    static final Pattern base16Pattern = Pattern.compile("([Xx][0-9a-fA-F]{2})+", Pattern.MULTILINE);

    static String decodeBase16(String encodedInput) {
        /* remove all the 'x's in the string, and extract the bytes from the resulting string */
        final byte[] bytes;
        try {
            bytes = encodedInput.replaceAll("[xX]", "").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Loggers.getTextExtractorLog().warn("UTF-8 was not a valid encoding while extracting bytes from encoded input string " + encodedInput, e);
            return encodedInput;
        }

        /* assemble all the decoded data into a string and return it */
        Base16Decoder decodedStream = new Base16Decoder(new ByteArrayInputStream(bytes), false);
        try {
            StringBuilder sb = new StringBuilder();

            while (0 != decodedStream.available()) {
                char c = (char) decodedStream.read();
                sb.append(c);
            }

            return sb.toString();
        } catch (IOException e) {
            Loggers.getTextExtractorLog().warn("Error while decoding string " + encodedInput, e);
            e.printStackTrace();
        } finally {
            try {
                decodedStream.close();
            } catch (IOException e) {
                Loggers.getTextExtractorLog().warn("Could not close stream", e);
            }
        }
        return encodedInput;
    }

    public static boolean isBase16Encoded(String s) {
        return base16Pattern.matcher(s).matches();
    }
}