/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.utils;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.reflect.ArrayReflection;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

/**
 * An ordered or unordered map of objects. This implementation uses arrays to store the keys and
 * values, which means {@link #getKey(Object, boolean) gets} do a comparison for each key in the
 * map. This is slower than a typical hash map implementation, but may be acceptable for small maps
 * and has the benefits that keys and values can be accessed by index, which makes iteration fast.
 * Like {@link Array}, if ordered is false, this class avoids a memory copy when removing elements
 * (the last element is moved to the removed element's position).
 *
 * @author Nathan Sweet
 */
public class ArrayMap<K, V> implements Iterable<ObjectMap.Entry<K, V>> {
  public K[] keys;
  public V[] values;
  public int size;
  public boolean ordered;

  @Nullable private transient Entries entries1 = null, entries2 = null;
  @Nullable private transient Values values1 = null, values2 = null;
  @Nullable private transient Keys keys1 = null, keys2 = null;

  /** Creates an ordered map with a capacity of 16. */
  public ArrayMap() {
    this(true, 16);
  }

  /** Creates an ordered map with the specified capacity. */
  public ArrayMap(int capacity) {
    this(true, capacity);
  }

  /**
   * @param ordered If false, methods that remove elements may change the order of other elements in
   *     the arrays, which avoids a memory copy.
   * @param capacity Any elements added beyond this will cause the backing arrays to be grown.
   */
  public ArrayMap(boolean ordered, int capacity) {
    this.ordered = ordered;
    keys = (K[]) new Object[capacity];
    values = (V[]) new Object[capacity];
  }

  /**
   * Creates a new map with {@link #keys} and {@link #values} of the specified type.
   *
   * @param ordered If false, methods that remove elements may change the order of other elements in
   *     the arrays, which avoids a memory copy.
   * @param capacity Any elements added beyond this will cause the backing arrays to be grown.
   */
  public ArrayMap(boolean ordered, int capacity, Class keyArrayType, Class valueArrayType) {
    this.ordered = ordered;
    keys = (K[]) ArrayReflection.newInstance(keyArrayType, capacity);
    values = (V[]) ArrayReflection.newInstance(valueArrayType, capacity);
  }

  /**
   * Creates an ordered map with {@link #keys} and {@link #values} of the specified type and a
   * capacity of 16.
   */
  public ArrayMap(Class keyArrayType, Class valueArrayType) {
    this(false, 16, keyArrayType, valueArrayType);
  }

  /**
   * Creates a new map containing the elements in the specified map. The new map will have the same
   * type of backing arrays and will be ordered if the specified map is ordered. The capacity is set
   * to the number of elements, so any subsequent elements added will cause the backing arrays to be
   * grown.
   */
  public ArrayMap(ArrayMap array) {
    this(
        array.ordered,
        array.size,
        array.keys.getClass().getComponentType(),
        array.values.getClass().getComponentType());
    size = array.size;
    System.arraycopy(array.keys, 0, keys, 0, size);
    System.arraycopy(array.values, 0, values, 0, size);
  }

  public int put(@Nullable K key, V value) {
    int index = indexOfKey(key);
    if (index == -1) {
      if (size == keys.length) resize(Math.max(8, (int) (size * 1.75f)));
      index = size++;
    }
    keys[index] = key;
    values[index] = value;
    return index;
  }

  public int put(K key, V value, int index) {
    int existingIndex = indexOfKey(key);
    if (existingIndex != -1) removeIndex(existingIndex);
    else if (size == keys.length) //
    resize(Math.max(8, (int) (size * 1.75f)));
    System.arraycopy(keys, index, keys, index + 1, size - index);
    System.arraycopy(values, index, values, index + 1, size - index);
    keys[index] = key;
    values[index] = value;
    size++;
    return index;
  }

  public void putAll(ArrayMap<? extends K, ? extends V> map) {
    putAll(map, 0, map.size);
  }

