package io.opentelemetry.sdk.metrics.internal.state;

import io.opentelemetry.sdk.common.export.MemoryMode;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import java.lang.reflect.InvocationTargetException;

/**
 * <p>This benchmark class is used to see memory allocation flame
 * graphs for a single run.
 *
 * <p>Steps:
 *
 * <ol>
 *   <li>Follow download instructions for async-profiler, located at
 *       https://github.com/async-profiler/async-profiler
 *   <li>Assuming you have extracted it at /tmp/async-profiler-2.9-macos, add the following to your
 *       JVM arguments of your run configuration:
 *       <pre>
 *       -agentpath:/tmp/async-profiler-2.9-macos/build/libasyncProfiler.so=start,event=alloc,flamegraph,file=/tmp/profiled_data.html
 *       </pre>
 *   <li>Tune the parameters as you see fit
 *   <li>Run the class
 *   <li>Open /tmp/profiled_data.html with your browser
 * </ol>
 */
public class ProfileBenchmark {

  private ProfileBenchmark() {}

  public static void main(String[] args)
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    // Parameters
    AggregationTemporality aggregationTemporality = AggregationTemporality.DELTA;
    MemoryMode memoryMode = MemoryMode.REUSABLE_DATA;
    TestInstrumentType testInstrumentType = TestInstrumentType.EXPONENTIAL_HISTOGRAM;

    InstrumentGarbageCollectionBenchmark.ThreadState benchmarkSetup =
        new InstrumentGarbageCollectionBenchmark.ThreadState();

    benchmarkSetup.aggregationTemporality = aggregationTemporality;
    benchmarkSetup.memoryMode = memoryMode;
    benchmarkSetup.testInstrumentType = testInstrumentType;

    InstrumentGarbageCollectionBenchmark benchmark =
        new InstrumentGarbageCollectionBenchmark();


    benchmarkSetup.setup();
    //benchmark.recordAndCollect(benchmarkSetup);

    warmup(benchmark, benchmarkSetup);
    measure(benchmark, benchmarkSetup);
  }

  public static void warmup(
      InstrumentGarbageCollectionBenchmark benchmark,
      InstrumentGarbageCollectionBenchmark.ThreadState benchmarkSetup)
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    for (int i = 0; i < 10; i++) {
      benchmark.recordAndCollect(benchmarkSetup);
    }
  }

  public static void measure(
      InstrumentGarbageCollectionBenchmark benchmark,
      InstrumentGarbageCollectionBenchmark.ThreadState benchmarkSetup)
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    for (int i = 0; i < 200; i++) {
      benchmark.recordAndCollect(benchmarkSetup);
    }
  }
}
