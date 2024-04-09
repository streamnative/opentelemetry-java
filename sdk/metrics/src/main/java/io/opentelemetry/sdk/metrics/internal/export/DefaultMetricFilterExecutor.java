/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.export;

import io.opentelemetry.sdk.metrics.data.Data;
import io.opentelemetry.sdk.metrics.data.DoublePointData;
import io.opentelemetry.sdk.metrics.data.ExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.data.HistogramPointData;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.metrics.data.SummaryPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryData;
import io.opentelemetry.sdk.metrics.internal.export.MetricFilter.AttributesFilterResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/***
 * 
 *  <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 *  at any time.
 */
public final class DefaultMetricFilterExecutor {

  public static Collection<MetricData> filter(
      Collection<MetricData> metricDataCollection, MetricFilter metricFilter) {

    return metricDataCollection.stream()
        .map(
            metricData -> {
              MetricFilter.MetricFilterResult metricFilterResult =
                  metricFilter.testMetric(
                      metricData.getInstrumentationScopeInfo(),
                      metricData.getName(),
                      metricData.getType(),
                      metricData.getUnit());
              switch (metricFilterResult) {
                case DROP:
                  return null;
                case ACCEPT:
                  return metricData;
                case ACCEPT_PARTIAL:
                  Data<?> data = metricData.getData();
                  List<? extends PointData> acceptedPoints =
                      data.getPoints().stream()
                          .filter(
                              point -> {
                                AttributesFilterResult attributesFilterResult =
                                    metricFilter.testAttributes(
                                        metricData.getInstrumentationScopeInfo(),
                                        metricData.getName(),
                                        metricData.getType(),
                                        metricData.getUnit(),
                                        point.getAttributes());
                                return attributesFilterResult == AttributesFilterResult.ACCEPT;
                              })
                          .collect(Collectors.toList());
                  return createImmtuableMetricData(metricData, acceptedPoints);
              }
              return null;
            })
        .filter(metricData -> metricData != null)
        .collect(Collectors.toCollection(() -> new ArrayList<>()));
  }

  @SuppressWarnings("unchecked")
  private static MetricData createImmtuableMetricData(
      MetricData metricData, Collection<? extends PointData> acceptedPoints) {
    switch (metricData.getType()) {
      case EXPONENTIAL_HISTOGRAM:
        return ImmutableMetricData.createExponentialHistogram(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableExponentialHistogramData.create(
                metricData.getExponentialHistogramData().getAggregationTemporality(),
                (Collection<ExponentialHistogramPointData>) acceptedPoints));
      case DOUBLE_GAUGE:
        return ImmutableMetricData.createDoubleGauge(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableGaugeData.create((Collection<DoublePointData>) acceptedPoints));
      case LONG_GAUGE:
        return ImmutableMetricData.createLongGauge(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableGaugeData.create((Collection<LongPointData>) acceptedPoints));
      case DOUBLE_SUM:
        SumData<DoublePointData> existingDoubleSumData = metricData.getDoubleSumData();
        return ImmutableMetricData.createDoubleSum(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableSumData.create(
                existingDoubleSumData.isMonotonic(),
                existingDoubleSumData.getAggregationTemporality(),
                (Collection<DoublePointData>) acceptedPoints));
      case LONG_SUM:
        SumData<LongPointData> existingLongSumData = metricData.getLongSumData();
        return ImmutableMetricData.createLongSum(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableSumData.create(
                existingLongSumData.isMonotonic(),
                existingLongSumData.getAggregationTemporality(),
                (Collection<LongPointData>) acceptedPoints));
      case SUMMARY:
        return ImmutableMetricData.createDoubleSummary(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableSummaryData.create((Collection<SummaryPointData>) acceptedPoints));
      case HISTOGRAM:
        return ImmutableMetricData.createDoubleHistogram(
            metricData.getResource(),
            metricData.getInstrumentationScopeInfo(),
            metricData.getName(),
            metricData.getDescription(),
            metricData.getUnit(),
            ImmutableHistogramData.create(
                metricData.getHistogramData().getAggregationTemporality(),
                (Collection<HistogramPointData>) acceptedPoints));
    }
    throw new IllegalStateException("Unexpected MetricDataType: " + metricData.getType());
  }

  private DefaultMetricFilterExecutor() {}
}
