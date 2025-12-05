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

import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Constructor;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import javax.annotation.Nullable;

/**
 * Pool that creates new instances of a type using reflection. The type must have a zero argument
 * constructor. {@link Constructor#setAccessible(boolean)} will be used if the class and/or
 * constructor is not visible.
 *
 * @author Nathan Sweet
 */
public class ReflectionPool<T> extends Pool<T> {
  // May be null if no suitable constructor is found; guarded by runtime checks.
  private final @Nullable Constructor constructor;

  public ReflectionPool(Class<T> type) {
    this(type, 16, Integer.MAX_VALUE);
  }

  public ReflectionPool(Class<T> type, int initialCapacity) {
    this(type, initialCapacity, Integer.MAX_VALUE);
  }

  public ReflectionPool(Class<T> type, int initialCapacity, int max) {
    super(initialCapacity, max);
    constructor = findConstructor(type);
    if (constructor == null)
      throw new RuntimeException(
          "Class cannot be created (missing no-arg constructor): " + type.getName());
  }

  @Nullable
  private @Null Constructor findConstructor(Class<T> type) {
    try {
      // Use the no-arg overload instead of passing a null Class[] to a non-null varargs parameter.
      return ClassReflection.getConstructor(type);
    } catch (Exception ex1) {
      try {
        Constructor constructor = ClassReflection.getDeclaredConstructor(type);
        constructor.setAccessible(true);
        return constructor;
      } catch (ReflectionException ex2) {
        return null;
      }
    }
  }

  @SuppressWarnings("ConstantConditions") // constructor is checked for null in the constructor.
  protected T newObject() {
    try {
      if (constructor == null) {
        throw new GdxRuntimeException(
            "Constructor for pooled type was not found: " + getClass().getName());
      }
      // No-arg constructor; avoid passing a nullable varargs array.
      return (T) constructor.newInstance();
    } catch (Exception ex) {
      throw new GdxRuntimeException(
          "Unable to create new instance: " + constructor.getDeclaringClass().getName(), ex);
    }
  }
}