  public void putAll(ArrayMap<? extends K, ? extends V> map, int offset, int length) {
    if (offset + length > map.size)
      throw new IllegalArgumentException(
          "offset + length must be <= size: " + offset + " + " + length + " <= " + map.size);
    int sizeNeeded = size + length - offset;
    if (sizeNeeded >= keys.length) resize(Math.max(8, (int) (sizeNeeded * 1.75f)));
    System.arraycopy(map.keys, offset, keys, size, length);
    System.arraycopy(map.values, offset, values, size, length);
    size += length;
  }

  /**
   * Returns the value (which may be null) for the specified key, or null if the key is not in the
   * map. Note this does a .equals() comparison of each key in reverse order until the specified key
   * is found.
   */
  @Nullable
  public @Null V get(K key) {
    return get(key, null);
  }

  /**
   * Returns the value (which may be null) for the specified key, or the default value if the key is
   * not in the map. Note this does a .equals() comparison of each key in reverse order until the
   * specified key is found.
   */
  @Nullable
  public @Null V get(K key, @Nullable @Null V defaultValue) {
    Object[] keys = this.keys;
    int i = size - 1;
    if (key == null) {
      for (; i >= 0; i--) if (keys[i] == key) return values[i];
    } else {
      for (; i >= 0; i--) if (key.equals(keys[i])) return values[i];
    }
    return defaultValue;
  }

  /**
   * Returns the key for the specified value. Note this does a comparison of each value in reverse
   * order until the specified value is found.
   *
   * @param identity If true, == comparison will be used. If false, .equals() comparison will be
   *     used.
   */
  @Nullable
  public @Null K getKey(V value, boolean identity) {
    Object[] values = this.values;
    int i = size - 1;
    if (identity || value == null) {
      for (; i >= 0; i--) if (values[i] == value) return keys[i];
    } else {
      for (; i >= 0; i--) if (value.equals(values[i])) return keys[i];
    }
    return null;
  }

