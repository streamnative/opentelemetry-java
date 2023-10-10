package io.opentelemetry.sdk.metrics.internal.state.tester;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.internal.state.TestInstrumentType;
import io.opentelemetry.sdk.metrics.internal.state.TestInstrumentType.EmptyInstrumentsState;

import java.util.List;
import java.util.Random;

public class AsyncCounterTester implements TestInstrumentType.InstrumentTester {
  @Override
  public Aggregation testedAggregation() {
    return Aggregation.sum();
  }

  @Override
  public TestInstrumentType.TestInstrumentsState buildInstruments(
      double instrumentCount,
      SdkMeterProvider sdkMeterProvider,
      List<Attributes> attributesList,
      Random random) {
    for (int i = 0; i < instrumentCount; i++) {
      sdkMeterProvider
          .get("meter")
          .counterBuilder("counter" + i)
          .buildWithCallback(
              observableLongMeasurement -> {
                for (int j = 0; j < attributesList.size(); j++) {
                  Attributes attributes = attributesList.get(j);
                  observableLongMeasurement.record(random.nextInt(10_000), attributes);
                }
              });

    }
    return new EmptyInstrumentsState();
  }

  @Override
  public void recordValuesInInstruments(
      TestInstrumentType.TestInstrumentsState testInstrumentsState,
      List<Attributes> attributesList,
      Random random) {
    // No need, all done via the callbacks
  }
}
