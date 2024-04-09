package io.opentelemetry.sdk.metrics.export;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.metrics.data.AggregationTemporality.CUMULATIVE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Lists;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.common.InstrumentationScopeInfo;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableDoublePointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramBuckets;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableExponentialHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableGaugeData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableHistogramPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableLongPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableMetricData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSumData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableSummaryPointData;
import io.opentelemetry.sdk.metrics.internal.data.ImmutableValueAtQuantile;
import io.opentelemetry.sdk.metrics.internal.export.MetricFilter;
import io.opentelemetry.sdk.resources.Resource;
import java.util.Collection;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the default implementation, provided for backward compatibility, of the {@link
 * MetricProducer#produce(Resource, MetricFilter)} method.
 */
public class MetricProducerTest {

  private static final Resource resource = Resource.create(
      Attributes.of(stringKey("my.resource"), "resource.value"));

  /**
   * In this test, we verify the default implementation of the {@link MetricProducer#produce(Resource,
   * MetricFilter)} method provides the correct data to the methods of the filter.
   */
  @Test
  void defaultImplementation_filterGetsCorrectData() {
    ControlledMetricProducer controlledMetricProducer = new ControlledMetricProducer();

    Collection<MetricData> producedMetricData = Lists.newArrayList(
        // Create metric data that won't get filtered
        ImmutableMetricData.createDoubleSum(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.1"),
            "double-sum-1",
            "double sum description",
            "1",
            ImmutableSumData.create(
                /* isMonotonic= */ true,
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableDoublePointData.create(
                        1000, 1000, Attributes.of(stringKey("key"), "value"), 10.1)
                )
            )
        )
    );

    controlledMetricProducer.setProduceResult(producedMetricData);

    controlledMetricProducer.produce(resource, new MetricFilter() {
      @Override
      public MetricFilterResult testMetric(
          InstrumentationScopeInfo instrumentationScopeInfo,
          String name,
          MetricDataType type,
          String unit) {

        assertThat(instrumentationScopeInfo.getName()).isEqualTo("instrumentation.scope.1");
        assertThat(name).isEqualTo("double-sum-1");
        assertThat(type).isEqualTo(MetricDataType.DOUBLE_SUM);
        assertThat(unit).isEqualTo("1");
        return MetricFilterResult.ACCEPT_PARTIAL;
      }

      @Override
      public AttributesFilterResult testAttributes(
          InstrumentationScopeInfo instrumentationScopeInfo,
          String name,
          MetricDataType type,
          String unit,
          Attributes attributes) {

        assertThat(instrumentationScopeInfo.getName()).isEqualTo("instrumentation.scope.1");
        assertThat(name).isEqualTo("double-sum-1");
        assertThat(type).isEqualTo(MetricDataType.DOUBLE_SUM);
        assertThat(unit).isEqualTo("1");
        assertThat(attributes.get(stringKey("key"))).isEqualTo("value");

        return AttributesFilterResult.ACCEPT;
      }
    });
  }

  /**
   * This test verifies the data that was dropped by testMetric is indeed dropped
   */
  @Test
  void defaultImplementation_filterMetricWorks() {
    ControlledMetricProducer controlledMetricProducer = new ControlledMetricProducer();

    // Create metric data that won't get filtered
    MetricData notFilteredMetricData = ImmutableMetricData.createDoubleSum(
        resource,
        InstrumentationScopeInfo.create("instrumentation.scope.1"),
        "double-sum-1",
        "double sum description",
        "1",
        ImmutableSumData.create(
            /* isMonotonic= */ true,
            /* temporality= */ CUMULATIVE,
            Lists.newArrayList(
                ImmutableDoublePointData.create(
                    1000, 1000, Attributes.of(stringKey("key"), "value"), 10.1)
            )
        )
    );
    Collection<MetricData> producedMetricData = Lists.newArrayList(
        notFilteredMetricData,

        // Create metric data that will get filtered
        ImmutableMetricData.createDoubleSum(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableSumData.create(
                /* isMonotonic= */ true,
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableDoublePointData.create(
                        1001, 1001, Attributes.of(stringKey("key2"), "value2"), 20.2)
                )
            )
        )
    );

    controlledMetricProducer.setProduceResult(producedMetricData);

    Collection<MetricData> filteredMetricData = controlledMetricProducer.produce(resource,
        new MetricFilter() {
          @Override
          public MetricFilterResult testMetric(
              InstrumentationScopeInfo instrumentationScopeInfo,
              String name,
              MetricDataType type,
              String unit) {

            if (instrumentationScopeInfo.getName().equals("instrumentation.scope.2")) {
              return MetricFilterResult.DROP;
            } else {
              return MetricFilterResult.ACCEPT;
            }
          }

          @Override
          public AttributesFilterResult testAttributes(
              InstrumentationScopeInfo instrumentationScopeInfo,
              String name,
              MetricDataType type,
              String unit,
              Attributes attributes) {

            throw new IllegalStateException("This method should not be called");
          }
        });

    assertThat(filteredMetricData).hasSize(1);
    assertThat(filteredMetricData).containsExactly(notFilteredMetricData);
  }

  /**
   * This test verifies the point data for the attrbiutes that were marked dropped by
   * the filter are indeed dropped and only the other ones remains on the metric data.
   * We test this for each metric data type.
   */
  @ParameterizedTest
  @MethodSource("defaultImplementation_filterAttributesWorks_arguments")
  void defaultImplementation_filterAttributesWorks(
      MetricData inputMetricData,
      MetricData expectedMetricData) {
    ControlledMetricProducer controlledMetricProducer = new ControlledMetricProducer();

    Collection<MetricData> producedMetricData = Lists.newArrayList(
        inputMetricData
    );

    controlledMetricProducer.setProduceResult(producedMetricData);

    Collection<MetricData> filteredMetricData = controlledMetricProducer.produce(resource,
        new MetricFilter() {
          @Override
          public MetricFilterResult testMetric(
              InstrumentationScopeInfo instrumentationScopeInfo,
              String name,
              MetricDataType type,
              String unit) {

            return MetricFilterResult.ACCEPT_PARTIAL;
          }

          @Override
          public AttributesFilterResult testAttributes(
              InstrumentationScopeInfo instrumentationScopeInfo,
              String name,
              MetricDataType type,
              String unit,
              Attributes attributes) {

            if (attributes.get(stringKey("filter-me")) != null) {
              return AttributesFilterResult.DROP;
            } else {
              return AttributesFilterResult.ACCEPT;
            }
          }
        });

    assertThat(filteredMetricData).hasSize(1);
    assertThat(filteredMetricData).containsExactly(expectedMetricData);
  }

  private static Stream<Arguments> defaultImplementation_filterAttributesWorks_arguments() {
    // In each arguments set, we create two metric data objects. The first one is the input metric
    // data, and the second one is the expected metric data after filtering.
    // In the input metric data, we have three points, one of which has an attribute with key
    // "filter-me". We expect that point to be removed after filtering. The expected metric data
    // should have the same data as the input metric data, but without the point with the attribute
    // "filter-me".
    //
    // We test this for each metric data type.
    return Stream.of(
        longSumDataTestArguments(),
        getDoubleSumTestArguments(),
        doubleGaugeDataTestArguments(),
        longGaugeDataTestArguments(),
        doubleHistogramTestArguments(),
        exponentialHistogramTestArguments(),
        summaryTestArguments()
    );
  }

  private static Arguments summaryTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createDoubleSummary(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "summary-2",
            "summary description",
            "1",
            ImmutableSummaryData.create(
                Lists.newArrayList(
                    ImmutableSummaryPointData.create(
                        1001,
                        1001,
                        Attributes.of(stringKey("key-1"), "value-1"),
                        10,
                        20,
                        Lists.newArrayList(ImmutableValueAtQuantile.create(0.5, 15))),
                    ImmutableSummaryPointData.create(
                        1002,
                        1002,
                        Attributes.of(stringKey("filter-me"), "value-2"),
                        20,
                        30,
                        Lists.newArrayList(ImmutableValueAtQuantile.create(0.5, 25))),
                    ImmutableSummaryPointData.create(
                        1003,
                        1003,
                        Attributes.of(stringKey("key-3"), "value-3"),
                        30,
                        40,
                        Lists.newArrayList(ImmutableValueAtQuantile.create(0.5, 35)))
                )
            )
        ),
        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createDoubleSummary(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "summary-2",
            "summary description",
            "1",
            ImmutableSummaryData.create(
                Lists.newArrayList(
                    ImmutableSummaryPointData.create(
                        1001,
                        1001,
                        Attributes.of(stringKey("key-1"), "value-1"),
                        10,
                        20,
                        Lists.newArrayList(ImmutableValueAtQuantile.create(0.5, 15))),
                    ImmutableSummaryPointData.create(
                        1003,
                        1003,
                        Attributes.of(stringKey("key-3"), "value-3"),
                        30,
                        40,
                        Lists.newArrayList(ImmutableValueAtQuantile.create(0.5, 35)))
                )
            )
        )
    );
  }

  private static Arguments exponentialHistogramTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createExponentialHistogram(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableExponentialHistogramData.create(
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableExponentialHistogramPointData.create(
                        1001,
                        1001,
                        0,
                        /* hasMin= */ false,
                        1099L,
                        /* hasMax= */ false,
                        0L,
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(10L, 20L, 30L)
                        ),
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(-10L, -20L, -30L)
                        ),
                        1001,
                        1001,
                        Attributes.of(stringKey("key-1"), "value-1"),
                        Lists.newArrayList()),

                    ImmutableExponentialHistogramPointData.create(
                        1002,
                        1002,
                        0,
                        /* hasMin= */ false,
                        1099L,
                        /* hasMax= */ false,
                        0L,
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(10L, 20L, 30L)
                        ),
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(-10L, -20L, -30L)
                        ),
                        1002,
                        1002,
                        Attributes.of(stringKey("filter-me"), "value-2"),
                        Lists.newArrayList()),

                    ImmutableExponentialHistogramPointData.create(
                        1003,
                        1003,
                        0,
                        /* hasMin= */ false,
                        1099L,
                        /* hasMax= */ false,
                        0L,
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(10L, 20L, 30L)
                        ),
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(-10L, -20L, -30L)
                        ),
                        1003,
                        1003,
                        Attributes.of(stringKey("key-3"), "value-3"),
                        Lists.newArrayList())
                )
            )
        ),

        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createExponentialHistogram(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableExponentialHistogramData.create(
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableExponentialHistogramPointData.create(
                        1001,
                        1001,
                        0,
                        /* hasMin= */ false,
                        1099L,
                        /* hasMax= */ false,
                        0L,
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(10L, 20L, 30L)
                        ),
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(-10L, -20L, -30L)
                        ),
                        1001,
                        1001,
                        Attributes.of(stringKey("key-1"), "value-1"),
                        Lists.newArrayList()),

                    ImmutableExponentialHistogramPointData.create(
                        1003,
                        1003,
                        0,
                        /* hasMin= */ false,
                        1099L,
                        /* hasMax= */ false,
                        0L,
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(10L, 20L, 30L)
                        ),
                        ImmutableExponentialHistogramBuckets.create(
                            0,
                            2,
                            Lists.newArrayList(-10L, -20L, -30L)
                        ),
                        1003,
                        1003,
                        Attributes.of(stringKey("key-3"), "value-3"),
                        Lists.newArrayList())
                )
            )
        )
    );
  }

  private static Arguments doubleHistogramTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createDoubleHistogram(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableHistogramData.create(
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableHistogramPointData.create(
                        1001,
                        1001,
                        Attributes.of(stringKey("key-1"), "value-1"),
                        1098L,
                        /* hasMin= */ false,
                        0L,
                        /* hasMax= */ false,
                        0L,
                        Lists.newArrayList(1.0, 2.0, 3.0),
                        Lists.newArrayList(1L, 2L, 3L, 4L)),
                    ImmutableHistogramPointData.create(
                        1002,
                        1002,
                        Attributes.of(stringKey("filter-me"), "value-2"),
                        1099L,
                        /* hasMin= */ false,
                        0L,
                        /* hasMax= */ false,
                        0L,
                        Lists.newArrayList(1.0, 2.0, 3.0),
                        Lists.newArrayList(1L, 2L, 3L, 4L)
                    ),
                    ImmutableHistogramPointData.create(
                        1003,
                        1003,
                        Attributes.of(stringKey("key-3"), "value-3"),
                        1100L,
                        /* hasMin= */ false,
                        0L,
                        /* hasMax= */ false,
                        0L,
                        Lists.newArrayList(1.0, 2.0, 3.0),
                        Lists.newArrayList(1L, 2L, 3L, 4L)
                    )
                ))),
        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createDoubleHistogram(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableHistogramData.create(
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableHistogramPointData.create(
                        1001,
                        1001,
                        Attributes.of(stringKey("key-1"), "value-1"),
                        1098L,
                        /* hasMin= */ false,
                        0L,
                        /* hasMax= */ false,
                        0L,
                        Lists.newArrayList(1.0, 2.0, 3.0),
                        Lists.newArrayList(1L, 2L, 3L, 4L)),
                    // We removed the attributes with key "filter-me"
                    ImmutableHistogramPointData.create(
                        1003,
                        1003,
                        Attributes.of(stringKey("key-3"), "value-3"),
                        1100L,
                        /* hasMin= */ false,
                        0L,
                        /* hasMax= */ false,
                        0L,
                        Lists.newArrayList(1.0, 2.0, 3.0),
                        Lists.newArrayList(1L, 2L, 3L, 4L)
                    )
                )
            )
        )
    );
  }

  private static Arguments longSumDataTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createLongSum(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableSumData.create(
                /* isMonotonic= */ true,
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableLongPointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20L),
                    ImmutableLongPointData.create(
                        1002, 1002, Attributes.of(stringKey("filter-me"), "value-2"), 30L),
                    ImmutableLongPointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40L)
                )
            )
        ),

        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createLongSum(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableSumData.create(
                /* isMonotonic= */ true,
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableLongPointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20L),
                    ImmutableLongPointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40L)
                )
            )
        )
    );
  }

  private static Arguments longGaugeDataTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createLongGauge(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableGaugeData.create(
                Lists.newArrayList(
                    ImmutableLongPointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20L),
                    ImmutableLongPointData.create(
                        1002, 1002, Attributes.of(stringKey("filter-me"), "value-2"), 30L),
                    ImmutableLongPointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40L)
                )
            )),
        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createLongGauge(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableGaugeData.create(
                Lists.newArrayList(
                    ImmutableLongPointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20L),
                    ImmutableLongPointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40L)
                )
            )
        )
    );
  }

  private static Arguments doubleGaugeDataTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createDoubleGauge(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableGaugeData.create(
                Lists.newArrayList(
                    ImmutableDoublePointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20.2),
                    ImmutableDoublePointData.create(
                        1002, 1002, Attributes.of(stringKey("filter-me"), "value-2"), 30.3),
                    ImmutableDoublePointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40.4)
                )
            )),
        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createDoubleGauge(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableGaugeData.create(
                Lists.newArrayList(
                    ImmutableDoublePointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20.2),
                    // We removed the attributes with key "filter-me"
                    ImmutableDoublePointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40.4)
                )
            ))
    );
  }

  private static Arguments getDoubleSumTestArguments() {
    return Arguments.of(
        /* inputMetricData */
        ImmutableMetricData.createDoubleSum(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableSumData.create(
                /* isMonotonic= */ true,
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableDoublePointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20.2),
                    ImmutableDoublePointData.create(
                        1002, 1002, Attributes.of(stringKey("filter-me"), "value-2"), 30.3),
                    ImmutableDoublePointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40.4)
                )
            )),
        /* expectedMetricData */
        // Same as original metric data, but without the point with attributes key "filter-me"
        ImmutableMetricData.createDoubleSum(
            resource,
            InstrumentationScopeInfo.create("instrumentation.scope.2"),
            "double-sum-2",
            "double sum description",
            "1",
            ImmutableSumData.create(
                /* isMonotonic= */ true,
                /* temporality= */ CUMULATIVE,
                Lists.newArrayList(
                    ImmutableDoublePointData.create(
                        1001, 1001, Attributes.of(stringKey("key-1"), "value-1"), 20.2),
                    // We removed the attributes with key "filter-me"
                    ImmutableDoublePointData.create(
                        1003, 1003, Attributes.of(stringKey("key-3"), "value-3"), 40.4)
                )
            )
        )
    );
  }

  private static class ControlledMetricProducer implements MetricProducer {
    private Collection<MetricData> produceResult;

    public void setProduceResult(Collection<MetricData> produceResult) {
      this.produceResult = produceResult;
    }

    @Override
    public Collection<MetricData> produce(Resource resource) {
      return produceResult;
    }
  }
}
