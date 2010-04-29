// Copyright (c) 2005-2006, Luc Maisonobe
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with
// or without modification, are permitted provided that
// the following conditions are met:
// 
//    Redistributions of source code must retain the
//    above copyright notice, this list of conditions and
//    the following disclaimer. 
//    Redistributions in binary form must reproduce the
//    above copyright notice, this list of conditions and
//    the following disclaimer in the documentation
//    and/or other materials provided with the
//    distribution. 
//    Neither the names of spaceroots.org, spaceroots.com
//    nor the names of their contributors may be used to
//    endorse or promote products derived from this
//    software without specific prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
// CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
// THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
// USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
// IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
// NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
// USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package org.spaceroots.jarmor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ASCII85DecoderTest extends TestCase {

  public ASCII85DecoderTest(String name) {
    super(name);
  }

  public void testASCII85Bytes() {
    try {
      assertEquals(0, decodeBytes("  \t\r\f\n~>".getBytes()).length);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testFinalQuantum() {
    checkArrays("!<~>".getBytes(),
                new byte[] { (byte) 0x01 });
    checkArrays("!<N~>".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02 });
    checkArrays("!<N?~>".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 });
    checkArrays("!<N?+~>".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02,
                             (byte) 0x03, (byte) 0x04 });
  }

  public void testIgnoredCharacters() {
    String raw = "!<~>";
    String nonASCII85 = " \t\n\f\r";
    for (int i = 0; i < 6; ++i) {
      StringBuffer buffer = new StringBuffer();
      for (int j = 0; j < raw.length(); ++j) {
        buffer.append(nonASCII85);
        buffer.append(raw.charAt(j));
      }
      checkArrays(buffer.toString().getBytes(),
                  new byte[] { (byte) 0x01 });
      nonASCII85 = nonASCII85 + nonASCII85;
    }
  }

  public void testRoundtripOneByte() {
    for (int a = 0; a < 256; ++a) {
      byte[] binary = new byte[] { (byte) a };
      checkArrays(encodeBytes(binary), binary);
    }
  }

  public void testRoundtripTwoBytes() {
    for (int a = 0; a < 256; ++a) {
      for (int b = 0; b < 256; ++b) {
        byte[] binary = new byte[] { (byte) a, (byte) b };
        checkArrays(encodeBytes(binary), binary);
      }
    }
  }

  public void testBug20060427() {
    checkArrays("M3d$~>".getBytes(),
                new byte[] {
                  (byte) 0x89, (byte) 0x96, (byte) 0xDF
                });
  }

  public void testSimple() {
    checkArrays("+,^C):5M,#!WrN)z!!!!!~>".getBytes(),
                new byte[] {
                  (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00,
                  (byte) 0x4e, (byte) 0x89, (byte) 0x3d, (byte) 0x42,
                  (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                });
  }

  public void testWikipedia() {
    checkArrays(("9jqo^BlbD-BleB1DJ+*+F(f,q/0JhKF<GL>Cj@.4Gp$d7F!,L7@<6@)/0JD"
               + "EF<G%<+EV:2F!,O<DJ+*.@<*K0@<6L(Df-\\0Ec5e;DffZ(EZee.Bl.9pF\""
               + "AGXBPCsi+DGm>@3BB/F*&OCAfu2/AKYi(DIb:@FD,*)+C]U=@3BN#EcYf8AT"
               + "D3s@q?d$AftVqCh[NqF<G:8+EV:.+Cf>-FD5W8ARlolDIal(DId<j@<?3r@:"
               + "F%a+D58'ATD4$Bl@l3De:,-DJs`8ARoFb/0JMK@qB4^F!,R<AKZ&-DfTqBG%"
               + "G>uD.RTpAKYo'+CT/5+Cei#DII?(E,9)oF*2M7/c~>").getBytes(),
                ("Man is distinguished, not only by his reason, but by this"
               + " singular passion from other animals, which is a lust of the"
               + " mind, that by a perseverance of delight in the continued"
               + " and indefatigable generation of knowledge, exceeds the short"
               + " vehemence of any carnal pleasure.").getBytes());
  }

  private void checkArrays(byte[] encoded, byte[] reference) {
    try {
      byte[] decoded = decodeBytes(encoded);
      assertEquals(reference.length, decoded.length);
      for (int i = 0; i < reference.length; ++i) {
        assertEquals(reference[i], decoded[i]);
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private byte[] decodeBytes(byte[] bytes)
  throws IOException {
    
    byte[] decoded = new byte[0];
    
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    ASCII85Decoder filter = new ASCII85Decoder(in);
    byte[] tmp = new byte[512];
    for (int n = filter.read(tmp, 0, tmp.length);
    n >= 0;
    n = filter.read(tmp, 0, tmp.length)) {
      if (n > 0) {
        byte[] newDecoded = new byte[decoded.length + n];
        System.arraycopy(decoded, 0, newDecoded, 0, decoded.length);
        System.arraycopy(tmp, 0, newDecoded, decoded.length, n);
        decoded = newDecoded;
      }
    }
    filter.close();
    
    return decoded;
    
  }

  private byte[] encodeBytes(byte[] bytes) {

    ByteArrayOutputStream out = new ByteArrayOutputStream();
 
    try {
      ASCII85Encoder filter = new ASCII85Encoder(out);
      filter.write(bytes);
      filter.close();
    } catch (IOException e) {
      fail("unexpected exception: " + e);
    }
    
    return out.toByteArray();
    
  }
  
  public static Test suite() {
    return new TestSuite(ASCII85DecoderTest.class);
  }

}
