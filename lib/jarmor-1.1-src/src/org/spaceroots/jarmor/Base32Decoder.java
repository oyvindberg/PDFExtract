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

import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;


/** This class decodes a text stream containing into a binary stream.

 * <p>The Base32 encoding is suitable when binary data needs to be
 * transmitted or stored as text, when case insensitivity is needed.
 * It is defined in <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC
 * 3548</a> <i>The Base16, Base32, and Base64 Data Encodings</i>
 * by S. Josefsson</p>

 * @author Luc Maisonobe
 * @see Base32Encoder
 */
public class Base32Decoder extends AbstractDecoder {

  /** Create a decoder wrapping a source of encoded data.
   * <p>The decoder built using this constructor will strictly
   * obey <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC 3548</a>.
   * This means that if some encoded bytes do not belong to the Base32
   * alphabet, an <code>IOException</code> will be thrown at read time.</p>
   * <p>Note that calling this constructor is equivalent to calling
   * {@link #Base32Decoder(InputStream,boolean)
   * Base32Decoder(<code>in</code>, <code>-true</code>)}.</p>
   * @param in source of encoded data to decode
   */
  public Base32Decoder(InputStream in) {
    super(in);
    strictRFCCompliance = true;
    phase = 0;
  }

  /** Create a decoder wrapping a source of encoded data.
   * <p>If the decoder built using this constructor strictly
   * obeys <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC 3548</a>
   * and some encoded bytes do not belong to the Base32 alphabet, then
   * an <code>IOException</code> will be thrown at read time.</p>
   * <p>Note that calling this constructor with
   * <code>strictRFCCompliance</code>set to true is equivalent to calling
   * the one argument {@link #Base32Decoder(InputStream) constructor}.</p>
   * @param in source of encoded data to decode
   * @param strictRFCCompliance if true, characters outside of the Base32
   * alphabet will trigger an <code>IOException</code> at read time,
   * otherwise they will be silently ignored
   */
  public Base32Decoder(InputStream in, boolean strictRFCCompliance) {
    super(in);
    this.strictRFCCompliance = strictRFCCompliance;
     phase = 0;
  }

  /** Filter some bytes from the underlying stream.
   * @return number of bytes inserted in the filtered bytes buffer or -1 if
   * the underlying stream has no bytes left
   * @exception IOException if the underlying stream throws one
   */
  protected int filterBytes()
    throws IOException {

    if (nEncoded < 1) {
      if (readEncodedBytes() < 0) {
        return -1;
      }
    }

    // bytes decoding loop
    int inserted = 0;
    for (int i = phase; i < nEncoded; ++i) {
      if (encoded[i] == '=') {
        endReached = true;
        return inserted;
      } else if (decode[encoded[i]] < 0) {
        if (strictRFCCompliance) {
          throw new IOException("non-Base32 character read in strict"
                                + " RFC 3548 compliance mode");
        }
      } else {
        // this is a Base32 character
        int current = decode[encoded[i]];

        // combine the various 5-bits bytes to produce 8-bits bytes
        switch (phase) {
        case 0:
          b0    = current;
          phase = 1;
          break;
        case 1:
          b1    = current;
          phase = 2;
          putFilteredByte(((b0 & 0x1F) << 3) | ((b1 & 0x1C) >> 2));
          ++inserted;
          break;
        case 2:
          b2    = current;
          phase = 3;
          break;
        case 3:
          b3    = current;
          phase = 4;
          putFilteredByte(((b1 & 0x03) << 6) | ((b2 & 0x1F) << 1)
                        | ((b3 & 0x10) >> 4));
          ++inserted;
          break;
        case 4:
          b4    = current;
          phase = 5;
          putFilteredByte(((b3 & 0x0F) << 4) | ((b4 & 0x1E) >> 1));
          ++inserted;
          break;
        case 5:
          b5    = current;
          phase = 6;
          break;
        case 6:
          b6    = current;
          phase = 7;
          putFilteredByte(((b4 & 0x01) << 7) | ((b5 & 0x1F) << 2)
                        | ((b6 & 0x18) >> 3));
          ++inserted;
          break;
       default:
          phase = 0;
          putFilteredByte(((b6 & 0x07) << 5) | (current & 0x1F));
          ++inserted;
        }
      }
    }

    nEncoded = 0;
    return inserted;

  }

  /** RFC 3548 compliance indicator. */
  private boolean strictRFCCompliance;

  /** Phase (modulo 8) of encoded bytes read. */
  private int phase;

  /** First bytes of the current quantum. */
  private int b0;
  private int b1;
  private int b2;
  private int b3;
  private int b4;
  private int b5;
  private int b6;

  /** Decoding array. */
  private static final int[] decode = new int[255];

  static {
    int[] code = new int[] {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
      'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
      'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
      'Y', 'Z', '2', '3', '4', '5', '6', '7'
    };
    Arrays.fill(decode, -1);
    for (int i = 0; i < code.length; ++i) {
      decode[code[i]] = i;
      if (Character.isLetter((char) code[i])) {
        decode[Character.toLowerCase((char) code[i])] = i;
      }
    }
  }

}
