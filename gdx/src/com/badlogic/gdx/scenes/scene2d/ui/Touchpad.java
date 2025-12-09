/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/

package com.badlogic.gdx.scenes.scene2d.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Pools;
import javax.annotation.Nullable;

/**
 * A touchpad is a widget that allows the user to control a character or other object in a game. It
 * is a circular area that the user can touch and drag to move the knob within the bounds of the
 * circle. The knob's position is used to determine the direction and magnitude of the movement.
 *
 * <p>The touchpad has a deadzone, which is a circular area in the center of the touchpad where the
 * knob will not move. This is useful for preventing small, unintentional movements of the knob.
 *
 * <p>The touchpad also has a resetOnTouchUp flag, which determines whether the knob will return to
 * the center of the touchpad when the user lifts their finger. If this flag is set to false, the
 * knob will remain in its current position when the user lifts their finger, and the user can
 * resume control by touching the touchpad again. If this flag is set to true, the knob will return
 * to the center of the touchpad when the user lifts their finger, and the user will need to touch
 * the touchpad again to resume control.
 *
 * <p>The touchpad can be used in conjunction with a {@link Skin} to provide a visual representation
 * of the touchpad and knob. The skin can be used to customize the appearance of the touchpad and
 * knob, including their size, shape, and color.
 *
 * <p>The touchpad can be used in a variety of games and applications, including platformers, racing
 * games, and other games that require precise control of a character or object. It can also be used
 * in non-game applications, such as drawing or painting applications, where precise control of a
 * cursor or brush is required.
 *
 * <p>The touchpad is a versatile and powerful widget that can be used to provide intuitive and
 * responsive control in a wide range of applications.
 *
 * <p>Note that the touchpad's behavior can be customized by overriding the {@link
 * #calculatePositionAndValue(float, float, boolean)} method. This method is called whenever the
 * user touches, drags, or lifts their finger from the touchpad, and is responsible for updating the
 * knob's position and the touchpad's value based on the user's input.
 *
 * <p>The touchpad also provides methods for querying the knob's position and the touchpad's value,
 * including {@link #getKnobX()}, {@link #getKnobY()}, {@link #getKnobPercentX()}, and {@link
 * #getKnobPercentY()}. These methods can be used to determine the direction and magnitude of the
 * user's input, and can be used to control a character or object in a game or application.
 *
 * <p>The touchpad is a powerful and flexible widget that can be used to provide intuitive and
 * responsive control in a wide range of applications.
 *
 * <p>Note that the touchpad's behavior can be customized by overriding the {@link
 * #calculatePositionAndValue(float, float, boolean)} method. This method is called whenever the
 * user touches, drags, or lifts their finger from the touchpad, and is responsible for updating the
 * knob's position and the touchpad's value based on the user's input.
 *
 * <p>The touchpad also provides methods for querying the knob's position and the touchpad's value,
 * including {@link #getKnobX()}, {@link #getKnobY()}, {@link #getKnobPercentX()}, and {@link
 * #getKnobPercentY()}. These methods can be used to determine the direction and magnitude of the
 * user's input, and can be used to control a character or object in a game or application.
 *
 * <p>The touchpad is a powerful and flexible widget that can be used to provide intuitive and
 * responsive control in a wide range of applications.
 *
 * <p>Note that the touchpad's behavior can be customized by overriding the {@link
 * #calculatePositionAndValue(float, float, boolean)} method. This method is called whenever the
 * user touches, drags, or lifts their finger from the touchpad, and is responsible for updating the
 * knob's position and the touchpad's value based on the user's input.
 *
 * <p>The touchpad also provides methods for querying the knob's position and the touchpad's value,
 * including {@link #getKnobX()}, {@link #getKnobY()}, {@link #getKnobPercentX()}, and {@link
 * #getKnobPercentY()}. These methods can be used to determine the direction and magnitude of the
 * user's input, and can be used to control a character or object in a game or application.
 *
 * <p>The touchpad is a powerful and flexible widget that can be used to provide intuitive and
 * responsive control in a wide range of applications.
 *
 * @author Josh Street
 */
public class Touchpad extends Widget {
  private TouchpadStyle style;
  boolean touched;
  boolean resetOnTouchUp = true;
  private float deadzoneRadius;
  private final Circle knobBounds = new Circle(0, 0, 0);
  private final Circle touchBounds = new Circle(0, 0, 0);
  private final Circle deadzoneBounds = new Circle(0, 0, 0);
  private final Vector2 knobPosition = new Vector2();
  private final Vector2 knobPercent = new Vector2();

  /**
   * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the
   *     knob to be moved.
   */
  public Touchpad(float deadzoneRadius, Skin skin) {
    this(deadzoneRadius, skin.get(TouchpadStyle.class));
  }

  /**
   * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the
   *     knob to be moved.
   */
  public Touchpad(float deadzoneRadius, Skin skin, String styleName) {
    this(deadzoneRadius, skin.get(styleName, TouchpadStyle.class));
  }

