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

package com.badlogic.gdx.graphics.g3d.particles.influencers;

import com.badlogic.gdx.graphics.g3d.particles.ParallelArray.FloatChannel;
import com.badlogic.gdx.graphics.g3d.particles.ParticleChannels;
import com.badlogic.gdx.graphics.g3d.particles.ParticleController;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.uber.nullaway.annotations.Initializer;
import edu.ucr.cs.riple.annotator.util.Nullability;
import java.util.Arrays;
import javax.annotation.Nullable;

/**
 * It's an {@link Influencer} which controls the particles dynamics (movement, rotations).
 *
 * @author Inferno
 */
public class DynamicsInfluencer extends Influencer {
  public Array<DynamicsModifier> velocities;
  @Nullable
  private FloatChannel accellerationChannel,
      positionChannel,
      previousPositionChannel,
      rotationChannel,
      angularVelocityChannel;
  boolean hasAcceleration, has2dAngularVelocity, has3dAngularVelocity;

  public DynamicsInfluencer() {
    this.velocities = new Array<DynamicsModifier>(true, 3, DynamicsModifier.class);
  }

  public DynamicsInfluencer(DynamicsModifier... velocities) {
    this.velocities = new Array<DynamicsModifier>(true, velocities.length, DynamicsModifier.class);
    for (DynamicsModifier value : velocities) {
      this.velocities.add((DynamicsModifier) value.copy());
    }
  }

  public DynamicsInfluencer(DynamicsInfluencer velocityInfluencer) {
    this((DynamicsModifier[]) velocityInfluencer.velocities.toArray(DynamicsModifier.class));
  }

