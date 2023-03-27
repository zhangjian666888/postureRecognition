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

import android.util.Pair;
import com.google.mlkit.vision.common.PointF3D;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static com.example.cameramodule.java.posedetector.classification.PoseEmbedding.getPoseEmbedding;
import static com.example.cameramodule.java.posedetector.classification.Utils.*;
import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Classifies {link Pose} based on given {@link PoseSample}s.
 *
 * KNN：邻近算法
 * 所谓K最近邻，就是K个最近的邻居的意思，说的是每个样本都可以用它最接近的K个邻近值来代表。近邻算法就是将数据集合中每一个记录进行分类的方法。
 * 算法流程：
 * 1.准备数据，对数据进行预处理。
 * 2.计算测试样本点（也就是待分类点）到其他每个样本点的距离。
 * 3.对每个距离进行排序，然后选择出距离最小的K个点。
 * 4.对K个点所属的类别进行比较，根据少数服从多数的原则，将测试样本点归入在K个点中占比最高的那一类。
 */
public class PoseClassifier {
  private static final String TAG = "PoseClassifier";
  private static final int MAX_DISTANCE_TOP_K = 30;
  private static final int MEAN_DISTANCE_TOP_K = 10;
  //因为Z轴相对于X和Y对精度影响小，所以所占权重默认0.2
  private static final PointF3D AXES_WEIGHTS = PointF3D.from(1, 1, 0.2f);

  private final List<PoseSample> poseSamples;
  private final int maxDistanceTopK;
  private final int meanDistanceTopK;
  private final PointF3D axesWeights;

  public PoseClassifier(List<PoseSample> poseSamples) {
    this(poseSamples, MAX_DISTANCE_TOP_K, MEAN_DISTANCE_TOP_K, AXES_WEIGHTS);
  }

  public PoseClassifier(List<PoseSample> poseSamples, int maxDistanceTopK,
      int meanDistanceTopK, PointF3D axesWeights) {
    this.poseSamples = poseSamples;
    this.maxDistanceTopK = maxDistanceTopK;
    this.meanDistanceTopK = meanDistanceTopK;
    this.axesWeights = axesWeights;
  }

  //把Pose类转换为PointF3D的List
  private static List<PointF3D> extractPoseLandmarks(Pose pose) {
    List<PointF3D> landmarks = new ArrayList<>();
    for (PoseLandmark poseLandmark : pose.getAllPoseLandmarks()) {
      landmarks.add(poseLandmark.getPosition3D());
    }
    return landmarks;
  }

  /**
   * 返回置信值的最大范围。
   * 由于我们通过计算maxDistanceTopK和meanDistanceTopK从离群值滤波中存活下来的possamples来计算置信度，所以这个范围是两个中的最小值
   */
  public int confidenceRange() {
    return min(maxDistanceTopK, meanDistanceTopK);
  }

  public ClassificationResult classify(Pose pose) {
    return classify(extractPoseLandmarks(pose));
  }

