/*
 * Copyright 2010 Øyvind Berg (elacin@gmail.com)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.elacin.pdfextract.builder;

import org.elacin.pdfextract.Loggers;
import org.spaceroots.jarmor.Base16Decoder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

public final class Base16Converter {

    static final Pattern base16Pattern = Pattern.compile("([Xx][0-9a-fA-F]{2})+", Pattern.MULTILINE);

    static String decodeBase16(String encodedInput) {
        /* remove all the 'x's in the string, and pdfextract the bytes from the resulting string */
        final byte[] bytes;
        try {
            bytes = encodedInput.replaceAll("[xX]", "").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Loggers.getPdfExtractorLog().warn("UTF-8 was not a valid encoding while extracting bytes from encoded input string " + encodedInput, e);
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
            Loggers.getPdfExtractorLog().warn("Error while decoding string " + encodedInput, e);
            e.printStackTrace();
        } finally {
            try {
                decodedStream.close();
            } catch (IOException e) {
                Loggers.getPdfExtractorLog().warn("Could not close stream", e);
            }
        }
        return encodedInput;
    }

    public static boolean isBase16Encoded(String s) {
        return base16Pattern.matcher(s).matches();
    }
}