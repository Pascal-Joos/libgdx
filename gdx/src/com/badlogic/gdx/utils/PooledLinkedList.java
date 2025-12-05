/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 ******************************************************************************/

package com.badlogic.gdx.utils;

import com.badlogic.gdx.utils.Pool.Poolable;
import javax.annotation.Nullable;

/** A pooled, doubly-linked list. */
public class PooledLinkedList<T> {

  private static class Item<T> implements Poolable {
    @Nullable T payload;
    @Nullable Item<T> next;
    @Nullable Item<T> prev;

    @Override
    public void reset() {
      payload = null;
      next = null;
      prev = null;
    }
  }

  private final Pool<Item<T>> pool;
  @Nullable private Item<T> head;
  @Nullable private Item<T> tail;
  @Nullable private Item<T> iter;
  @Nullable private Item<T> curr;
  private int size;

  public PooledLinkedList() {
    this(16);
  }

  public PooledLinkedList(int maxPoolSize) {
    pool =
        new Pool<Item<T>>(16, maxPoolSize) {
          @Override
          protected Item<T> newObject() {
            return new Item<T>();
          }
        };
  }

  public void add(T object) {
    Item<T> item = pool.obtain();
    item.payload = object;
    item.next = null;
    item.prev = tail;

    if (head == null) {
      head = item;
      tail = item;
    } else {
      if (tail != null) {
        tail.next = item;
      }
      tail = item;
    }

    size++;
  }

  public void addFirst(T object) {
    Item<T> item = pool.obtain();
    item.payload = object;
    item.prev = null;
    item.next = head;

    if (head == null) {
      head = item;
      tail = item;
    } else {
      head.prev = item;
      head = item;
    }

    size++;
  }

  public void iter() {
    iter = head;
    curr = null;
  }

  @Nullable
  public T next() {
    if (iter == null) return null;

    T payload = iter.payload;
    curr = iter;
    iter = iter.next;
    return payload;
  }

  @Nullable
  public T previous() {
    if (iter == null) return null;

    T payload = iter.payload;
    curr = iter;
    iter = iter.prev;
    return payload;
  }

  /** Removes the current list item based on the iterator position. */
  public void remove() {
    if (curr == null) return;

    size--;

    Item<T> c = curr;
    Item<T> n = curr.next;
    Item<T> p = curr.prev;
    pool.free(curr);
    curr = null;

    if (size == 0) {
      head = null;
      tail = null;
      return;
    }

    if (c == head) {
      if (n != null) {
        n.prev = null;
      }
      head = n;
      return;
    }

    if (c == tail) {
      if (p != null) {
        p.next = null;
      }
      tail = p;
      return;
    }

    // Middle element: both neighbors must be non-null, but guard for safety/nullness analysis
    if (p == null || n == null) return;

    p.next = n;
    n.prev = p;
  }

  /** Removes the tail of the list regardless of iteration status */
  @Nullable
  public T removeLast() {
    if (tail == null) {
      return null;
    }

    T payload = tail.payload;

    size--;

    Item<T> p = tail.prev;
    pool.free(tail);

    if (size == 0) {
      head = null;
      tail = null;
    } else {
      tail = p;
      if (tail != null) {
        tail.next = null;
      }
    }

    return payload;
  }

  public void clear() {
    iter();
    T v = null;
    while ((v = next()) != null) remove();
  }
}
