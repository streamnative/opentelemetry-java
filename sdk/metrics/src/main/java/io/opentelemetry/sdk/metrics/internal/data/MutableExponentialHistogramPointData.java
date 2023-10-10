package io.opentelemetry.sdk.metrics.internal.data;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.DoubleExemplarData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.aggregator.MutableExponentialHistogramBuckets;
import java.util.Collections;
import java.util.List;

public class MutableExponentialHistogramPointData implements ExponentialHistogramPointData {

  private long startEpochNanos;

  private long epochNanos;

  private Attributes attributes = Attributes.empty();

  private int scale;

  private double sum;

  private long count;

  private long zeroCount;

  private boolean hasMin;

  private double min;

  private boolean hasMax;

  private double max;

  private ExponentialHistogramBuckets positiveBuckets = new MutableExponentialHistogramBuckets();

  private ExponentialHistogramBuckets negativeBuckets = new MutableExponentialHistogramBuckets();

  private List<DoubleExemplarData> exemplars = Collections.emptyList();

  @Override
  public int getScale() {
    return scale;
  }

  @Override
  public double getSum() {
    return sum;
  }

  @Override
  public long getCount() {
    return count;
  }

  @Override
  public long getZeroCount() {
    return zeroCount;
  }

  @Override
  public boolean hasMin() {
    return hasMin;

  }

  @Override
  public double getMin() {
    return min;
  }

  @Override
  public boolean hasMax() {
    return hasMax;
  }

  @Override
  public double getMax() {
    return max;
  }

  @Override
  public ExponentialHistogramBuckets getPositiveBuckets() {
    return positiveBuckets;
  }

  @Override
  public ExponentialHistogramBuckets getNegativeBuckets() {
    return negativeBuckets;
  }

  @Override
  public long getStartEpochNanos() {
    return startEpochNanos;
  }

  @Override
  public long getEpochNanos() {
    return epochNanos;
  }

  @Override
  public Attributes getAttributes() {
    return attributes;
  }

  @Override
  public List<DoubleExemplarData> getExemplars() {
    return exemplars;
  }

  @SuppressWarnings("TooManyParameters")
  public ExponentialHistogramPointData set(
      int scale,
      double sum,
      long zeroCount,
      boolean hasMin,
      double min,
      boolean hasMax,
      double max,
      ExponentialHistogramBuckets positiveBuckets,
      ExponentialHistogramBuckets negativeBuckets,
      long startEpochNanos,
      long epochNanos,
      Attributes attributes,
      List<DoubleExemplarData> exemplars) {
    this.scale = scale;
    this.sum = sum;
    this.zeroCount = zeroCount;
    this.hasMin = hasMin;
    this.min = min;
    this.hasMax = hasMax;
    this.max = max;
    this.positiveBuckets = positiveBuckets;
    this.negativeBuckets = negativeBuckets;
    this.startEpochNanos = startEpochNanos;
    this.epochNanos = epochNanos;
    this.attributes = attributes;
    this.exemplars = exemplars;

    return this;
  }
}
