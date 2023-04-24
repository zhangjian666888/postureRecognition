/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.cameramodule.java.posedetector.classification;

import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;

import static com.example.cameramodule.java.posedetector.classification.Utils.*;

/**
 * 为给定的姿态地标列表生成嵌入。
 */
public class PoseEmbedding {
  //适用于躯干，以获得最小的身体尺寸的乘数
  private static final float TORSO_MULTIPLIER = 2.5f;

  public static List<PointF3D> getPoseEmbedding(List<PointF3D> landmarks) {
    List<PointF3D> normalizedLandmarks = normalize(landmarks);
    return getEmbedding(normalizedLandmarks);
  }

  private static List<PointF3D> normalize(List<PointF3D> landmarks) {
    List<PointF3D> normalizedLandmarks = new ArrayList<>(landmarks);
    //求出人体的center，具体就是left_hip和right_hip的中点hipsCenter
    PointF3D center = average(
        landmarks.get(PoseLandmark.LEFT_HIP), landmarks.get(PoseLandmark.RIGHT_HIP));
    //然后人体各个坐标减去这一点的坐标，等于是新建立了一个以hipsCenter为原点的坐标系来定位各个关键点
    subtractAll(center, normalizedLandmarks);
    //先用方法getPoseSize(List landmarks)求出maxDistance（x,y二维空间上的），再用上面求到的各关键点坐标除以maxDistance以实现归一化
    multiplyAll(normalizedLandmarks, 1 / getPoseSize(normalizedLandmarks));
    // Multiplication by 100 is not required, but makes it easier to debug.
    multiplyAll(normalizedLandmarks, 100);
    return normalizedLandmarks;
  }

  /**
   * 先求出hipsCenter和shouldersCenter，两者坐标相减，求这个点的到原点的距离torsoSize，
   * 这个距离再乘以TORSO_MULTIPLIER作为maxDistance，
   * “基于实验，torsoSize * TORSO_MULTIPLIER是我们想要的的基准，但实际尺寸可能是更大的，取决于一个给定的姿势的肢体的延伸等，所以我们计算。”
   * 为了避免实际尺寸更大，再求身体的所有关键点与hipsCenter的距离并与maxDistance比较
   * @param landmarks
   * @return
   */
  private static float getPoseSize(List<PointF3D> landmarks) {
    // Note: This approach uses only 2D landmarks to compute pose size as using Z wasn't helpful
    // in our experimentation but you're welcome to tweak.
    PointF3D hipsCenter = average(
        landmarks.get(PoseLandmark.LEFT_HIP), landmarks.get(PoseLandmark.RIGHT_HIP));

    PointF3D shouldersCenter = average(
        landmarks.get(PoseLandmark.LEFT_SHOULDER),
        landmarks.get(PoseLandmark.RIGHT_SHOULDER));

    float torsoSize = l2Norm2D(subtract(hipsCenter, shouldersCenter));

    float maxDistance = torsoSize * TORSO_MULTIPLIER;
    // torsoSize * TORSO_MULTIPLIER is the floor we want based on experimentation but actual size
    // can be bigger for a given pose depending on extension of limbs etc so we calculate that.
    for (PointF3D landmark : landmarks) {
      float distance = l2Norm2D(subtract(hipsCenter, landmark));
      if (distance > maxDistance) {
        maxDistance = distance;
      }
    }
    return maxDistance;
  }

  /**
   * 使用几个成对的3D距离来形成姿态embeddin
   * hipsCenter 臀部中心
   * shouldersCenter 肩部中心
   * shoulders 肩膀
   * elbows 手肘
   * elbows 手肘
   * wrists 手腕
   * hips 臀部
   * knees 膝盖
   * ankles 脚踝
   * wrists 手腕
   * 得到的这些距离在三维空间表示记录下来
   * @param lm
   * @return
   */
  private static List<PointF3D> getEmbedding(List<PointF3D> lm) {
    List<PointF3D> embedding = new ArrayList<>();

    // We use several pairwise 3D distances to form pose embedding. These were selected
    // based on experimentation for best results with our default pose classes as captued in the
    // pose samples csv. Feel free to play with this and add or remove for your use-cases.

    // We group our distances by number of joints between the pairs.
    // One joint.
    embedding.add(subtract(
        average(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.RIGHT_HIP)),
        average(lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.RIGHT_SHOULDER))
    ));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.LEFT_ELBOW)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_SHOULDER), lm.get(PoseLandmark.RIGHT_ELBOW)));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_ELBOW), lm.get(PoseLandmark.LEFT_WRIST)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_ELBOW), lm.get(PoseLandmark.RIGHT_WRIST)));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_KNEE)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_KNEE)));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_KNEE), lm.get(PoseLandmark.LEFT_ANKLE)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_KNEE), lm.get(PoseLandmark.RIGHT_ANKLE)));

    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HEEL), lm.get(PoseLandmark.RIGHT_FOOT_INDEX)));
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_HEEL), lm.get(PoseLandmark.LEFT_FOOT_INDEX)));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_ANKLE), lm.get(PoseLandmark.LEFT_HEEL)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_ANKLE), lm.get(PoseLandmark.RIGHT_HEEL)));

    // Two joints.
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.LEFT_WRIST)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_SHOULDER), lm.get(PoseLandmark.RIGHT_WRIST)));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_ANKLE)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_ANKLE)));

    // Four joints.
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_WRIST)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_WRIST)));

    // Five joints.
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_SHOULDER), lm.get(PoseLandmark.LEFT_ANKLE)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_SHOULDER), lm.get(PoseLandmark.RIGHT_ANKLE)));

    embedding.add(subtract(lm.get(PoseLandmark.LEFT_HIP), lm.get(PoseLandmark.LEFT_WRIST)));
    embedding.add(subtract(lm.get(PoseLandmark.RIGHT_HIP), lm.get(PoseLandmark.RIGHT_WRIST)));

    // Cross body.
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_ELBOW), lm.get(PoseLandmark.RIGHT_ELBOW)));
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_KNEE), lm.get(PoseLandmark.RIGHT_KNEE)));
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_WRIST), lm.get(PoseLandmark.RIGHT_WRIST)));
    embedding.add(subtract(lm.get(PoseLandmark.LEFT_ANKLE), lm.get(PoseLandmark.RIGHT_ANKLE)));

    return embedding;
  }

  private PoseEmbedding() {}
}
