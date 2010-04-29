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

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

/** This class encodes a binary stream into a text stream.

 * <p>The Base32 encoding is suitable when binary data needs to be
 * transmitted or stored as text, when case insensitivity is needed.
 * It is defined in <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC
 * 3548</a> <i>The Base16, Base32, and Base64 Data Encodings</i>
 * by S. Josefsson</p>

 * <p>If strict RFC 3548 compliance is specified (by using the one
 * argument {@link #Base32Encoder(OutputStream) constructor}),
 * the produced stream is guaranteed to use only the upper case letters
 * 'A' to 'Z', the digits '2' to '7', and the '=' padding character.</p>

 * <p>If the encoded text must obey higher level requirement, the user can
 * ask for the encoded text to be split in lines by specifying a maximal
 * line length, an end of line marker and an optional start of line marker
 * (for indentation purposes for example) by using the four arguments
 * {@link #Base32Encoder(OutputStream,int,byte[],byte[]) constructor}. In
 * this case, the markers <em>must not</em> belong to the Base32 alphabet,
 * and the corresponding decoder <em>must</em> be set up to silently
 * ignore these characters.</p>

 * <p>Converting the Base32 encoded data stream into text should be
 * straigthforward regardless of the character set as according to the
 * RFC the alphabet used (perhaps with the exception of the start/end of
 * line markers if the user is not cautious) has the same representation
 * in all versions of ISO 646, US-ASCII, and all versions of EBCDIC.</p>

 * <p>The encoded stream is about 60% larger than the corresponding
 * binary stream (5 binary bits are converted into 8 encoded bits, there
 * may be up to additional 6 padding bytes, and there may be start/end of
 * line markers).</p>

 * @author Luc Maisonobe
 * @see Base32Decoder
 */
public class Base32Encoder extends FilterOutputStream {

  /** Create an encoder wrapping a sink of binary data.
   * <p>The encoder built using this constructor will strictly
   * obey <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC 3548</a>.</p>
   * <p>Note that calling this constructor is equivalent to calling
   * {@link #Base32Encoder(OutputStream,int,byte[],byte[])
   * Base32Encoder(<code>out</code>, <code>-1</code>, <code>null</code>,
   * <code>null</code>)}.</p>
   * @param out sink of binary data to filter
   */
  public Base32Encoder(OutputStream out) {
    super(out);
    lineLength = -1;
  }

  /** Create an encoder wrapping a sink of binary data.
   * <p>The encoder built using this constructor will <em>not</em> strictly
   * obey <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC 3548</a>. The
   * corresponding decoder must be aware of the settings used here to
   * properly ignore the start/end of line markers.</p>
   * <p>Note that specifying a negative number for <code>lineLength</code>
   * is really equivalent to calling the one argument
   * {@link #Base32Encoder(OutputStream) constructor}.</p>
   * <p>If non-null start/end of line are used, they must be free of any
   * Base32 characters that would otherwise interfere with the decoding
   * process on the other side of the channel. For safety, it is recommended
   * to stick to space (' ', 0x32) and horizontal tabulation ('\t', 0x9)
   * characters for the start of line marker, and to line feed ('\n', 0xa) and
   * carriage return ('\r', 0xd) characters according to the platform convention
   * for the end of line marker.</p>
   * @param out sink of binary data to filter
   * @param lineLength maximal length of a ligne (counting <code>sol</code>
   * but not counting <code>eol</code>), if negative lines will not be
   * split
   * @param sol start of line marker to use (mainly for indentation
   * purposes), may be null
   * @param eol end of line marker to use, may be null only if
   * <code>lineLength</code> is negative
   */
  public Base32Encoder(OutputStream out,
                       int lineLength, byte[] sol, byte[] eol) {
    super(out);
    this.lineLength = lineLength;
    this.sol        = sol;
    this.eol        = eol;
  }

