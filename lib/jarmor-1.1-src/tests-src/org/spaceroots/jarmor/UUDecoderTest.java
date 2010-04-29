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

public class UUDecoderTest extends TestCase {

  public UUDecoderTest(String name) {
    super(name);
  }

  public void testNoHeader() {
    try {
      decodeBytes("!`0``\n`\nend\n".getBytes(), null, -1, -1, -1);
      fail("an exception should have been thrown");
    } catch (IOException e) {
      // expected behaviour
    } catch (Exception e) {
      fail("wrong exception caught");
    }
  }

  public void testNoTrailer() {
    try {
      decodeBytes("begin 644 one\n!`0``\n`\n".getBytes(),
                  "one", 6, 4, 4);
      fail("an exception should have been thrown");
    } catch (IOException e) {
      // expected behaviour
    } catch (Exception e) {
      fail("wrong exception caught");
    }
  }

  public void testFinalQuantum() {
    checkArrays("begin 644 one\n!`0``\n`\nend\n".getBytes(),
                new byte[] { (byte) 0x01 },
                "one", 6, 4, 4);
    checkArrays("begin 644 two\n\"`0(`\n`\nend\n".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02 },
                "two", 6, 4, 4);
    checkArrays("begin 644 three\n#`0(#\n`\nend\n".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02, (byte) 0x03 },
                "three", 6, 4, 4);
    checkArrays("begin 644 four\n$`0(#!```\n`\nend\n".getBytes(),
                new byte[] { (byte) 0x01, (byte) 0x02,
                             (byte) 0x03, (byte) 0x04 },
                "four", 6, 4, 4);
  }

  public void testSimple() {
    checkArrays(("begin 644 empty.gz\n4'XL(`$Z)/"
              + "4(\"`P,`````````````\n`\nend\n").getBytes(),
                new byte[] {
                  (byte) 0x1f, (byte) 0x8b, (byte) 0x08, (byte) 0x00,
                  (byte) 0x4e, (byte) 0x89, (byte) 0x3d, (byte) 0x42,
                  (byte) 0x02, (byte) 0x03, (byte) 0x03, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                  (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
                },
                "empty.gz", 6, 4, 4);
  }

  private void checkArrays(byte[] encoded,
                           byte[] reference, String name,
                           int userPerms, int groupPerms, int othersPerms) {
    try {
      byte[] decoded = decodeBytes(encoded,
                                   name, userPerms, groupPerms, othersPerms);
      assertEquals(reference.length, decoded.length);
      for (int i = 0; i < reference.length; ++i) {
        assertEquals(reference[i], decoded[i]);
      }
    } catch (IOException e) {
      fail(e.getMessage());
    }
  }

  private byte[] decodeBytes(byte[] bytes, String name,
                             int userPerms, int groupPerms, int othersPerms)
  throws IOException {
    
    byte[] decoded = new byte[0];
    
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    UUDecoder filter = new UUDecoder(in);
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

    assertEquals(name,        filter.getName());
    assertEquals(userPerms,   filter.getUserPerms());
    assertEquals(groupPerms,  filter.getGroupPerms());
    assertEquals(othersPerms, filter.getOthersPerms());
    
    return decoded;
    
  }

  public static Test suite() {
    return new TestSuite(UUDecoderTest.class);
  }

}
