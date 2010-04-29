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

import java.io.IOException;
import java.io.InputStream;


/** This class decodes a text stream containing into a binary stream.

 * <p>The ASCII85encoding is suitable when binary data needs to be
 * transmitted or stored as text. It has been defined by Adobe for
 * the PostScript and PDF formats (see PDF Reference, section 3.3
 * Details of Filtered Streams).</p>

 * <p>The encoded stream is about 25% larger than the
 * corresponding binary stream (32 binary bits are converted into 40
 * encoded bits, and there may be start/end of line markers).</p>

 * @author Luc Maisonobe
 * @see ASCII85Encoder
 */
public class ASCII85Decoder extends AbstractDecoder {

  /** Create a decoder wrapping a source of encoded data.
   * @param in source of encoded data to decode
   */
  public ASCII85Decoder(InputStream in) {
    super(in);
    phase        = 0;
    previousWasZ = false;
   }

  /** Filter some bytes from the underlying stream.
   * @return number of bytes inserted in the filtered bytes buffer or -1
   * @exception IOException if the underlying stream throws one or
   * if a non-ASCII85 character is encountered
   */
  protected int filterBytes() throws IOException {

    if (nEncoded < 1) {
      if (readEncodedBytes() < 0) {
        return -1;
      }
    }

    // bytes decoding loop
    int inserted = 0;
    for (int i = 0; i < nEncoded; ++i) {

      int current = encoded[i] & 0xFF;

      if (current == 'z') {

        if (phase != 0) {
          throw new IOException("forbidden 'z' encoding in the middle of a block");
        }

        // special encoding of four null bytes
        previousWasZ = true;
        putFilteredByte(0);
        putFilteredByte(0);
        putFilteredByte(0);
        putFilteredByte(0);
        inserted += 4;

      } else if ((current != '\0') && (current != '\t')
              && (current != '\f') && (current != '\r')
              && (current != '\n') && (current != ' ')) {
         // this is not a whitespace character

        // it must be either an ASCII85 character or the end marker
        if ((current < '!') || (current > 'u')) {
          if (current == '~') {

            do {
              // skip whitespace characters
              if (++i < nEncoded) {
                current = encoded[i];
              } else {
                current = in.read() & 0xFF; // beware, -1 is changed into 0xFF
              }
            } while ((current == '\0') || (current == '\t')
                  || (current == '\f') || (current == '\r')
                  || (current == '\n') || (current == ' '));

            if ((phase == 1) || (current != '>')) {
              throw new IOException("malformed last encoded block");
            }
            if (previousWasZ) {
              throw new IOException("forbidden 'z' encoding in last block");
            }

            if (phase > 0) {
              b3 += b4 >> 8;
              b2 += b3 >> 8;
              b1 += b2 >> 8;
              b4 &= 0xFF;
              b3 &= 0xFF;
              b2 &= 0xFF;
              b1 &= 0xFF;
              switch (phase) {
              case 2:
                if ((b4 + b3 + b2) > 0) {
                  b1 += 1;
                }
                putFilteredByte(b1);
                inserted += 1;
                break;
              case 3:
                if ((b4 + b3) > 0) {
                  b2 += 1;
                }
                b1 += b2 >> 8;
                putFilteredByte(b1);
                putFilteredByte(b2 & 0xFF);
                inserted += 2;
                break;
              case 4:
                if (b4 > 0) {
                  b3 += 1;
                }
                b2 += b3 >> 8;
                b1 += b2 >> 8;
                putFilteredByte(b1);
                putFilteredByte(b2 & 0xFF);
                putFilteredByte(b3 & 0xFF);
                inserted += 3;
                break;
              default:
                // nothing to do
              }
            }

            endReached = true;
            return inserted;

          }
          throw new IOException("non ASCII85 character: " + current);
        }

        // this is a regular character, subtract the common offset
        current -= 33;

        // combine the various "85-gits" to produce 8-bits bytes
        switch (phase) {
        case 0:
          // 0x031C84B1 is 85^4
          b1    = 0x03 * current;
          b2    = 0x1C * current;
          b3    = 0x84 * current;
          b4    = 0xB1 * current;
          phase = 1;
          previousWasZ = false;
          break;
        case 1:
          // 0x00095EED is 85^3
          b2   += 0x09 * current;
          b3   += 0x5E * current;
          b4   += 0xED * current;
          phase = 2;
          break;
        case 2:
          // 0x00001C39 is 85^2
          b3   += 0x1C * current;
          b4   += 0x39 * current;
          phase = 3;
          break;
        case 3:
          // 0x00000055 is 85^1
          b4   += 0x55 * current;
          phase = 4;
          break;
       default:
          b4 += current;
          b3 += b4 >> 8;
          b2 += b3 >> 8;
          b1 += b2 >> 8;
          putFilteredByte(b1);
          putFilteredByte(b2 & 0xFF);
          putFilteredByte(b3 & 0xFF);
          putFilteredByte(b4 & 0xFF);
          inserted += 4;
          phase = 0;
        }
      }
    }

    nEncoded = 0;
    return inserted;

  }

  /** Coefficients of the 32-bits quantum in base 256. */
  private int b1;
  private int b2;
  private int b3;
  private int b4;

  /** Phase (modulo 5) of encoded bytes read. */
  private int phase;

  /** Indicator of 'z' special encoding. */
  private boolean previousWasZ;

}
