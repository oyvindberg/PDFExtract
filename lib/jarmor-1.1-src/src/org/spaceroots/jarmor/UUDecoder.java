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

import java.io.IOException;
import java.io.InputStream;



/** This class decodes a text stream containing into a binary stream.

 * <p>The UU encoding is suitable when binary data needs to be
 * transmitted or stored as text. It was used to send binary data
 * through UUCP (Unix to Unix Copy Protocol). The encoded text uses a
 * subset of the ASCII character set.</p>

 * <p>The encoded data is preceded by a header line starting with the
 * keyword <code>begin</code> and it is followed by two trailer lines,
 * the last one starting with the keyword <code>end</code>. Lines preceding
 * the header line or following the trailer lines are allowed and silently
 * ignored.</p>

 * <p>The encoded stream is about 38% larger than the corresponding
 * binary stream (6 binary bits are converted into 8 encoded bits,
 * there are stream header and trailer, and each 60 encoded bytes
 * chunk is started by a control character and ended by a
 * newline).</p>

 * @author Luc Maisonobe
 * @see UUEncoder
 */
public class UUDecoder extends AbstractDecoder {


  /** Create a decoder wrapping a source of encoded data.
   * @param in source of encoded data to decode
   */
  public UUDecoder(InputStream in) {
    super(in);
    headerPending = true;
  }

  /** Get the name of the file.
   * <p>This name is relevant only if the decoded data is to be stored
   * in a file, it is the name that was provided inside the stream by
   * the encoder.</p>
   * @return the name of the file
   */
  public String getName() {
    return name;
  }

  /** Get the access permissions for the user (owner) of the file.
   * <p>These permissions are relevant only if the decoded data is to be
   * stored in a file, the value returned was provided inside the stream
   * by the encoder. They are specified by or-ing together the desired
   * bits patterns {@link UUEncoder#READ}, {@link UUEncoder#WRITE} and
   * {@link UUEncoder#EXECUTE}.</p>
   * @return access permissions for the user
   * @see #getGroupPerms
   * @see #getOthersPerms
  */
  public int getUserPerms() {
    return userPerms;
  }

  /** Get the access permissions for the group of the file.
   * <p>These permissions are relevant only if the decoded data is to be
   * stored in a file, the value returned was provided inside the stream
   * by the encoder. They are specified by or-ing together the desired
   * bits patterns {@link UUEncoder#READ}, {@link UUEncoder#WRITE} and
   * {@link UUEncoder#EXECUTE}.</p>
   * @return access permissions for the group
   * @see #getUserPerms
   * @see #getOthersPerms
   */
  public int getGroupPerms() {
    return groupPerms;
  }

  /** Get the access permissions for others.
   * <p>These permissions are relevant only if the decoded data is to be
   * stored in a file, the value returned was provided inside the stream
   * by the encoder. They are specified by or-ing together the desired
   * bits patterns {@link UUEncoder#READ}, {@link UUEncoder#WRITE} and
   * {@link UUEncoder#EXECUTE}.</p>
   * @return access permissions others
   * @see #getUserPerms
   * @see #getGroupPerms
   */
  public int getOthersPerms() {
    return othersPerms;
  }

  /** Filter some bytes from the underlying stream.
   * @return number of bytes inserted in the filtered bytes buffer or -1 if
   * the underlying stream has no bytes left
   * @exception IOException if the underlying stream throws one
   */
  protected int filterBytes()
    throws IOException {

    if (headerPending) {
      parseHeader();
    }

    if (nEncoded < 1) {
      if (readEncodedBytes() < 0) {
        return -1;
      }
    }

    // bytes decoding loop
    int inserted = 0;
    while (current < nEncoded) {

      if (current < firstQuantum) {
        if (decode(current) == 0) {
          // this is the end of the encoded data
          parseTrailer();
          return inserted;
        }

        // store the expected number of raw bytes encoded in the line
        remaining = decode(current);
        current   = firstQuantum;

      }

      if (encoded[current] == '\n') {

        if (remaining != 0) {
          throw new IOException("encoded length inconsistent with encoded data");
        }

        // update state for next line
        firstQuantum = current + 2;

      } else {
        // this is an encoded data byte
        int fByte;

        // combine the various 6-bits bytes to produce 8-bits bytes
        switch ((current - firstQuantum) % 4) {
        case 0:
          // nothing to do
          break;
        case 1:
          fByte = ((decode(current - 1) & 0x3F) << 2)
                | ((decode(current) & 0x30) >> 4);
          if (remaining > 0) {
            putFilteredByte(fByte);
            ++inserted;
            --remaining;
          } else if (fByte != 0) {
            throw new IOException("unexpected non null bytes after encoded line");
          }
          break;
        case 2:
          fByte = ((decode(current - 1) & 0x0F) << 4)
                | ((decode(current) & 0x3C) >> 2);
          if (remaining > 0) {
            putFilteredByte(fByte);
            ++inserted;
            --remaining;
          } else if (fByte != 0) {
            throw new IOException("unexpected non null bytes after encoded line");
          }
          break;
        default:
          fByte = ((decode(current - 1) & 0x03) << 6)
                 | (decode(current) & 0x3F);
          if (remaining > 0) {
            putFilteredByte(fByte);
            ++inserted;
            --remaining;
          } else if (fByte != 0) {
            throw new IOException("unexpected non null bytes after encoded line");
          }
          
        }
      }

      ++current;

    }

    // preserve current quantum for next round
    int start = current - ((current - firstQuantum) % 4);
    System.arraycopy(encoded, start, encoded, 0, nEncoded - start);
    nEncoded     -= start;
    current      -= start;
    firstQuantum -= start;
    return inserted;
      
  }

