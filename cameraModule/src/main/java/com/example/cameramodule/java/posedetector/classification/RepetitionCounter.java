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

/**
 * 为给定的类统计重复次数
 */
public class RepetitionCounter {
  // These thresholds can be tuned in conjunction with the Top K values in {@link PoseClassifier}.
  // The default Top K value is 10 so the range here is [0-10].
  //默认进入最大阈值
  private static final float DEFAULT_ENTER_THRESHOLD = 6f;
  //默认退出最大阈值
  private static final float DEFAULT_EXIT_THRESHOLD = 4f;
  //动作名称 xxx_up  xxx_down
  private final String className;
  //自定义进入最大阈值
  private final float enterThreshold;
  //自定义退出最大阈值
  private final float exitThreshold;
  //达到标准的动作数
  private int numRepeats;
  //是否到达进入时的阈值
  private boolean poseEntered;

  public RepetitionCounter(String className) {
    this(className, DEFAULT_ENTER_THRESHOLD, DEFAULT_EXIT_THRESHOLD);
  }

  public RepetitionCounter(String className, float enterThreshold, float exitThreshold) {
    this.className = className;
    this.enterThreshold = enterThreshold;
    this.exitThreshold = exitThreshold;
    numRepeats = 0;
    poseEntered = false;
  }

  /**
   * 添加一个新的Pose分类结果，更新并返回给定类的重复次数Reps
   */
  public int addClassificationResult(ClassificationResult classificationResult) {
    float poseConfidence = classificationResult.getClassConfidence(className);
    /**
     * 如果poseEntered = false，判断poseConfidence是否大于enterThreshold，
     * 如果成立poseEntered = true，直接返回numRepeats
     * 即为如果判断当前姿势为某一个动作，则证明现在处于这个动作的状态
     * 比如说蹲到70°的时候已经判定为蹲下这一状态，当80°，90°的时候，也是直接返回次数而不累加
     */
    if (!poseEntered) {
      poseEntered = poseConfidence > enterThreshold;
      return numRepeats;
    }
    /**
     * 如果poseEntered = true，poseConfidence小于exitThreshold，
     * numRepeats++，poseEntered = false，返回numRepeats
     * 即之前处于这个动作的状态，然后现在这个动作状态结束，则表示退出这个动作状态，并给这个动作的重复次数+1
     * 比如蹲起之前是蹲下的状态，现在在站起过程，40°的时候即判定为蹲下状态结束，次数+1，表示一个蹲下动作完成
     */
    if (poseConfidence < exitThreshold) {
      numRepeats++;
      poseEntered = false;
    }
    /**
     * 如果poseEntered = true，poseConfidence大于等于exitThreshold，
     * 直接返回numRepeats
     * 即一直处在当前动作状态（比如蹲起是一直站着还没开始蹲下，则直接返回之前的次数）
     */
    return numRepeats;
  }

  public String getClassName() {
    return className;
  }

  public int getNumRepeats() {
    return numRepeats;
  }
}
