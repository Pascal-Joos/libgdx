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

package com.badlogic.gdx.graphics.g3d.model.data;

import com.badlogic.gdx.math.Vector2;
import javax.annotation.Nullable;

public class ModelTexture {
  public static final int USAGE_UNKNOWN = 0;
  public static final int USAGE_NONE = 1;
  public static final int USAGE_DIFFUSE = 2;
  public static final int USAGE_EMISSIVE = 3;
  public static final int USAGE_AMBIENT = 4;
  public static final int USAGE_SPECULAR = 5;
  public static final int USAGE_SHININESS = 6;
  public static final int USAGE_NORMAL = 7;
  public static final int USAGE_BUMP = 8;
  public static final int USAGE_TRANSPARENCY = 9;
  public static final int USAGE_REFLECTION = 10;

  @Nullable public String id;
  public String fileName;
  @Nullable public Vector2 uvTranslation;
  @Nullable public Vector2 uvScaling;
  public int usage;
}