  /** Parse the file header.
   * @exception IOException if the underlying stream throws one
   */
  private void parseHeader()
  throws IOException {

    while (headerPending) {

      // check if we have a complete line
      int eol = -1;
      while (eol < 0) {
        for (int i = 0; (eol < 0) && (i < nEncoded); ++i) {
          if (encoded[i] == '\n') {
            eol = i;
          }
        }   
        if (eol < 0) {
          // we don't have enough characters
          if (readEncodedBytes() < 0) {
            throw new IOException("missing uuencode header");
          }
        }
      }
    
      if ((eol < 4) || (encoded[0] != 'b') || (encoded[1] != 'e')
          || (encoded[2] != 'g') || (encoded[3] != 'i') || (encoded[4] != 'n')) {
        // this is not the header line, skip it
        System.arraycopy(encoded, eol + 1, encoded, 0,
                         nEncoded - eol);
        nEncoded -= eol;
      } else {

        // skip the whitespace characters
        int i = 5;
        while ((i < eol) && Character.isWhitespace((char) encoded[i])) {
          ++i;
        }

        if (((i + 2) < eol)
            && (encoded[i]   >= '0') && (encoded[i]   <= '7')
            && (encoded[i+1] >= '0') && (encoded[i+1] <= '7')
            && (encoded[i+2] >= '0') && (encoded[i+2] <= '7')) {
        
          // store the permissions
          userPerms   = encoded[i]   - '0';
          groupPerms  = encoded[i+1] - '0';
          othersPerms = encoded[i+2] - '0';

          // in order to allow space in file names, uudecode as provided in
          // version 4.3.x of the GNU sharutils package uses a single space
          // between permissions and file name
          if (encoded[i+3] == ' ') {
            i += 4;

            // store the file name (which may contain space characters)
            StringBuffer buffer = new StringBuffer();
            while (i < eol) {
              buffer.append((char) encoded[i++]);
            }
            name = buffer.toString();

            // set up state for data decoding
            headerPending = false;
            System.arraycopy(encoded, eol + 1, encoded, 0, nEncoded - eol);
            nEncoded    -= eol;
            firstQuantum = 1;
            current      = 0;
            return;

          }
        }

        throw new IOException("malformed uuencode header");

      }

    }

  }

  /** Parse the file trailer.
   * @exception IOException if the underlying stream throws one
   */
  private void parseTrailer()
  throws IOException {

    // make sure we have enough room to read the two trailer lines
    System.arraycopy(encoded, current, encoded, 0, nEncoded - current);
    nEncoded -= current;

    // read the two trailer lines
    int eol1Index = -1, eol2Index = -1;
    while (eol2Index < 0) {
      for (int i = eol1Index + 1; (eol2Index < 0) && (i < nEncoded); ++i) {
        if (encoded[i] == '\n') {
          if (eol1Index < 0) {
            eol1Index = i;
          } else {
            eol2Index = i;
          }
        }
      }

      if (eol2Index < 0) {
        // we need more characters
        if (readEncodedBytes() < 0) {
          throw new IOException("missing uuencode trailer");
        }
      }

    }

    // check the trailer
    current = 1;
    while ((current < eol1Index)
        && Character.isWhitespace((char) encoded[current])) {
      ++current;
    }
    if (current++ == eol1Index) {
      while ((current < eol2Index)
          && Character.isWhitespace((char) encoded[current])) {
        ++current;
      }
      if (((current + 2) < eol2Index)  && (encoded[current] == 'e')
          && (encoded[current + 1] == 'n') && (encoded[current + 2] == 'd')) {
        current += 3;        
        while ((current < eol2Index)
            && Character.isWhitespace((char) encoded[current])) {
          ++current;
        }
        if (current == eol2Index) {
          // the trailer is correct
          endReached = true;
          return;
        }
      }
    }

    throw new IOException("malformed uuencode trailer");

  }

  /** Decode a 6-bits byte from the encoded array.
   * @param index index of the raw byte in the encoded array
   * @return the corresponding 6-bit byte
   */
  private int decode(int index) {
    return (encoded[index] - 32) & 0x3F;
  }

  /** Name of the data file. */
  private String name;
  
  /** Access permission for the user (owner). */
  private int userPerms;
  
  /** Access permission for the group. */
  private int groupPerms;
  
  /** Access permission for others. */
  private int othersPerms;
  
  /** Indicator of header read status. */
  private boolean headerPending;
  
  /** Index of the start of the current encoded line. */
  private int firstQuantum;

  /** Number of remaining filtered bytes in the current line. */
  private int remaining;

  /** Current index in the encoded array. */
  private int current;

}
