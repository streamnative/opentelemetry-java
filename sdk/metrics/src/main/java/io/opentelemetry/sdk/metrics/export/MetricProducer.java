/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.export;

import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.internal.export.MetricFilter;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;
import javax.annotation.concurrent.ThreadSafe;

/**
 * {@link MetricProducer} is the interface that is used to make metric data available to the {@link
 * MetricReader}s. The primary implementation is provided by {@link
 * io.opentelemetry.sdk.metrics.SdkMeterProvider}.
 *
 * <p>Alternative {@link MetricProducer} implementations can be used to bridge aggregated metrics
 * from other frameworks, and are registered with {@link
 * SdkMeterProviderBuilder#registerMetricProducer(MetricProducer)}. NOTE: When possible, metrics
 * from other frameworks SHOULD be bridged using the metric API, normally with asynchronous
 * instruments which observe the aggregated state of the other framework. However, {@link
 * MetricProducer} exists to accommodate scenarios where the metric API is insufficient. It should
 * be used with caution as it requires the bridge to take a dependency on {@code
 * opentelemetry-sdk-metrics}, which is generally not advised.
 *
 * <p>Implementations must be thread-safe.
 *
 * @since 1.31.0
 */
@ThreadSafe
public interface MetricProducer {

  /**
   * Returns a collection of produced {@link MetricData}s to be exported. This will only be those
   * metrics that have been produced since the last time this method was called.
   *
   * @return a collection of produced {@link MetricData}s to be exported.
   * @deprecated Implement {@link #produce(Resource, MetricFilter)} instead.
   */
  @Deprecated
  default Collection<MetricData> produce(Resource resource) {
    throw new UnsupportedOperationException("This method is deprecated.");
  }

  /**
   * Returns a collection of produced {@link MetricData}s to be exported, filtered by the provided
   * {@link MetricFilter}. This will only be those metrics that have been produced since the last
   * time this method was called.
   *
   * <p>Implementation SHOULD use the {@code metricFilter} as early as possible to gain as much
   * performance gain possible (memory allocation, internal metric fetching, etc.).
   *
   * <p>Default implementation is provided for backward compatability with existing
   * metric producers which implement the produce method without the filter, as the
   * SDK will use the method with the metric filter.
   *
   * @return a collection of produced {@link MetricData}s to be exported, filtered by {@code
   *     metricFilter}
   */
  default Collection<MetricData> produce(Resource resource, MetricFilter metricFilter) {
    return DefaultMetricFilterExecutor.filter(produce(resource), metricFilter);
  }
}
