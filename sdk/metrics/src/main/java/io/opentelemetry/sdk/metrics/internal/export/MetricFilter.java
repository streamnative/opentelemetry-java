/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.export;

import io.opentelemetry.api.common.Attributes;
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
 * <p>The metric filter allows filtering an entire metric stream - dropping or allowing all its
 * {@link Attributes} - by its {@link #testMetric(InstrumentationScopeInfo, String, MetricDataType,
 * String)} method, which accepts the metric stream information (scope, name, kind and unit) and
 * returns an enumeration: {@link MetricFilterResult#ACCEPT}, {@link MetricFilterResult#DROP} or
 * {@link MetricFilterResult#ACCEPT_PARTIAL}. If the latter is returned, the {@link
 * #testAttributes(InstrumentationScopeInfo, String, MetricDataType, String, Attributes)} method is
 * to be called per {@link Attributes} of that metric stream, returning an enumeration ({@link
 * AttributesFilterResult}) determining if the data point for that (metric stream, {@link
 * Attributes}) pair is to be allowed in the result of the {@link MetricProducer#produce(Resource)}.
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

  /**
   * Returns a metric filter that accepts all metrics and attributes.
   *
   * @return a {@link MetricFilter} that accepts all metrics and attributes.
   */
  static MetricFilter acceptAll() {
    return new MetricFilter() {
      @Override
      public MetricFilterResult testMetric(
          InstrumentationScopeInfo instrumentationScopeInfo,
          String name,
          MetricDataType type,
          String unit) {
        return MetricFilterResult.ACCEPT;
      }

      @Override
      public AttributesFilterResult testAttributes(
          InstrumentationScopeInfo instrumentationScopeInfo,
          String name,
          MetricDataType type,
          String unit,
          Attributes attributes) {
        return AttributesFilterResult.ACCEPT;
      }
    };
  }

  /**
   * Test if the given metric stream is allowed to be collected.
   *
   * <p>This method is called once for every metric stream, in each execution of {@link
   * MetricProducer#produce(Resource)}.
   *
   * @param instrumentationScopeInfo The metric stream instrumentation scope
   * @param name The name of the metric stream
   * @param type The metric stream type
   * @param unit The metric stream unit
   * @return The result of the test
   */
  MetricFilterResult testMetric(
      InstrumentationScopeInfo instrumentationScopeInfo,
      String name,
      MetricDataType type,
      String unit);

  /**
   * Test if the given {@link Attributes} are allowed to be collected.
   *
   * <p>A method which determines for a given metric stream and {@link Attributes} if it should be
   * allowed or filtered out.
   *
   * <p>This operation should only be called if {@link #testMetric(InstrumentationScopeInfo, String,
   * MetricDataType, String)} returned {@link MetricFilterResult#ACCEPT_PARTIAL} for the given
   * metric stream arguments (instrumentationScope, name, kind, unit).
   *
   * @param instrumentationScopeInfo The metric stream instrumentation scope
   * @param name The name of the metric stream
   * @param type The metric stream type
   * @param unit The metric stream unit
   * @param attributes The attributes
   * @return The result of the test
   */
  AttributesFilterResult testAttributes(
      InstrumentationScopeInfo instrumentationScopeInfo,
      String name,
      MetricDataType type,
      String unit,
      Attributes attributes);

  enum MetricFilterResult {
    /**
     * All {@link Attributes} of the given metric stream are allowed (not to be filtered). This
     * provides a “short-circuit” as there is no need to call {@link
     * #testAttributes(InstrumentationScopeInfo, String, MetricDataType, String, Attributes)} for
     * each attribute set.
     */
    ACCEPT,

    /**
     * All {@link Attributes} of the given metric stream are NOT allowed (filtered out - dropped).
     * This provides a “short-circuit” as there is no need to call {@link
     * #testAttributes(InstrumentationScopeInfo, String, MetricDataType, String, Attributes)} for
     * each {@link Attributes}, and no need to collect those data points be it synchronous or
     * asynchronous: e.g. the callback for this given instrument does not need to be invoked.
     */
    DROP,

    /**
     * Some attributes are allowed and some aren’t. Hence, TestAttributes operation must be called
     * for each {@link Attributes} of that instrument.
     */
    ACCEPT_PARTIAL
  }

  enum AttributesFilterResult {
    /** The given {@link Attributes} are allowed (not to be filtered). */
    ACCEPT,

    /** The given {@link Attributes} are NOT allowed (filtered out - dropped). */
    DROP
  }
}
