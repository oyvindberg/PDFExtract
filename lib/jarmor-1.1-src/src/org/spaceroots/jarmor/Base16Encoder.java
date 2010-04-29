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

 * <p>The Base16 encoding is suitable when binary data needs to be
 * transmitted or stored as text, when case insensitivity is needed.
 * It is defined in <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC
 * 3548</a> <i>The Base16, Base32, and Base64 Data Encodings</i>
 * by S. Josefsson</p>

 * <p>If strict RFC 3548 compliance is specified (by using the one
 * argument {@link #Base16Encoder(OutputStream) constructor}),
 * the produced stream is guaranteed to use only the upper case letters
 * 'A' to 'F' and the digits '0' to '9'.</p>

 * <p>If the encoded text must obey higher level requirement, the user can
 * ask for the encoded text to be split in lines by specifying a maximal
 * line length, an end of line marker and an optional start of line marker
 * (for indentation purposes for example) by using the four arguments
 * {@link #Base16Encoder(OutputStream,int,byte[],byte[]) constructor}. In
 * this case, the markers <em>must not</em> belong to the Base16 alphabet,
 * and the corresponding decoder <em>must</em> be set up to silently
 * ignore these characters.</p>

 * <p>Converting the Base16 encoded data stream into text should be
 * straigthforward regardless of the character set as according to the
 * RFC the alphabet used (perhaps with the exception of the start/end of
 * line markers if the user is not cautious) has the same representation
 * in all versions of ISO 646, US-ASCII, and all versions of EBCDIC.</p>

 * <p>The encoded stream is about twice as large than the
 * corresponding binary stream (4 binary bits are converted into 8
 * encoded bits, and there may be start/end of line markers).</p>

 * @author Luc Maisonobe
 * @see Base16Decoder
 */
public class Base16Encoder extends FilterOutputStream {

  /** Create an encoder wrapping a sink of binary data.
   * <p>The encoder built using this constructor will strictly
   * obey <a href="http://www.ietf.org/rfc/rfc3548.txt">RFC 3548</a>.</p>
   * <p>Note that calling this constructor is equivalent to calling
   * {@link #Base16Encoder(OutputStream,int,byte[],byte[])
   * Base16Encoder(<code>out</code>, <code>-1</code>, <code>null</code>,
   * <code>null</code>)}.</p>
   * @param out sink of binary data to filter
   */
  public Base16Encoder(OutputStream out) {
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
   * {@link #Base16Encoder(OutputStream) constructor}.</p>
   * <p>If non-null start/end of line are used, they must be free of any
   * Base16 characters that would otherwise interfere with the decoding
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
  public Base16Encoder(OutputStream out,
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
    putByte(code[(b >> 4) & 0xF]);
    putByte(code[b & 0xF]);
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

  /** Current length of the line being written. */
  private int length;

  /** Encoding array. */
  private static final int[] code = {
    '0', '1', '2', '3', '4', '5', '6', '7',
    '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
  };

}
