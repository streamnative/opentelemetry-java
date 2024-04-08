/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import static io.opentelemetry.sdk.internal.ThrowableUtil.propagateIfFatal;
import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.DELTA;
import static io.opentelemetry.sdk.metrics.internal.export.MetricFilter.MetricFilterResult.DROP;
import static java.util.stream.Collectors.toList;

import io.opentelemetry.sdk.internal.ThrottlingLogger;
import io.opentelemetry.sdk.metrics.internal.descriptor.InstrumentDescriptor;
import io.opentelemetry.sdk.metrics.internal.descriptor.MetricDescriptor;
import io.opentelemetry.sdk.metrics.internal.export.MetricFilter;
import io.opentelemetry.sdk.metrics.internal.export.MetricFilter.MetricFilterResult;
import io.opentelemetry.sdk.metrics.internal.export.RegisteredReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A registered callback.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class CallbackRegistration {
  private static final Logger logger = Logger.getLogger(CallbackRegistration.class.getName());

  private final ThrottlingLogger throttlingLogger = new ThrottlingLogger(logger);
  private final List<SdkObservableMeasurement> observableMeasurements;
  private final Runnable callback;
  private final List<InstrumentDescriptor> instrumentDescriptors;
  private final boolean hasStorages;

  private CallbackRegistration(
      List<SdkObservableMeasurement> observableMeasurements, Runnable callback) {
    this.observableMeasurements = observableMeasurements;
    this.callback = callback;
    this.instrumentDescriptors =
        observableMeasurements.stream()
            .map(SdkObservableMeasurement::getInstrumentDescriptor)
            .collect(toList());
    if (instrumentDescriptors.isEmpty()) {
      throw new IllegalStateException("Callback with no instruments is not allowed");
    }
    this.hasStorages =
        observableMeasurements.stream()
            .flatMap(measurement -> measurement.getStorages().stream())
            .findAny()
            .isPresent();
  }

  /**
   * Create a callback registration.
   *
   * <p>The {@code observableMeasurements} define the set of measurements the {@code runnable} may
   * record to. The active reader of each {@code observableMeasurements} is set via {@link
   * SdkObservableMeasurement#setActiveReader(RegisteredReader, long, long)} before {@code runnable}
   * is called, and set to {@code null} afterwards.
   *
   * @param observableMeasurements the measurements that the runnable may record to
   * @param runnable the callback
   * @return the callback registration
   */
  public static CallbackRegistration create(
      List<SdkObservableMeasurement> observableMeasurements, Runnable runnable) {
    return new CallbackRegistration(observableMeasurements, runnable);
  }

  @Override
  public String toString() {
    return "CallbackRegistration{instrumentDescriptors=" + instrumentDescriptors + "}";
  }

  void invokeCallback(
      RegisteredReader reader, long startEpochNanos, long epochNanos, MetricFilter metricFilter) {
    // Return early if no storages are registered
    if (!hasStorages) {
      return;
    }

    int expectedMetricCount = 0;
    boolean onlyCumulativeTemporalityStorages = true;
    for (SdkObservableMeasurement measurement : observableMeasurements) {
      expectedMetricCount += measurement.getStorages().size();

      for (AsynchronousMetricStorage<?, ?> asynchronousMetricStorage : measurement.getStorages()) {
        if (asynchronousMetricStorage.getAggregationTemporality() == DELTA) {
          onlyCumulativeTemporalityStorages = false;
          break;
        }
      }
    }

    // FIXME: What happens when the aggregation is drop ==> empty aggregation?
    // Delta temporality requires all points to be recorded because the filter can change
    // to suddenly return a value for a metric, and it's delta requires knowing the last point,
    // even if it was filtered out
    if (onlyCumulativeTemporalityStorages) {
      MetricFilterResult[] metricFilterResults = new MetricFilterResult[expectedMetricCount];
      int i = 0;
      for (SdkObservableMeasurement sdkObservableMeasurement : observableMeasurements) {
        for (AsynchronousMetricStorage<?, ?> asynchronousMetricStorage :
            sdkObservableMeasurement.getStorages()) {
          MetricDescriptor metricDescriptor = asynchronousMetricStorage.getMetricDescriptor();

          metricFilterResults[i++] =
              metricFilter.testMetric(
                  sdkObservableMeasurement.getInstrumentationScopeInfo(),
                  metricDescriptor.getName(),
                  metricDescriptor.getMetricDataType(),
                  metricDescriptor.getSourceInstrument().getUnit());
        }

        if (allIs(metricFilterResults, DROP)) {
          // If all the metrics are to be dropped, we don't need to invoke the callback
          return;
        }
      }
    }

    // Set the active reader on each observable measurement so that measurements are only recorded
    // to relevant storages
    observableMeasurements.forEach(
        observableMeasurement -> {
          observableMeasurement.setActiveReader(reader, startEpochNanos, epochNanos);
          observableMeasurement.setActiveFilter(metricFilter);
        });
    try {
      callback.run();
    } catch (Throwable e) {
      propagateIfFatal(e);
      throttlingLogger.log(
          Level.WARNING, "An exception occurred invoking callback for " + this + ".", e);
    } finally {
      observableMeasurements.forEach(
          observableMeasurement -> {
            observableMeasurement.unsetActiveReader();
            observableMeasurement.unsetActiveFilter();
          });
    }
  }

  private static boolean allIs(
      MetricFilterResult[] metricFilterResults,
      MetricFilterResult sameFilterResult) {
    for (MetricFilterResult r : metricFilterResults) {
      if (r != sameFilterResult) {
        return false;
      }
    }
    return true;
  }
}
