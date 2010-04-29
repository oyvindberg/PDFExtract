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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class Base32DecoderTest extends TestCase {

  public Base32DecoderTest(String name) {
    super(name);
  }
  
  public void testNoBytes() {
    try {
      assertEquals(0, decodeBytes(new byte[0], true).length);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testNoBase32Bytes() {
    try {
      assertEquals(0, decodeBytes("0189+/\n".getBytes(), false).length);
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  public void testStrictRFCCompliance() {
    try {
      decodeBytes("0189+/\n".getBytes(), true);
      fail("an exception should have been thrown");
    } catch (IOException e) {
      // expected result
    } catch (Exception e) {
      fail("wrong exception caught");
    }
  }

  public void testFinalQuantum() {
    checkArrays("AE======".getBytes(),
                new byte[] { (byte) 0x01 }, true);
    checkArrays("AEBA====".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02 }, true);
    checkArrays("AEBAG===".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 },
                true);
    checkArrays("AEBAGBA=".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03,
                             (byte) 0x04 },
                true);
    checkArrays("AEBAGBAF".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03,
                             (byte) 0x04, (byte) 0x05 },
                true);
  }

  public void testIgnoredCharacters() {
    String raw = "AE======";
    String nonBase32 = " \t\n@[](){}+/,;.0189:\\~&#%*-";
    for (int i = 0; i < 6; ++i) {
      StringBuffer buffer = new StringBuffer();
      for (int j = 0; j < raw.length(); ++j) {
        buffer.append(nonBase32);
        buffer.append(raw.charAt(j));
      }
      checkArrays(buffer.toString().getBytes(),
                  new byte[] { (byte) 0x01 }, false);
      nonBase32 = nonBase32 + nonBase32;
    }
  }

  public void testSimple() {
    checkArrays("D6FQQACORE6UEAQDAMAAAAAAAAAAAAAA".getBytes(),
                new byte[] {
                  (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00,
                  (byte) 0x4e, (byte) 0x89, (byte) 0x3d, (byte) 0x42,
                  (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                }, true);
  }

  public void testMixedCase() {
    checkArrays("D6FQqACOrE6UEAQDAmAAAaaAAAAAAAAA".getBytes(),
                new byte[] {
                  (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00,
                  (byte) 0x4e, (byte) 0x89, (byte) 0x3d, (byte) 0x42,
                  (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                }, true);
  }

  private void checkArrays(byte[] encoded, byte[] reference,
                           boolean strictRFCCompliance) {
    try {
      byte[] decoded = decodeBytes(encoded, strictRFCCompliance);
      assertEquals(reference.length, decoded.length);
      for (int i = 0; i < reference.length; ++i) {
        assertEquals(reference[i], decoded[i]);
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private byte[] decodeBytes(byte[] bytes, boolean strictRFCCompliance)
  throws IOException {
    
    byte[] decoded = new byte[0];
    
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    Base32Decoder filter = new Base32Decoder(in, strictRFCCompliance);
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

  public static Test suite() {
    return new TestSuite(Base32DecoderTest.class);
  }

}
