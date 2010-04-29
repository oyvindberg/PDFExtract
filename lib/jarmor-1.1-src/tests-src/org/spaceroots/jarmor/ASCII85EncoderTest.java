// Copyright (c) 2005, Luc Maisonobe
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ASCII85EncoderTest extends TestCase {

  public ASCII85EncoderTest(String name) {
    super(name);
  }

  public void testNoBytes() {
    byte[] encoded = encodeBytes(new byte[0]);
    assertEquals(0, encoded.length);
  }
  
  public void testASCII85Alphabet() {
    for (int i = 0; i < 256; ++i) {
      byte[] encoded = encodeBytes(new byte[] { (byte) i });
      assertEquals(4, encoded.length);
      for (int j = 0; j < (encoded.length - 2); ++j) {
        assertTrue(("!\"#$%&'()*+,-./0123456789:;<=>?@"
                  + "ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`"
                  + "abcdefghijklmnopqrstu").indexOf(encoded[j]) >= 0);
      }
      assertEquals('~', encoded[2]);
      assertEquals('>', encoded[3]);
    }
  }
  
  public void testFinalQuantum() {
    checkArrays(new byte[] { (byte) 0x01 },
                "!<~>".getBytes());
    checkArrays(new byte[] { (byte) 0x01, (byte) 0x02 },
                "!<N~>".getBytes());
    checkArrays(new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 },
                "!<N?~>".getBytes());
    checkArrays(new byte[] { (byte) 0x01, (byte) 0x02,
                             (byte) 0x03, (byte) 0x04 },
                "!<N?+~>".getBytes());
  }

  public void testSimple() {
    checkArrays(new byte[] {
                  (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00,
                  (byte) 0x4e, (byte) 0x89, (byte) 0x3d, (byte) 0x42,
                  (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                },
                "+,^C):5M,#!WrN)z!!!!!~>".getBytes());
  }

  public void testWikipedia() {
    checkArrays(("Man is distinguished, not only by his reason, but by this"
        + " singular passion from other animals, which is a lust of the"
        + " mind, that by a perseverance of delight in the continued"
        + " and indefatigable generation of knowledge, exceeds the short"
        + " vehemence of any carnal pleasure.").getBytes(),
        ("9jqo^BlbD-BleB1DJ+*+F(f,q/0JhKF<GL>Cj@.4Gp$d7F!,L7@<6@)/0JD"
       + "EF<G%<+EV:2F!,O<DJ+*.@<*K0@<6L(Df-\\0Ec5e;DffZ(EZee.Bl.9pF\""
       + "AGXBPCsi+DGm>@3BB/F*&OCAfu2/AKYi(DIb:@FD,*)+C]U=@3BN#EcYf8AT"
       + "D3s@q?d$AftVqCh[NqF<G:8+EV:.+Cf>-FD5W8ARlolDIal(DId<j@<?3r@:"
       + "F%a+D58'ATD4$Bl@l3De:,-DJs`8ARoFb/0JMK@qB4^F!,R<AKZ&-DfTqBG%"
       + "G>uD.RTpAKYo'+CT/5+Cei#DII?(E,9)oF*2M7/c~>").getBytes());
  }

  private void checkArrays(byte[] decoded, byte[] reference) {
    byte[] encoded = encodeBytes(decoded);
    assertEquals(reference.length, encoded.length);
    for (int i = 0; i < reference.length; ++i) {
      assertEquals(reference[i], encoded[i]);
    }
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
    return new TestSuite(ASCII85EncoderTest.class);
  }

}
