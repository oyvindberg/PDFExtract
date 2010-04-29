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

public class Base64EncoderTest extends TestCase {
  
  public Base64EncoderTest(String name) {
    super(name);
  }

  public void testNoBytes() {
    byte[] encoded = encodeBytes(new byte[0]);
    assertEquals(0, encoded.length);
  }
  
  public void testBase64Alphabet() {
    for (int i = 0; i < 256; ++i) {
      byte[] encoded = encodeBytes(new byte[] { (byte) i });
      assertEquals(4, encoded.length);
      boolean atEnd = false;
      for (int j = 0; j < encoded.length; ++j) {
        if (atEnd) {
          assertEquals('=', (char) encoded[j]);
        } else if (((char) encoded[j]) == '=') {
          atEnd = true;
        } else {
          assertTrue(("ABCDEFGHIJKLMNOPQRSTUVWXYZ".indexOf(encoded[j]) >= 0)
                     || ("abcdefghijklmnopqrstuvwxyz".indexOf(encoded[j]) >= 0)
                     || ("0123456789+/".indexOf(encoded[j]) >= 0));
        }
      }
    }
  }
  
  public void testFinalQuantum() {
    checkArrays(new byte[] { (byte) 0x01 },
                "AQ==".getBytes());
    checkArrays(new byte[] { (byte) 0x01, (byte) 0x02 },
                "AQI=".getBytes());
    checkArrays(new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 },
                "AQID".getBytes());
    checkArrays(new byte[] { (byte) 0x01, (byte) 0x02,
                             (byte) 0x03, (byte) 0x04 },
                "AQIDBA==".getBytes());
  }

  public void testSimple() {
    checkArrays(new byte[] {
                  (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00,
                  (byte) 0x4e, (byte) 0x89, (byte) 0x3d, (byte) 0x42,
                  (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                },
                "H4sIAE6JPUICAwMAAAAAAAAAAAA=".getBytes());
  }

  public void testRFC3548() {
    checkArrays(new byte[] {
                  (byte) 0x14, (byte) 0xfb, (byte) 0x9c,
                  (byte) 0x03, (byte) 0xd9, (byte) 0x7e
                },
                "FPucA9l+".getBytes());
    checkArrays(new byte[] {
                  (byte) 0x14, (byte) 0xfb, (byte) 0x9c,
                  (byte) 0x03, (byte) 0xd9
                },
                "FPucA9k=".getBytes());
    checkArrays(new byte[] {
                  (byte) 0x14, (byte) 0xfb, (byte) 0x9c, (byte) 0x03
                },
                "FPucAw==".getBytes());
  }

  public void testSplitLines() {

    byte[] raw = new byte[256];
    for (int i = 0; i < raw.length; ++i) {
      raw[i] = (byte) i;
    };

    for (int l = 4; l < 128; ++l) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      try {
        Base64Encoder filter = new Base64Encoder(out, l,
                                                 "   ".getBytes(),
                                                 "\n".getBytes());
        filter.write(raw);
        filter.close();
      } catch (IOException e) {
        fail("unexpected exception: " + e);
      }
      assertEquals(4 * (87 + 343 / (l - 3)), out.toByteArray().length);
    }

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
      Base64Encoder filter = new Base64Encoder(out);
      filter.write(bytes);
      filter.close();
    } catch (IOException e) {
      fail("unexpected exception: " + e);
    }
    
    return out.toByteArray();
    
  }
  
  public static Test suite() {
    return new TestSuite(Base64EncoderTest.class);
  }

}
