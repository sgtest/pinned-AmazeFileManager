

package com.amaze.filemanager.filesystem.compressed.sevenz;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.compress.utils.ByteUtils;

/** Base Codec class. */
abstract class CoderBase {
  private final Class<?>[] acceptableOptions;

  /**
   * @param acceptableOptions types that can be used as options for this codec.
   */
  protected CoderBase(final Class<?>... acceptableOptions) {
    this.acceptableOptions = acceptableOptions;
  }

  /**
   * @return whether this method can extract options from the given object.
   */
  boolean canAcceptOptions(final Object opts) {
    for (final Class<?> c : acceptableOptions) {
      if (c.isInstance(opts)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return property-bytes to write in a Folder block
   */
  byte[] getOptionsAsProperties(final Object options) throws IOException {
    return ByteUtils.EMPTY_BYTE_ARRAY;
  }

  /**
   * @return configuration options that have been used to create the given InputStream from the
   *     given Coder
   */
  Object getOptionsFromCoder(final Coder coder, final InputStream in) throws IOException {
    return null;
  }

  /**
   * @return a stream that reads from in using the configured coder and password.
   */
  abstract InputStream decode(
      final String archiveName,
      final InputStream in,
      long uncompressedLength,
      final Coder coder,
      byte[] password,
      int maxMemoryLimitInKb)
      throws IOException;

  /**
   * @return a stream that writes to out using the given configuration.
   */
  OutputStream encode(final OutputStream out, final Object options) throws IOException {
    throw new UnsupportedOperationException("Method doesn't support writing");
  }

  /**
   * If the option represents a number, return its integer value, otherwise return the given default
   * value.
   */
  protected static int numberOptionOrDefault(final Object options, final int defaultValue) {
    return options instanceof Number ? ((Number) options).intValue() : defaultValue;
  }
}