  public K getKeyAt(int index) {
    if (index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
    return keys[index];
  }

  public V getValueAt(int index) {
    if (index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
    return values[index];
  }

  public K firstKey() {
    if (size == 0) throw new IllegalStateException("Map is empty.");
    return keys[0];
  }

  public V firstValue() {
    if (size == 0) throw new IllegalStateException("Map is empty.");
    return values[0];
  }

  public void setKey(int index, K key) {
    if (index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
    keys[index] = key;
  }

  public void setValue(int index, V value) {
    if (index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
    values[index] = value;
  }

  public void insert(int index, K key, V value) {
    if (index > size) throw new IndexOutOfBoundsException(String.valueOf(index));
    if (size == keys.length) resize(Math.max(8, (int) (size * 1.75f)));
    if (ordered) {
      System.arraycopy(keys, index, keys, index + 1, size - index);
      System.arraycopy(values, index, values, index + 1, size - index);
    } else {
      keys[size] = keys[index];
      values[size] = values[index];
    }
    size++;
    keys[index] = key;
    values[index] = value;
  }

  public boolean containsKey(K key) {
    K[] keys = this.keys;
    int i = size - 1;
    if (key == null) {
      while (i >= 0) if (keys[i--] == key) return true;
    } else {
      while (i >= 0) if (key.equals(keys[i--])) return true;
    }
    return false;
  }

  /**
   * @param identity If true, == comparison will be used. If false, .equals() comparison will be
   *     used.
   */
  public boolean containsValue(V value, boolean identity) {
    V[] values = this.values;
    int i = size - 1;
    if (identity || value == null) {
      while (i >= 0) if (values[i--] == value) return true;
    } else {
      while (i >= 0) if (value.equals(values[i--])) return true;
    }
    return false;
  }

  public int indexOfKey(@Nullable K key) {
    Object[] keys = this.keys;
    if (key == null) {
      for (int i = 0, n = size; i < n; i++) if (keys[i] == key) return i;
    } else {
      for (int i = 0, n = size; i < n; i++) if (key.equals(keys[i])) return i;
    }
    return -1;
  }

  public int indexOfValue(V value, boolean identity) {
    Object[] values = this.values;
    if (identity || value == null) {
      for (int i = 0, n = size; i < n; i++) if (values[i] == value) return i;
    } else {
      for (int i = 0, n = size; i < n; i++) if (value.equals(values[i])) return i;
    }
    return -1;
  }

  @Nullable
  public @Null V removeKey(K key) {
    Object[] keys = this.keys;
    if (key == null) {
      for (int i = 0, n = size; i < n; i++) {
        if (keys[i] == key) {
          V value = values[i];
          removeIndex(i);
          return value;
        }
      }
    } else {
      for (int i = 0, n = size; i < n; i++) {
        if (key.equals(keys[i])) {
          V value = values[i];
          removeIndex(i);
          return value;
        }
      }
    }
    return null;
  }

  public boolean removeValue(V value, boolean identity) {
    Object[] values = this.values;
    if (identity || value == null) {
      for (int i = 0, n = size; i < n; i++) {
        if (values[i] == value) {
          removeIndex(i);
          return true;
        }
      }
    } else {
      for (int i = 0, n = size; i < n; i++) {
        if (value.equals(values[i])) {
          removeIndex(i);
          return true;
        }
      }
    }
    return false;
  }

  /** Removes and returns the key/values pair at the specified index. */
  public void removeIndex(int index) {
    if (index >= size) throw new IndexOutOfBoundsException(String.valueOf(index));
    Object[] keys = this.keys;
    size--;
    if (ordered) {
      System.arraycopy(keys, index + 1, keys, index, size - index);
      System.arraycopy(values, index + 1, values, index, size - index);
    } else {
      keys[index] = keys[size];
      values[index] = values[size];
    }
    keys[size] = null;
    values[size] = null;
  }

  /** Returns true if the map has one or more items. */
  public boolean notEmpty() {
    return size > 0;
  }

  /** Returns true if the map is empty. */
  public boolean isEmpty() {
    return size == 0;
  }

  /** Returns the last key. */
  public K peekKey() {
    return keys[size - 1];
  }

  /** Returns the last value. */
  public V peekValue() {
    return values[size - 1];
  }

  /**
   * Clears the map and reduces the size of the backing arrays to be the specified capacity if they
   * are larger.
   */
  public void clear(int maximumCapacity) {
    if (keys.length <= maximumCapacity) {
      clear();
      return;
    }
    size = 0;
    resize(maximumCapacity);
  }

  public void clear() {
    Arrays.fill(keys, 0, size, null);
    Arrays.fill(values, 0, size, null);
    size = 0;
  }

  /**
   * Reduces the size of the backing arrays to the size of the actual number of entries. This is
   * useful to release memory when many items have been removed, or if it is known that more entries
   * will not be added.
   */
  public void shrink() {
    if (keys.length == size) return;
    resize(size);
  }

  /**
   * Increases the size of the backing arrays to accommodate the specified number of additional
   * entries. Useful before adding many entries to avoid multiple backing array resizes.
   */
  public void ensureCapacity(int additionalCapacity) {
    if (additionalCapacity < 0)
      throw new IllegalArgumentException("additionalCapacity must be >= 0: " + additionalCapacity);
    int sizeNeeded = size + additionalCapacity;
    if (sizeNeeded > keys.length) resize(Math.max(Math.max(8, sizeNeeded), (int) (size * 1.75f)));
  }

  protected void resize(int newSize) {
    K[] newKeys = (K[]) ArrayReflection.newInstance(keys.getClass().getComponentType(), newSize);
    System.arraycopy(keys, 0, newKeys, 0, Math.min(size, newKeys.length));
    this.keys = newKeys;

    V[] newValues =
        (V[]) ArrayReflection.newInstance(values.getClass().getComponentType(), newSize);
    System.arraycopy(values, 0, newValues, 0, Math.min(size, newValues.length));
    this.values = newValues;
  }

  public void reverse() {
    for (int i = 0, lastIndex = size - 1, n = size / 2; i < n; i++) {
      int ii = lastIndex - i;
      K tempKey = keys[i];
      keys[i] = keys[ii];
      keys[ii] = tempKey;

      V tempValue = values[i];
      values[i] = values[ii];
      values[ii] = tempValue;
    }
  }

  public void shuffle() {
    for (int i = size - 1; i >= 0; i--) {
      int ii = MathUtils.random(i);
      K tempKey = keys[i];
      keys[i] = keys[ii];
      keys[ii] = tempKey;

      V tempValue = values[i];
      values[i] = values[ii];
      values[ii] = tempValue;
    }
  }

  /**
   * Reduces the size of the arrays to the specified size. If the arrays are already smaller than
   * the specified size, no action is taken.
   */
  public void truncate(int newSize) {
    if (size <= newSize) return;
    for (int i = newSize; i < size; i++) {
      keys[i] = null;
      values[i] = null;
    }
    size = newSize;
  }

  public int hashCode() {
    K[] keys = this.keys;
    V[] values = this.values;
    int h = 0;
    for (int i = 0, n = size; i < n; i++) {
      K key = keys[i];
      V value = values[i];
      if (key != null) h += key.hashCode() * 31;
      if (value != null) h += value.hashCode();
    }
    return h;
  }

  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof ArrayMap)) return false;
    ArrayMap other = (ArrayMap) obj;
    if (other.size != size) return false;
    K[] keys = this.keys;
    V[] values = this.values;
    for (int i = 0, n = size; i < n; i++) {
      K key = keys[i];
      V value = values[i];
      if (value == null) {
        if (other.get(key, ObjectMap.dummy) != null) return false;
      } else {
        if (!value.equals(other.get(key))) return false;
      }
    }
    return true;
  }

  /** Uses == for comparison of each value. */
  public boolean equalsIdentity(Object obj) {
    if (obj == this) return true;
    if (!(obj instanceof ArrayMap)) return false;
    ArrayMap other = (ArrayMap) obj;
    if (other.size != size) return false;
    K[] keys = this.keys;
    V[] values = this.values;
    for (int i = 0, n = size; i < n; i++)
      if (values[i] != other.get(keys[i], ObjectMap.dummy)) return false;
    return true;
  }

  public String toString() {
    if (size == 0) return "{}";
    K[] keys = this.keys;
    V[] values = this.values;
    StringBuilder buffer = new StringBuilder(32);
    buffer.append('{');
    buffer.append(keys[0]);
    buffer.append('=');
    buffer.append(values[0]);
    for (int i = 1; i < size; i++) {
      buffer.append(", ");
      buffer.append(keys[i]);
      buffer.append('=');
      buffer.append(values[i]);
    }
    buffer.append('}');
    return buffer.toString();
  }

  public Iterator<Entry<K, V>> iterator() {
    return entries();
  }

  /**
   * Returns an iterator for the entries in the map. Remove is supported.
   *
   * <p>If {@link Collections#allocateIterators} is false, the same iterator instance is returned
   * each time this method is called. Use the {@link Entries} constructor for nested or
   * multithreaded iteration.
   *
   * @see Collections#allocateIterators
   */
  public Entries<K, V> entries() {
    if (Collections.allocateIterators) return new Entries(this);
    if (entries1 == null) {
      entries1 = new Entries(this);
      entries2 = new Entries(this);
    }
    Entries<K, V> e1 = entries1;
    Entries<K, V> e2 = entries2;
    if (!e1.valid) {
      e1.index = 0;
      e1.valid = true;
      if (e2 == null) throw new IllegalStateException("entries2 not initialized");
      e2.valid = false;
      return e1;
    }
    if (e2 == null) throw new IllegalStateException("entries2 not initialized");
    e2.index = 0;
    e2.valid = true;
    e1.valid = false;
    return e2;
  }
  }

