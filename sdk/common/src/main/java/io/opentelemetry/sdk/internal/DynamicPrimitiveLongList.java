/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.sdk.internal;

import java.util.AbstractList;

/**
 * A dynamic list of primitive long values.
 *
 * <p>This list dynamically manages sub-arrays of primitive long values to allow dynamic resizing
 * while avoiding the overhead of boxing in Long objects. The list can be resized, and values can be
 * accessed as primitive longs.
 *
 * <p><b>Supported {@code List<Long>} methods:</b>
 *
 * <ul>
 *   <li>{@link #get(int)} - retrieves the element at the specified position in this list.
 *   <li>{@link #set(int, Long)} - replaces the element at the specified position in this list with
 *       the specified element.
 *   <li>{@link #size()} - returns the number of elements in this list.
 * </ul>
 *
 * <p><b>Additional utility methods:</b>
 *
 * <ul>
 *   <li>{@link #getLong(int)} - retrieves the element at the specified position in this list as a
 *       primitive long.
 *   <li>{@link #setLong(int, long)} - replaces the element at the specified position in this list
 *       with the specified primitive long element.
 *   <li>{@link #resetAndResizeTo(int)} - resets and resizes the list to the specified size.
 * </ul>
 *
 * <p>Note: This implementation does not support adding or removing elements after the list is
 * created.
 */
public class DynamicPrimitiveLongList extends AbstractList<Long> {
  private static final int DEFAULT_SUBARRAY_CAPACITY = 10;
  private final int subarrayCapacity;
  private long[][] arrays;
  private int size;
  private int arrayCount;

  public DynamicPrimitiveLongList() {
    this(DEFAULT_SUBARRAY_CAPACITY);
  }

  public DynamicPrimitiveLongList(int subarrayCapacity) {
    if (subarrayCapacity <= 0) {
      throw new IllegalArgumentException("Subarray capacity must be positive");
    }
    this.subarrayCapacity = subarrayCapacity;
    arrays = new long[1][subarrayCapacity];
    arrayCount = 1;
    size = 0;
  }

  @Override
  public Long get(int index) {
    return getLong(index);
  }

  public long getLong(int index) {
    rangeCheck(index);
    return arrays[index / subarrayCapacity][index % subarrayCapacity];
  }

  @Override
  public Long set(int index, Long element) {
    return setLong(index, element);
  }

  /**
   * Replaces the element at the specified position in this list with the specified element.
   *
   * @param index index of the element to replace
   * @param element element to be stored at the specified position
   * @return the element previously at the specified position
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public long setLong(int index, long element) {
    ensureCapacity(index + 1);
    if (index >= size) {
      size = index + 1;
    }
    long oldValue = arrays[index / subarrayCapacity][index % subarrayCapacity];
    arrays[index / subarrayCapacity][index % subarrayCapacity] = element;
    return oldValue;
  }

  /** Returns the number of elements in this list. */
  @Override
  public int size() {
    return size;
  }

  /**
   * Empties the list and resizes its internal capacity to a new value.
   *
   * @param newCapacity The new capacity of the list
   */
  public void resetAndResizeTo(int newCapacity) {
    if (newCapacity < 0) {
      throw new IllegalArgumentException("New size must be non-negative");
    }
    ensureCapacity(newCapacity);
    size = 0;
  }

  private void ensureCapacity(int minCapacity) {
    // This is equivalent to Math.ceil(minCapacity / subArrayCapacity)
    int requiredArrays = (minCapacity + subarrayCapacity - 1) / subarrayCapacity;

    if (requiredArrays > arrayCount) {
      arrays = java.util.Arrays.copyOf(arrays, requiredArrays);
      for (int i = arrayCount; i < requiredArrays; i++) {
        arrays[i] = new long[subarrayCapacity];
      }
      arrayCount = requiredArrays;
    }
  }

  private void rangeCheck(int index) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException(outOfBoundsMsg(index));
    }
  }

  private String outOfBoundsMsg(int index) {
    return "Index: " + index + ", Size: " + size;
  }
}
