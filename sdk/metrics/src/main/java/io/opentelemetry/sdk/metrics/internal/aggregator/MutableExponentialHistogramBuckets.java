/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.aggregator;

import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

public class MutableExponentialHistogramBuckets implements ExponentialHistogramBuckets {

  private int scale;
  private int offset;
  private long totalCount;

  @Nullable private List<Long> bucketCounts;

  @Override
  public int getScale() {
    return scale;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public List<Long> getBucketCounts() {
    return bucketCounts == null ? Collections.emptyList() : bucketCounts;
  }

  @Override
  public long getTotalCount() {
    return totalCount;
  }

  public MutableExponentialHistogramBuckets set(
      DoubleBase2ExponentialHistogramBuckets doubleBase2ExponentialHistogramBuckets) {

    this.scale = doubleBase2ExponentialHistogramBuckets.getScale();
    this.offset = doubleBase2ExponentialHistogramBuckets.getOffset();
    this.totalCount = doubleBase2ExponentialHistogramBuckets.getTotalCount();
    this.bucketCounts =
        doubleBase2ExponentialHistogramBuckets.getBucketCountsWithReusableList(bucketCounts);

    return this;
  }
}