  /**
   * Returns an iterator for the values in the map. Remove is supported.
   *
   * <p>If {@link Collections#allocateIterators} is false, the same iterator instance is returned
  public Values<V> values() {
    if (Collections.allocateIterators) return new Values(this);
    if (values1 == null) {
      values1 = new Values(this);
      values2 = new Values(this);
    }
    Values<V> v1 = values1;
    Values<V> v2 = values2;
    if (!v1.valid) {
      v1.index = 0;
      v1.valid = true;
      if (v2 == null) throw new IllegalStateException("values2 not initialized");
      v2.valid = false;
      return v1;
    }
    if (v2 == null) throw new IllegalStateException("values2 not initialized");
    v2.index = 0;
    v2.valid = true;
    v1.valid = false;
    return v2;
  }
    v1.valid = false;
    return v2;
  }

  /**
   * Returns an iterator for the keys in the map. Remove is supported.
   *
   * <p>If {@link Collections#allocateIterators} is false, the same iterator instance is returned
   * each time this method is called. Use the {@link Entries} constructor for nested or
   * multithreaded iteration.
  public Keys<K> keys() {
    if (Collections.allocateIterators) return new Keys(this);
    if (keys1 == null) {
      keys1 = new Keys(this);
      keys2 = new Keys(this);
    }
    Keys<K> k1 = keys1;
    Keys<K> k2 = keys2;
    if (!k1.valid) {
      k1.index = 0;
      k1.valid = true;
      if (k2 == null) throw new IllegalStateException("keys2 not initialized");
      k2.valid = false;
      return k1;
    }
    if (k2 == null) throw new IllegalStateException("keys2 not initialized");
    k2.index = 0;
    k2.valid = true;
    k1.valid = false;
    return k2;
  }
    k1.valid = false;
    return k2;
  }


  public static class Entries<K, V> implements Iterable<Entry<K, V>>, Iterator<Entry<K, V>> {
    private final ArrayMap<K, V> map;
    Entry<K, V> entry = new Entry();
    int index;
    boolean valid = true;

    public Entries(ArrayMap<K, V> map) {
      this.map = map;
    }

    public boolean hasNext() {
      if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
      return index < map.size;
    }

    public Iterator<Entry<K, V>> iterator() {
      return this;
    }

    /** Note the same entry instance is returned each time this method is called. */
    public Entry<K, V> next() {
      if (index >= map.size) throw new NoSuchElementException(String.valueOf(index));
      if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
      entry.key = map.keys[index];
      entry.value = map.values[index++];
      return entry;
    }

