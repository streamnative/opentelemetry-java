/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.exporter.internal.otlp;

import io.opentelemetry.exporter.internal.marshal.CodedOutputStream;
import io.opentelemetry.exporter.internal.marshal.MarshalerUtil;
import io.opentelemetry.exporter.internal.marshal.MarshalerWithSize;
import io.opentelemetry.exporter.internal.marshal.Serializer;
import io.opentelemetry.exporter.internal.marshal.DefaultMessageSize;
import io.opentelemetry.exporter.internal.otlp.metrics.MarshallingObjectsPool;
import io.opentelemetry.exporter.internal.marshal.MessageSize;
import io.opentelemetry.proto.common.v1.internal.AnyValue;
import java.io.IOException;

/**
 * A Marshaler of string-valued {@link AnyValue}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
final class StringAnyValueMarshaler extends MarshalerWithSize {

  private final byte[] valueUtf8;

  private StringAnyValueMarshaler(byte[] valueUtf8) {
    super(calculateSize(valueUtf8));
    this.valueUtf8 = valueUtf8;
  }

  static MarshalerWithSize create(String value) {
    return new StringAnyValueMarshaler(MarshalerUtil.toBytes(value));
  }

  static MessageSize messageSize(String value, MarshallingObjectsPool pool) {
    int encodedSize = AnyValue.STRING_VALUE.getTagSize()
        + CodedOutputStream.computeStringSizeNoTag(value);

    DefaultMessageSize messageSize = pool.getDefaultMessageSizePool().borrowObject();
    messageSize.set(encodedSize);
    return messageSize;
  }

  static void encode(Serializer output, String value) throws IOException {
    output.writeString(AnyValue.STRING_VALUE, value);
  }

  @Override
  public void writeTo(Serializer output) throws IOException {
    // Do not call serialize* method because we always have to write the message tag even if the
    // value is empty since it's a oneof.
    output.writeString(AnyValue.STRING_VALUE, valueUtf8);
  }

  private static int calculateSize(byte[] valueUtf8) {
    return AnyValue.STRING_VALUE.getTagSize()
        + CodedOutputStream.computeByteArraySizeNoTag(valueUtf8);
  }
}