  /**
   * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the
   *     knob to be moved.
   */
  public Touchpad(float deadzoneRadius, TouchpadStyle style) {
    if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
    if (style == null) throw new IllegalArgumentException("style cannot be null");
    this.deadzoneRadius = deadzoneRadius;
    // Ensure the non-null field is initialized on all non-exceptional paths
    this.style = style;

    knobPosition.set(getWidth() / 2f, getHeight() / 2f);

    setStyleInternal(style);
    setSize(getPrefWidth(), getPrefHeight());

    addListener(
        new InputListener() {
          public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (touched) return false;
            touched = true;
            calculatePositionAndValue(x, y, false);
            return true;
          }

          public void touchDragged(InputEvent event, float x, float y, int pointer) {
            calculatePositionAndValue(x, y, false);
          }

          public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            touched = false;
            calculatePositionAndValue(x, y, resetOnTouchUp);
          }
        });
  }

  /** Internal helper that assumes a non-null style and updates layout state. */
  private void setStyleInternal(TouchpadStyle style) {
    this.style = style;
    invalidateHierarchy();
  }

  public void setStyle(TouchpadStyle style) {
    if (style == null) throw new IllegalArgumentException("style cannot be null");
    setStyleInternal(style);
  }

  /**
   * Returns the touchpad's style. Modifying the returned style may not have an effect until {@link
   * #setStyle(TouchpadStyle)} is called.
   */
  public TouchpadStyle getStyle() {
    return style;
  }

  @Nullable
  public Actor hit(float x, float y, boolean touchable) {
    if (touchable && this.getTouchable() != Touchable.enabled) return null;
    if (!isVisible()) return null;
    return touchBounds.contains(x, y) ? this : null;
  }

  void calculatePositionAndValue(float x, float y, boolean isTouchUp) {
    float oldPositionX = knobPosition.x;
    float oldPositionY = knobPosition.y;
    float oldPercentX = knobPercent.x;
    float oldPercentY = knobPercent.y;
    float centerX = knobBounds.x;
    float centerY = knobBounds.y;
    knobPosition.set(centerX, centerY);
    knobPercent.set(0f, 0f);
    if (!isTouchUp) {
      if (!deadzoneBounds.contains(x, y)) {
        knobPercent.set((x - centerX) / knobBounds.radius, (y - centerY) / knobBounds.radius);
        float length = knobPercent.len();
        if (length > 1) knobPercent.scl(1 / length);
        if (knobBounds.contains(x, y)) {
          knobPosition.set(x, y);
        } else {
          knobPosition
              .set(knobPercent)
              .nor()
              .scl(knobBounds.radius)
              .add(knobBounds.x, knobBounds.y);
        }
      }
    }
    if (oldPercentX != knobPercent.x || oldPercentY != knobPercent.y) {
      ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
      if (fire(changeEvent)) {
        knobPercent.set(oldPercentX, oldPercentY);
        knobPosition.set(oldPositionX, oldPositionY);
      }
      Pools.free(changeEvent);
    }
  }

  public void layout() {
    // Recalc pad and deadzone bounds
    float halfWidth = getWidth() / 2;
    float halfHeight = getHeight() / 2;
    float radius = Math.min(halfWidth, halfHeight);
    touchBounds.set(halfWidth, halfHeight, radius);
    if (style.knob != null)
      radius -= Math.max(style.knob.getMinWidth(), style.knob.getMinHeight()) / 2;
    knobBounds.set(halfWidth, halfHeight, radius);
    deadzoneBounds.set(halfWidth, halfHeight, deadzoneRadius);
    // Recalc pad values and knob position
    knobPosition.set(halfWidth, halfHeight);
    knobPercent.set(0, 0);
  }

  public void draw(Batch batch, float parentAlpha) {
    validate();

    Color c = getColor();
    batch.setColor(c.r, c.g, c.b, c.a * parentAlpha);

    float x = getX();
    float y = getY();
    float w = getWidth();
    float h = getHeight();

    final Drawable bg = style.background;
    if (bg != null) bg.draw(batch, x, y, w, h);

    final Drawable knob = style.knob;
    if (knob != null) {
      x += knobPosition.x - knob.getMinWidth() / 2f;
      y += knobPosition.y - knob.getMinHeight() / 2f;
      knob.draw(batch, x, y, knob.getMinWidth(), knob.getMinHeight());
    }
  }

  public float getPrefWidth() {
    return style.background != null ? style.background.getMinWidth() : 0;
  }

  public float getPrefHeight() {
    return style.background != null ? style.background.getMinHeight() : 0;
  }

  public boolean isTouched() {
    return touched;
  }

  public boolean getResetOnTouchUp() {
    return resetOnTouchUp;
  }

  /**
   * @param reset Whether to reset the knob to the center on touch up.
   */
  public void setResetOnTouchUp(boolean reset) {
    this.resetOnTouchUp = reset;
  }

  /**
   * @param deadzoneRadius The distance in pixels from the center of the touchpad required for the
   *     knob to be moved.
   */
  public void setDeadzone(float deadzoneRadius) {
    if (deadzoneRadius < 0) throw new IllegalArgumentException("deadzoneRadius must be > 0");
    this.deadzoneRadius = deadzoneRadius;
    invalidate();
  }

  /**
   * Returns the x-position of the knob relative to the center of the widget. The positive direction
   * is right.
   */
  public float getKnobX() {
    return knobPosition.x;
  }

  /**
   * Returns the y-position of the knob relative to the center of the widget. The positive direction
   * is up.
   */
  public float getKnobY() {
    return knobPosition.y;
  }

  /**
   * Returns the x-position of the knob as a percentage from the center of the touchpad to the edge
   * of the circular movement area. The positive direction is right.
   */
  public float getKnobPercentX() {
    return knobPercent.x;
  }

  /**
   * Returns the y-position of the knob as a percentage from the center of the touchpad to the edge
   * of the circular movement area. The positive direction is up.
   */
  public float getKnobPercentY() {
    return knobPercent.y;
  }

  /** The style for a {@link Touchpad}. */
  public static class TouchpadStyle {
    /** Optional. */
    @Nullable public Drawable background;

    /** Optional. */
    @Nullable public Drawable knob;

    public TouchpadStyle() {}

    public TouchpadStyle(Drawable background, Drawable knob) {
      this.background = background;
      this.knob = knob;
    }

    public TouchpadStyle(TouchpadStyle style) {
      this.background = style.background;
      this.knob = style.knob;
    }
  }
}
