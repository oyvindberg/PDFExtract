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

 * <p>The UU encoding is suitable when binary data needs to be
 * transmitted or stored as text. It was used to send binary data
 * through UUCP (Unix to Unix Copy Protocol). The encoded text uses a
 * subset of the ASCII character set.</p>

 * <p>The encoded stream is about 38% larger than the corresponding
 * binary stream (6 binary bits are converted into 8 encoded bits,
 * there are stream header and trailer, and each 60 encoded bytes
 * chunk is started by a control character and ended by a
 * newline).</p>

 * @author Luc Maisonobe
 * @see UUDecoder
 */
public class UUEncoder extends FilterOutputStream {

  /** Bit-pattern for READ permission. */
  public static final int READ = 4;

  /** Bit-pattern for WRITE permission. */
  public static final int WRITE = 2;

  /** Bit-pattern for EXECUTE permission. */
  public static final int EXECUTE = 1;

  /** Create an encoder wrapping a sink of binary data.
   * <p>The encoder built will have a default name of "decoded.data"
   * and default permissions set to <code>READ|WRITE</code> for the
   * user, <code>READ</code> for the group and <code>READ</code> for
   * others.</p>
   * @param out sink of binary data to filter
   */
  public UUEncoder(OutputStream out) {
    this(out, "decoded.data", READ|WRITE, READ, READ);
  }

  /** Create an encoder wrapping a sink of binary data.
   * <p>The name and the various permission listed here are relevant
   * if the data is to be stored on a file once decoded. The permissions
   * are specified by or-ing together the desired bits patterns
   * {@link #READ}, {@link #WRITE} and {@link #EXECUTE}.</p>
   * @param out sink of binary data to filter
   * @param name name of the data file
   * @param userPerms access permission for the user (owner)
   * @param groupPerms access permission for the group
   * @param othersPerms access permission for others
   */
  public UUEncoder(OutputStream out, String name,
                   int userPerms, int groupPerms, int othersPerms) {
    super(out);
    this.name        = name;
    this.userPerms   = userPerms   & 0x7;
    this.groupPerms  = groupPerms  & 0x7;
    this.othersPerms = othersPerms & 0x7;
    headerPending    = true;
    buffer = new int[45];
  }

  /** Closes this output stream and releases any system resources
   * associated with the stream.
   * @exception IOException if the underlying stream throws one
   */
  public void close()
    throws IOException {

    if (headerPending) {
      outputHeader();
    }

    // end the last line properly
    if (length != 0) {
      outputLine();
    }

    // output the file trailer
    out.write("`\nend\n".getBytes());

    // close the underlying stream
    out.close();

  }

  /** Writes the specified byte to this output stream.
   * @param b byte to write (only the 8 low order bits are used)
   */
  public void write(int b)
    throws IOException {
    buffer[length++] = b;
    if (length == buffer.length) {
      if (headerPending) {
        outputHeader();
      }
      outputLine();
    }
  }

  /** Output the file header.
   * @exception IOException if the underlying stream throws one
   */
  private void outputHeader()
  throws IOException {
    out.write("begin ".getBytes());
    out.write(Integer.toString(userPerms).getBytes());
    out.write(Integer.toString(groupPerms).getBytes());
    out.write(Integer.toString(othersPerms).getBytes());
    out.write(' ');
    out.write(name.getBytes());
    out.write('\n');
    headerPending = false;
  }

  /** Output a completed encoded line.
   * @exception IOException if the underlying stream throws one
   */
  private void outputLine()
    throws IOException {

    // output the encoded length of the line
    putByte(length);
    
    // complete last quantum if needed
    switch (length % 3) {
    case 1:
      buffer[length++] = 0;
      // fall through ...
    case 2:
      buffer[length++] = 0;
    }

    // encode the line
    for (int i = 0, phase = 0; i < length; ++i) {
      switch (phase) {
      case 0 :
        putByte(((buffer[i] & 0xFC) >> 2));
        phase = 1;
        break;
      case 1 :
        putByte(((buffer[i-1] & 0x03) << 4) | ((buffer[i] & 0xF0) >> 4));
        phase = 2;
        break;
      default :
        putByte(((buffer[i-1] & 0x0F) << 2) | ((buffer[i] & 0xC0) >> 6));
        putByte(buffer[i] & 0x3F);
        phase = 0;
        break;
      }
    }

    // output the end of line
    out.write('\n');

    length = 0;

  }

  /** Produce one encoded byte.
   * @param b byte to output
   */
  private void putByte(int b)
    throws IOException {
    out.write((b == 0) ? '`' : (32 + b));
  }

  /** Name of the data file. */
  private String name;
  
  /** Access permission for the user (owner). */
  private int userPerms;
  
  /** Access permission for the group. */
  private int groupPerms;
  
  /** Access permission for others. */
  private int othersPerms;

  /** Indicator for header written status. */
  private boolean headerPending;

  /** Current length of the line being written. */
  private int length;

  /** Buffer holding the 45 bytes building the next line to output. */
  private int[] buffer;

}