  public ClassificationResult classify(List<PointF3D> landmarks) {
    //新建一个ClassificationResult类用来储存结果，如果没有landmark被检测到，直接返回。
    ClassificationResult result = new ClassificationResult();
    if (landmarks.isEmpty()) {
      return result;
    }
    //反转x轴，获得镜像坐标，所以是水平不变的
    List<PointF3D> flippedLandmarks = new ArrayList<>(landmarks);
    multiplyAll(flippedLandmarks, PointF3D.from(-1, 1, 1));

    //然后分别对landmarks和镜像反转后的flippedLandmarks进行getPoseEmbedding操作
    List<PointF3D> embedding = getPoseEmbedding(landmarks);
    List<PointF3D> flippedEmbedding = getPoseEmbedding(flippedLandmarks);

    /**
     * 接下来就是具体的基于KNN的分类阶段
     * 分类分为两个阶段:
     * 1.首先，我们根据最大距离选取top-K样本。它允许移除几乎与给定姿势相同的样品，但可能有几个关节向另一个方向弯曲。
     * 2.然后通过均值距离选取前k个样本。在去除异常值后，我们选取平均值最接近的样本。
     */
    //定义一个优先队列maxDistances，通过对比Pair的float由大到小来排序
    PriorityQueue<Pair<PoseSample, Float>> maxDistances = new PriorityQueue<>(
        maxDistanceTopK, (o1, o2) -> -Float.compare(o1.second, o2.second));

    /**
     * 以最小的距离检索前K个possamples以去除异常值。
     * 遍历embedding和flippedEmbedding的关键点坐标，
     * 与sampleEmbedding对应的坐标做差，
     * 然后再乘以axesWeights（各维度的权重，默认为[1, 1, 0.2f]），
     * 对于其中最大的差值取绝对值，
     * 以不断更新originalMax和flippedMax，
     * 即所有关键点中与samplePose在某一维度的最大距离。
     *
     * new一个新的Pair<poseSample, originalMax和flippedMax的最小值，即是面对镜头还是背对镜头>。
     * 将Pair加进之前定义的优先队列。
     * 对于每一个poseSample都有一个与之对应的Pair，通过比较originalMax或flippedMax，得到 top-K samples by MAX distance。
     */
    for (PoseSample poseSample : poseSamples) {
      List<PointF3D> sampleEmbedding = poseSample.getEmbedding();
      float originalMax = 0;
      float flippedMax = 0;
      for (int i = 0; i < embedding.size(); i++) {
        originalMax =
            max(
                originalMax,
                maxAbs(multiply(subtract(embedding.get(i), sampleEmbedding.get(i)), axesWeights)));
        flippedMax =
            max(
                flippedMax,
                maxAbs(
                    multiply(
                        subtract(flippedEmbedding.get(i), sampleEmbedding.get(i)), axesWeights)));
      }
      // Set the max distance as min of original and flipped max distance.
      maxDistances.add(new Pair<>(poseSample, min(originalMax, flippedMax)));
      // We only want to retain top n so pop the highest distance.
      if (maxDistances.size() > maxDistanceTopK) {
        maxDistances.poll();
      }
    }

    /**
     * 第二阶段:
     */
    //定义一个优先队列meanDistances，通过对比Pair的float由大到小来排序
    PriorityQueue<Pair<PoseSample, Float>> meanDistances = new PriorityQueue<>(
        meanDistanceTopK, (o1, o2) -> -Float.compare(o1.second, o2.second));

    /**
     * 通过上一步，我们的到了TopK个maxDistance，对于每一个maxDistances中的Pair<PoseSample, Float>：
     * 遍历embedding和flippedEmbedding的关键点坐标，与sampleEmbedding对应的坐标做差，
     * 然后再乘以axesWeights（各维度的权重，默认为[1, 1, 0.2f]）并取绝对值，
     * 然后求三个绝对值的和，不断累加originalSum和flippedSum，
     * 即所有关键点与samplePose对应点的三个维度上的距离之和。
     * 之后取originalSum和flippedSum中较小的值，除以（关键点个数*2）得到meanDistance
     * new一个新的Pair<poseSample, meanDistance>。将Pair加进之前定义的优先队列。
     * 对于每一个poseSample都有一个与之对应的Pair，通过比较meanDistance，得到 top-K samples by MEAN distance。
     */
    for (Pair<PoseSample, Float> sampleDistances : maxDistances) {
      PoseSample poseSample = sampleDistances.first;
      List<PointF3D> sampleEmbedding = poseSample.getEmbedding();

      float originalSum = 0;
      float flippedSum = 0;
      for (int i = 0; i < embedding.size(); i++) {
        originalSum += sumAbs(multiply(
            subtract(embedding.get(i), sampleEmbedding.get(i)), axesWeights));
        flippedSum += sumAbs(
            multiply(subtract(flippedEmbedding.get(i), sampleEmbedding.get(i)), axesWeights));
      }
      // Set the mean distance as min of original and flipped mean distances.
      float meanDistance = min(originalSum, flippedSum) / (embedding.size() * 2);
      meanDistances.add(new Pair<>(poseSample, meanDistance));
      // We only want to retain top k so pop the highest mean distance.
      if (meanDistances.size() > meanDistanceTopK) {
        meanDistances.poll();
      }
    }

    /**
     * 对于最终的top-K samples by MEAN distance，获取Pair中PoseSample的类名，并将其添加到最后的结果中并返回。
     */
    for (Pair<PoseSample, Float> sampleDistances : meanDistances) {
      String className = sampleDistances.first.getClassName();
      result.incrementClassConfidence(className);
    }

    return result;
  }
}
