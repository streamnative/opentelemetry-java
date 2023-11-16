/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.metrics.internal.state;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import javax.annotation.Nullable;

/**
 * Concurrent Array-based Stack.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 *
 * <p>This class is thread-safe for the specific concurrent use-case described.
 */
public final class ConcurrentArrayBasedStack<T> {

  static final int DEFAULT_CAPACITY = 10;

  private volatile AtomicReferenceArray<T> array;

  private final AtomicInteger size;

  public ConcurrentArrayBasedStack() {
    array = new AtomicReferenceArray<>(DEFAULT_CAPACITY);
    size = new AtomicInteger(0);
  }

  /**
   * Add {@code element} to the top of the stack (LIFO).
   *
   * @param element The element to add
   * @throws NullPointerException if {@code element} is null
   */
  public void push(T element) {
    if (element == null) {
      throw new NullPointerException("Null is not permitted as element in the stack");
    }
    int currentSize = size.get();
    // Rectify negative sizes
    if (currentSize < 0) {
      currentSize = 0;
      size.set(0);
    }
    if (currentSize == array.length()) {
      resizeArray(array.length() * 2);
    }
    array.set(currentSize, element);
    size.incrementAndGet();
  }

  /**
   * Removes and returns an element from the top of the stack (LIFO).
   *
   * @return the top most element in the stack (last one added)
   */
  @Nullable
  public T pop() {
    int currentSize = size.decrementAndGet();
    if (currentSize < 0) {
      size.set(0);
      return null;
    }
    T element = array.get(currentSize);
    array.set(currentSize, null);
    return element;
  }

  public boolean isEmpty() {
    return size.get() <= 0;
  }

  @SuppressWarnings("ManualMinMaxCalculation")
  public int size() {
    int currentSize = size.get();
    return currentSize < 0 ? 0 : currentSize;
  }

  private void resizeArray(int newCapacity) {
    AtomicReferenceArray<T> newArray = new AtomicReferenceArray<>(newCapacity);
    int currentSize = size.get();
    for (int i = 0; i < currentSize; i++) {
      newArray.set(i, array.get(i));
    }
    array = newArray;
  }
}