    public void remove() {
      index--;
      map.removeIndex(index);
    }

    public void reset() {
      index = 0;
    }
  }

  public static class Values<V> implements Iterable<V>, Iterator<V> {
    private final ArrayMap<Object, V> map;
    int index;
    boolean valid = true;

    public Values(ArrayMap<Object, V> map) {
      this.map = map;
    }

    public boolean hasNext() {
      if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
      return index < map.size;
    }

    public Iterator<V> iterator() {
      return this;
    }

    public V next() {
      if (index >= map.size) throw new NoSuchElementException(String.valueOf(index));
      if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
      return map.values[index++];
    }

    public void remove() {
      index--;
      map.removeIndex(index);
    }

    public void reset() {
      index = 0;
    }

    public Array<V> toArray() {
      return new Array(true, map.values, index, map.size - index);
    }

    public Array<V> toArray(Array array) {
      array.addAll(map.values, index, map.size - index);
      return array;
    }
  }

  public static class Keys<K> implements Iterable<K>, Iterator<K> {
    private final ArrayMap<K, Object> map;
    int index;
    boolean valid = true;

    public Keys(ArrayMap<K, Object> map) {
      this.map = map;
    }

    public boolean hasNext() {
      if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
      return index < map.size;
    }

    public Iterator<K> iterator() {
      return this;
    }

    public K next() {
      if (index >= map.size) throw new NoSuchElementException(String.valueOf(index));
      if (!valid) throw new GdxRuntimeException("#iterator() cannot be used nested.");
      return map.keys[index++];
    }

    public void remove() {
      index--;
      map.removeIndex(index);
    }

    public void reset() {
      index = 0;
    }

    public Array<K> toArray() {
      return new Array(true, map.keys, index, map.size - index);
    }

    public Array<K> toArray(Array array) {
      array.addAll(map.keys, index, map.size - index);
      return array;
    }
  }
}
