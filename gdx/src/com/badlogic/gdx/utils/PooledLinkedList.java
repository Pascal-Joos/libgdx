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

import com.uber.nullaway.annotations.Initializer;
import javax.annotation.Nullable;

/**
 * A simple linked list that pools its nodes.
 *
 * @author mzechner
 */
public class PooledLinkedList<T> {
  static final class Item<T> {
    @Nullable public T payload;
    @Nullable public Item<T> next;
    @Nullable public Item<T> prev;
  }

  @Nullable private Item<T> head;
  @Nullable private Item<T> tail;
  @Nullable private Item<T> iter;
  @Nullable private Item<T> curr;
  private int size = 0;

  private final Pool<Item<T>> pool;

  public PooledLinkedList(int maxPoolSize) {
    this.pool =
        new Pool<Item<T>>(16, maxPoolSize) {
          @Override
          protected Item<T> newObject() {
            return new Item<T>();
          }
        };
  }

  /** Adds the specified object to the end of the list regardless of iteration status */
  /** Adds the specified object to the end of the list regardless of iteration status */
  @Initializer
  public void add(T object) {
    Item<T> item = pool.obtain();
    item.payload = object;
    item.next = null;
    item.prev = null;

    if (head == null) {
      head = item;
      tail = item;
      size++;
      return;
    }

    Item<T> currentTail = tail;
    if (currentTail == null) {
      return;
    }

    item.prev = currentTail;
    currentTail.next = item;
    tail = item;
    size++;
  }

  /** Adds the specified object to the head of the list regardless of iteration status */
  public void addFirst(T object) {
    Item<T> item = pool.obtain();
    item.payload = object;
    item.next = head;
    item.prev = null;

    if (head != null) {
      head.prev = item;
    } else {
      tail = item;
    }

    head = item;

    size++;
  }

  /** Returns the number of items in the list */
  public int size() {
    return size;
  }

  /** Starts iterating over the list's items from the head of the list */
  public void iter() {
    iter = head;
  }

  /** Starts iterating over the list's items from the tail of the list */
  public void iterReverse() {
    iter = tail;
  }

  /**
   * Gets the next item in the list
   *
   * @return the next item in the list or null if there are no more items
   */
  @Nullable
  public @Null T next() {
    if (iter == null) return null;

    T payload = iter.payload;
    curr = iter;
    iter = iter.next;
    return payload;
  }

  /**
   * Gets the previous item in the list
   *
   * @return the previous item in the list or null if there are no more items
   */
  @Nullable
  public @Null T previous() {
    if (iter == null) return null;

    T payload = iter.payload;
    curr = iter;
    iter = iter.prev;
    return payload;
  }

  /** Removes the current list item based on the iterator position. */
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

    if (p != null) {
      p.next = n;
    }
    if (n != null) {
      n.prev = p;
    }
  }

  /** Removes the tail of the list regardless of iteration status */
  /** Removes the tail of the list regardless of iteration status */
  @Nullable
  public @Null T removeLast() {
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