  /** Closes this output stream and releases any system resources
   * associated with the stream.
   * @exception IOException if the underlying stream throws one
   */
  public void close()
    throws IOException {

    switch (counter) {
    case 0:
      // nothing to do
      break;
    case 1:
      // the last quantum contains one byte, the first of its eight
      // bytes encoded counterpart has already been produced from its
      // 5 most significant bits, the remaining 3 bits must be encoded
      // to form the second of the eight bytes encoded counterpart, and
      // the six last bytes must be set to the '=' padding character
      putByte(code[(last << 2) & 0x1F]);
      putByte('='); putByte('='); putByte('=');
      putByte('='); putByte('='); putByte('=');
      break;
    case 2:
      // the last quantum contains two bytes, the first three of its
      // eight bytes encoded counterpart have already been produced
      // from its 15 most significant bits, the remaining 1 bit must
      // be encoded to form the fourth of the eight bytes encoded
      // counterpart, and the four last bytes must be set to the '='
      // padding character
      putByte(code[(last << 4) & 0x1F]);
      putByte('='); putByte('=');
      putByte('='); putByte('=');
      break;
    case 3:
      // the last quantum contains three bytes, the first four of its
      // eight bytes encoded counterpart have already been produced
      // from its 20 most significant bits, the remaining 4 bits must
      // be encoded to form the fifth of the eight bytes encoded
      // counterpart, and the three last bytes must be set to the '='
      // padding character
      putByte(code[(last << 1) & 0x1F]);
      putByte('='); putByte('='); putByte('=');
      break;
    default:
      // the last quantum contains four bytes, the first six of its
      // eight bytes encoded counterpart have already been produced
      // from its 30 most significant bits, the remaining 2 bits must
      // be encoded to form the seventh of the eight bytes encoded
      // counterpart, and the last byte must be set to the '='
      // padding character
      putByte(code[(last << 3) & 0x1F]);
      putByte('=');
    }

    // end the last line properly
    if (length != 0) {
      out.write(eol, 0, eol.length);
    }

    // close the underlying stream
    out.close();

  }

  /** Writes the specified byte to this output stream.
   * @param b byte to write (only the 8 low order bits are used)
   */
  public void write(int b)
    throws IOException {

    switch (counter) {
    case 0:
      // this byte starts a new 40-bits quantum
      putByte(code[(b >> 3) & 0x1F]);
      counter = 1;
      break;
    case 1:
      // this byte continues an already started 40-bits quantum
      putByte(code[((last & 0x07) << 2) | ((b & 0xC0) >> 6)]);
      putByte(code[(b >> 1) & 0x1F]);
      counter = 2;
      break;
    case 2:
      // this byte continues an already started 40-bits quantum
      putByte(code[((last & 0x01) << 4) | ((b & 0xF0) >> 4)]);
      counter = 3;
      break;
    case 3:
      // this byte continues an already started 40-bits quantum
      putByte(code[((last & 0x0F) << 1) | ((b & 0x80) >> 7)]);
      putByte(code[(b >> 2) & 0x1F]);
      counter = 4;
      break;
    default:
      // this byte ends a 40-bits quantum
      putByte(code[((last & 0x03) << 3) | ((b & 0xE0) >> 5)]);
      putByte(code[b & 0x1F]);
      counter = 0;
    }

    last = b;

  }

  /** Put a byte in the underlying stream, inserting line breaks as
   * needed.
   * @param b byte to put in the underlying stream (only the 8 low
   * order bits are used)
   * @exception IOException if the underlying stream throws one
   */
  private void putByte(int b)
    throws IOException {
    if (lineLength >= 0) {
      // split encoded lines if needed
      if ((length == 0) && (sol != null)) {
        out.write(sol, 0, sol.length);
        length = sol.length;
      }
      out.write(b);
      if (++length >= lineLength) {
        out.write(eol, 0, eol.length);
        length = 0;
      }
    } else {
      // strictly adhere to RFC 3548
      out.write(b);
    }
  }

  /** Line length (not counting eol). */
  private int lineLength;

  /** Start of line marker (indentation). */
  private byte[] sol;

  /** End Of Line marker. */
  private byte[] eol;

  /** Last accepted byte (may contain pending bits). */
  private int last;

  /** Counter (modulo 5) of accepted bytes. */
  private int counter;

  /** Current length of the line being written. */
  private int length;

  /** Encoding array. */
  private static final int[] code = {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
    'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
    'Y', 'Z', '2', '3', '4', '5', '6', '7'
  };

}
