// Copyright (c) 2005-2009, Luc Maisonobe
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

abstract class AbstractDecoder extends FilterInputStream {

  /** Create a filter for decoding a binary stream.
   * @param in source of encoded data to decode
   */
  protected AbstractDecoder(InputStream in) {
    super(in);
    filtered     = new byte[512];
    inIndex      =  0;
    currentIndex =  0;
    encoded      = new byte[512];
    nEncoded     = 0;
    endReached   = false;
  }

  /** Get the number of bytes that can be read from this input
   * stream without blocking.
   * @return number of bytes that can be read from this input stream
   * without blocking
   * @exception IOException if the underlying stream throws one
   */
  public int available() throws IOException {
  
    if (inIndex == currentIndex) {
      // the filtered buffer has no pending bytes,
      // make a conservative guess according to the underlying stream
      return (in.available() > 0) ? 1 : 0;
    }
  
    return pendingBytes();
  
  }

  /** Reads the next byte of data from this input stream.
   * @return the next byte of data, or -1 if the end of the stream is
   * reached.
   * @exception IOException if the underlying stream throws one
   */
  public int read() throws IOException {
  
    while (pendingBytes() == 0) {
      // nothing is available, try to read and decode new bytes
      if (endReached || (filterBytes() < 0)) {
        return -1;
      }
    }
  
    // provide one byte from the filtered buffer
    int b = filtered[currentIndex];
    if (++currentIndex == filtered.length) {
      currentIndex = 0;
    }
    return b & 0xff;
  
  }

  /** Reads up to len bytes of data from this input stream into an
   * array of bytes.
   * @param b   the buffer into which the data is read.
   * @param off the start offset of the data.
   * @param len the maximum number of bytes read.
   * @return the total number of bytes read into the buffer, or -1 if
   * there is no more data because the end of the stream has been
   * reached
   * @exception IOException if the underlying stream throws one
   */
  public int read(byte[] b, int off, int len) throws IOException {

    if (len <= 0) {
      return 0;
    }

    while (inIndex == currentIndex) {
      // nothing is available, try to read and filter new bytes
      if (endReached || (filterBytes() < 0)) {
        return -1;
      }
    }
  
    // copy the filtered bytes
    int n = Math.min(len, pendingBytes());
    int tailSize = filtered.length - currentIndex;
    if ((inIndex > currentIndex) || (n <= tailSize)) {
      System.arraycopy(filtered, currentIndex, b, off, n);
      currentIndex += n;
    } else {
      System.arraycopy(filtered, currentIndex, b, off, tailSize);
      System.arraycopy(filtered, 0, b, off + tailSize, n - tailSize);
      currentIndex = n - tailSize;
    }
  
    return n;
  
  }

  /** Skips over and discards n bytes of data from the input stream.
   * @param n the number of bytes to be skipped
   * @return the actual number of bytes skipped
   * @exception IOException if the underlying stream throws one
   */
  public long skip(long n) throws IOException {
  
    if (inIndex == currentIndex) {
      // nothing is available, try to read and filter new bytes
      if (endReached || (filterBytes() < 0)) {
        return 0;
      }
    }
  
    // skip the filtered bytes
    n = Math.min(n, pendingBytes());
    currentIndex += (int) n;
    if (currentIndex >= filtered.length) {
      currentIndex -= filtered.length;
    }
  
    return n;
  
  }

  /** Close the stream.
   * @exception IOException if the underlying stream throws one
   */
  public void close() throws IOException {
    in.close();
  }

  /** Check if the {@link FilterInputStream#mark} and
   * {@link FilterInputStream#reset} methods are supported.
   * @return always false
   */
  public boolean markSupported() {
    return false;
  }

  /** Return the number of pending bytes.
   * <p>Pending bytes are bytes already read and filtered, but not yet
   * provided to the outer classes</p>
   * @return number of pending bytes
   */
  private int pendingBytes() {
    int gap = inIndex - currentIndex;
    return (gap >= 0) ? gap : (filtered.length + gap);
  }

  /** Ensure the filtered bytes buffer capacity.
   * @param capacity number of filtered bytes the buffer must be able
   * to hold
   */
  private void ensureCapacity(int capacity) {
  
    if (capacity < filtered.length) {
      return;
    }

    byte[] newBuffer = new byte[Math.max(capacity + 1,
                                         filtered.length * 2)];
    int start = currentIndex;
  
    // copy the filtered bytes in the new buffer
    if (start < inIndex) {
      // the filtered bytes are gathered in the same slice
      System.arraycopy(filtered, start, newBuffer, 0, inIndex - start);
      currentIndex -= start;
      inIndex      -= start;
    } else if (start > inIndex) {
      // the filtered bytes are split in two slices of the buffer
      int tailSize = filtered.length - start;
      System.arraycopy(filtered, start, newBuffer, 0, tailSize);
      System.arraycopy(filtered, 0, newBuffer, tailSize, inIndex);
      currentIndex += (currentIndex < start) ? tailSize : -start;
      inIndex      += tailSize;
    } else {
      // the buffer is empty
      currentIndex = 0;
      inIndex      = 0;
    }
  
    filtered = newBuffer;
  
  }

  /** Read new encoded bytes.
   * @return number of bytes read or -1 if the underlying stream has
   * no bytes left
   * @exception IOException if the underlying stream throws one
   */
  protected int readEncodedBytes() throws IOException {

    if (endReached) {
      return -1;
    }

    if (nEncoded >= encoded.length) {
      // expand the encoded buffer
      byte[] newEncoded = new byte[2 * encoded.length];
      System.arraycopy(encoded, 0, newEncoded, 0, nEncoded);
      encoded = newEncoded;
    }

    // read some bytes
    int n = in.read(encoded, nEncoded, encoded.length - nEncoded);
    if (n >= 0) {
      nEncoded += n;
    }
 
    return n;

  }

  /** Read and filter bytes from the underlying stream and put them
   * in the filtered bytes buffer.
   * <p>Subclasses must provide a specialized version of this method which must
   * call the {@link #readEncodedBytes} to get new bytes from the underlying
   * stream and {@link #putFilteredByte} method to store the filtered bytes.</p>
   * @return number of bytes inserted in the filtered bytes buffer or -1 if
   * the underlying stream has no bytes left
   * @exception IOException if the underlying stream throws one
   * @see #putFilteredByte
   */
  protected abstract int filterBytes() throws IOException;

  /** Put a filtered byte in the buffer.
   * <p>The filtered bytes buffer will be expanded if it already contains
   * too many pending bytes.</p>
   * @param b byte to put in the filtered buffer
   */
  protected void putFilteredByte(int b) {

    // expand the buffer if necessary
    ensureCapacity(pendingBytes() + 1);

    // store the byte
    filtered[inIndex] = (byte) b;

    if (++inIndex == filtered.length) {
      // wrap index at circular buffer end
      inIndex = 0;
    }

  }

 /** Filtered bytes buffer. */
  private byte[] filtered;

  /** Index where to put new filtered bytes in the buffer. */
  private int inIndex;

  /** Index of the current position in the filtered buffer. */
  private int currentIndex;

  /** Encoded bytes buffer. */
  protected byte[] encoded;

  /** Number of bytes already in the encoded array. */
  protected int nEncoded;

  /** End indicator. */
  protected boolean endReached;

}