  @Initializer
  @Override
  public void allocateChannels() {
    for (int k = 0; k < velocities.size; ++k) {
      velocities.items[k].allocateChannels();
    }

    // Hack, shouldn't be done but after all the modifiers allocated their channels
    // it's possible to check if we need to allocate previous position channel
    accellerationChannel = controller.particles.getChannel(ParticleChannels.Acceleration);
    hasAcceleration = accellerationChannel != null;
    if (hasAcceleration) {
      positionChannel = controller.particles.addChannel(ParticleChannels.Position);
      previousPositionChannel = controller.particles.addChannel(ParticleChannels.PreviousPosition);
    }

    // Angular velocity check
    angularVelocityChannel = controller.particles.getChannel(ParticleChannels.AngularVelocity2D);
    has2dAngularVelocity = angularVelocityChannel != null;
    if (has2dAngularVelocity) {
      rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation2D);
      has3dAngularVelocity = false;
    } else {
      angularVelocityChannel = controller.particles.getChannel(ParticleChannels.AngularVelocity3D);
      has3dAngularVelocity = angularVelocityChannel != null;
      if (has3dAngularVelocity)
        rotationChannel = controller.particles.addChannel(ParticleChannels.Rotation3D);
    }
  }

  @Override
  public void set(ParticleController particleController) {
    super.set(particleController);
    for (int k = 0; k < velocities.size; ++k) {
      velocities.items[k].set(particleController);
    }
  }

  @Override
  public void init() {
    for (int k = 0; k < velocities.size; ++k) {
      velocities.items[k].init();
    }
  }

  public void activateParticles(int startIndex, int count) {
    if (hasAcceleration && previousPositionChannel != null && positionChannel != null) {
      // Previous position is the current position
      // Attention, this requires that some other influencer setting the position channel must
      // execute before this influencer.
      for (int i = startIndex * Nullability.castToNonnull(positionChannel).strideSize,
              c = i + count * Nullability.castToNonnull(positionChannel).strideSize;
          i < c;
          i += Nullability.castToNonnull(positionChannel).strideSize) {
        previousPositionChannel.data[i + ParticleChannels.XOffset] =
            Nullability.castToNonnull(positionChannel).data[i + ParticleChannels.XOffset];
        previousPositionChannel.data[i + ParticleChannels.YOffset] =
            Nullability.castToNonnull(positionChannel).data[i + ParticleChannels.YOffset];
        previousPositionChannel.data[i + ParticleChannels.ZOffset] =
            Nullability.castToNonnull(positionChannel).data[i + ParticleChannels.ZOffset];
        /*
         * //Euler intialization previousPositionChannel.data[i+ParticleChannels.XOffset] =
         * previousPositionChannel.data[i+ParticleChannels.YOffset] = previousPositionChannel.data[i+ParticleChannels.ZOffset]
         * = 0;
         */
      }
    }

    if (rotationChannel != null && has2dAngularVelocity) {
      // Rotation back to 0
      for (int i = startIndex * Nullability.castToNonnull(rotationChannel).strideSize,
              c = i + count * Nullability.castToNonnull(rotationChannel).strideSize;
          i < c;
          i += Nullability.castToNonnull(rotationChannel).strideSize) {
        Nullability.castToNonnull(rotationChannel).data[i + ParticleChannels.CosineOffset] = 1;
        Nullability.castToNonnull(rotationChannel).data[i + ParticleChannels.SineOffset] = 0;
      }
    } else if (rotationChannel != null && has3dAngularVelocity) {
      // Rotation back to 0
      for (int i = startIndex * Nullability.castToNonnull(rotationChannel).strideSize,
              c = i + count * Nullability.castToNonnull(rotationChannel).strideSize;
          i < c;
          i += Nullability.castToNonnull(rotationChannel).strideSize) {
        Nullability.castToNonnull(rotationChannel).data[i + ParticleChannels.XOffset] = 0;
        Nullability.castToNonnull(rotationChannel).data[i + ParticleChannels.YOffset] = 0;
        Nullability.castToNonnull(rotationChannel).data[i + ParticleChannels.ZOffset] = 0;
        Nullability.castToNonnull(rotationChannel).data[i + ParticleChannels.WOffset] = 1;
      }
    }

    for (int k = 0; k < velocities.size; ++k) {
      velocities.items[k].activateParticles(startIndex, count);
    }
  }

  public void update() {
    if ((!hasAcceleration || accellerationChannel == null)
        && !has2dAngularVelocity
        && !has3dAngularVelocity) return;

    if (hasAcceleration && accellerationChannel != null)
      Arrays.fill(
          Nullability.castToNonnull(accellerationChannel).data,
          0,
          controller.particles.size * Nullability.castToNonnull(accellerationChannel).strideSize,
          0);
    if ((has2dAngularVelocity || has3dAngularVelocity) && angularVelocityChannel != null)
      Arrays.fill(
          Nullability.castToNonnull(angularVelocityChannel).data,
          0,
          controller.particles.size * Nullability.castToNonnull(angularVelocityChannel).strideSize,
          0);

    for (int k = 0; k < velocities.size; ++k) {
      velocities.items[k].update();
    }

    if (hasAcceleration
        && accellerationChannel != null
        && positionChannel != null
        && previousPositionChannel != null) {
      /*
       * //Euler Integration for(int i=0, offset = 0; i < controller.particles.size; ++i, offset +=positionChannel.strideSize){
       * previousPositionChannel.data[offset + ParticleChannels.XOffset] += accellerationChannel.data[offset +
       * ParticleChannels.XOffset]*controller.deltaTime; previousPositionChannel.data[offset + ParticleChannels.YOffset] +=
       * accellerationChannel.data[offset + ParticleChannels.YOffset]*controller.deltaTime; previousPositionChannel.data[offset
       * + ParticleChannels.ZOffset] += accellerationChannel.data[offset + ParticleChannels.ZOffset]*controller.deltaTime;
       *
       * positionChannel.data[offset + ParticleChannels.XOffset] += previousPositionChannel.data[offset +
       * ParticleChannels.XOffset]*controller.deltaTime; positionChannel.data[offset + ParticleChannels.YOffset] +=
       * previousPositionChannel.data[offset + ParticleChannels.YOffset]*controller.deltaTime; positionChannel.data[offset +
       * ParticleChannels.ZOffset] += previousPositionChannel.data[offset + ParticleChannels.ZOffset]*controller.deltaTime; }
       */
      for (int i = 0, offset = 0;
          i < controller.particles.size;
          ++i, offset += Nullability.castToNonnull(positionChannel).strideSize) {
        float
            x = Nullability.castToNonnull(positionChannel).data[offset + ParticleChannels.XOffset],
            y = Nullability.castToNonnull(positionChannel).data[offset + ParticleChannels.YOffset],
            z = Nullability.castToNonnull(positionChannel).data[offset + ParticleChannels.ZOffset];
        Nullability.castToNonnull(positionChannel).data[offset + ParticleChannels.XOffset] =
            2 * x
                - Nullability.castToNonnull(previousPositionChannel)
                    .data[offset + ParticleChannels.XOffset]
                + Nullability.castToNonnull(accellerationChannel)
                        .data[offset + ParticleChannels.XOffset]
                    * controller.deltaTimeSqr;
        Nullability.castToNonnull(positionChannel).data[offset + ParticleChannels.YOffset] =
            2 * y
                - Nullability.castToNonnull(previousPositionChannel)
                    .data[offset + ParticleChannels.YOffset]
                + Nullability.castToNonnull(accellerationChannel)
                        .data[offset + ParticleChannels.YOffset]
                    * controller.deltaTimeSqr;
        Nullability.castToNonnull(positionChannel).data[offset + ParticleChannels.ZOffset] =
            2 * z
                - Nullability.castToNonnull(previousPositionChannel)
                    .data[offset + ParticleChannels.ZOffset]
                + Nullability.castToNonnull(accellerationChannel)
                        .data[offset + ParticleChannels.ZOffset]
                    * controller.deltaTimeSqr;
        Nullability.castToNonnull(previousPositionChannel).data[offset + ParticleChannels.XOffset] =
            x;
        Nullability.castToNonnull(previousPositionChannel).data[offset + ParticleChannels.YOffset] =
            y;
        Nullability.castToNonnull(previousPositionChannel).data[offset + ParticleChannels.ZOffset] =
            z;
      }
    }

    if (has2dAngularVelocity && rotationChannel != null && angularVelocityChannel != null) {
      for (int i = 0, offset = 0;
          i < controller.particles.size;
          ++i, offset += Nullability.castToNonnull(rotationChannel).strideSize) {
        float rotation =
            Nullability.castToNonnull(angularVelocityChannel).data[i] * controller.deltaTime;
        if (rotation != 0) {
          float cosBeta = MathUtils.cosDeg(rotation), sinBeta = MathUtils.sinDeg(rotation);
          float currentCosine =
              Nullability.castToNonnull(rotationChannel)
                  .data[offset + ParticleChannels.CosineOffset];
          float currentSine =
              Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.SineOffset];
          float newCosine = currentCosine * cosBeta - currentSine * sinBeta,
              newSine = currentSine * cosBeta + currentCosine * sinBeta;
          Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.CosineOffset] =
              newCosine;
          Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.SineOffset] =
              newSine;
        }
      }
    } else if (has3dAngularVelocity && rotationChannel != null && angularVelocityChannel != null) {
      for (int i = 0, offset = 0, angularOffset = 0;
          i < controller.particles.size;
          ++i, offset += Nullability.castToNonnull(rotationChannel).strideSize,
              angularOffset += Nullability.castToNonnull(angularVelocityChannel).strideSize) {

        float
            wx =
                Nullability.castToNonnull(angularVelocityChannel)
                    .data[angularOffset + ParticleChannels.XOffset],
            wy =
                Nullability.castToNonnull(angularVelocityChannel)
                    .data[angularOffset + ParticleChannels.YOffset],
            wz =
                Nullability.castToNonnull(angularVelocityChannel)
                    .data[angularOffset + ParticleChannels.ZOffset],
            qx = Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.XOffset],
            qy = Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.YOffset],
            qz = Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.ZOffset],
            qw = Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.WOffset];
        TMP_Q
            .set(wx, wy, wz, 0)
            .mul(qx, qy, qz, qw)
            .mul(0.5f * controller.deltaTime)
            .add(qx, qy, qz, qw)
            .nor();
        Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.XOffset] =
            TMP_Q.x;
        Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.YOffset] =
            TMP_Q.y;
        Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.ZOffset] =
            TMP_Q.z;
        Nullability.castToNonnull(rotationChannel).data[offset + ParticleChannels.WOffset] =
            TMP_Q.w;
      }
    }
  }

  @Override
  public DynamicsInfluencer copy() {
    return new DynamicsInfluencer(this);
  }

  @Override
  public void write(Json json) {
    json.writeValue("velocities", velocities, Array.class, DynamicsModifier.class);
  }

  @Override
  public void read(Json json, JsonValue jsonData) {
    velocities.addAll(json.readValue("velocities", Array.class, DynamicsModifier.class, jsonData));
  }
}
