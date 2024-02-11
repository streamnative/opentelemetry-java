/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.export;

import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricProducer;
import io.opentelemetry.sdk.resources.Resource;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Metric filters are used to filter {@link PointData} by a {@link MetricProducer}.
 *
 * <p>The filtering is done at the {@link MetricProducer} for performance reasons.
 *
 * <p>The metric filter allows filtering an entire metric stream - dropping or allowing
 * all its attribute sets - by its `TestMetric` operation, which accepts the metric stream
 * information (scope, name, kind and unit)  and returns an enumeration: `Accept`, `Drop`
 * or `Allow_Partial`. If the latter returned, the `TestAttributes` operation
 * is to be called per attribute set of that metric stream, returning an enumeration
 * determining if the data point for that (metric stream, attributes) pair is to be
 * allowed in the result of the {@link MetricProducer#produce(Resource)} operation.
 *
 * <p>Implementations must be thread-safe.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 *
 * @since 1.35.0
 */
@ThreadSafe
public interface MetricFilter {

  MetricFilterResult testMetric(
      InstrumentationScopeInfo instrumentationScopeInfo,
      String name,
      MetricDataType type,
      String unit);

  enum MetricFilterResult {
    /**
     * All attributes of the given metric stream are allowed (not to be filtered).
     * This provides a “short-circuit” as there is no need to call {@link #testAttributes}
     * operation for each attribute set.
     */
    ACCEPT,

    /**
     * All attributes of the given metric stream are NOT allowed (filtered out - dropped).
     * This provides a “short-circuit” as there is no need to call TestAttributes operation
     * for each attribute set, and no need to collect those data points be it synchronous
     * or asynchronous: e.g. the callback for this given instrument does not need to be invoked.
     */
    DROP,
    ALLOW_PARTIAL
  }
}
