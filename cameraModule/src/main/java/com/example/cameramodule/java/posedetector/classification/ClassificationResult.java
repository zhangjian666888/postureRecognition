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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.max;

/**
 * 表示Posecasifier输出的Pose分类结果。
 */
public class ClassificationResult {
  /**
   * key是className
   * value是classConfidences这个动作的置信度，含义是在top K nearest neighbors（前K个最近邻）中出现的次数，范围是[0, K]，类型为float（因为EMA平滑处理可能会导致出现小数），代表了这个类中一个姿势出现的置信度。
   */
  private final Map<String, Float> classConfidences;

  public ClassificationResult() {
    classConfidences = new HashMap<>();
  }

  public Set<String> getAllClasses() {
    return classConfidences.keySet();
  }

  public float getClassConfidence(String className) {
    return classConfidences.containsKey(className) ? classConfidences.get(className) : 0;
  }
  //根据classConfidences排序，返回最大值，即概率最高的pose的类名
  public String getMaxConfidenceClass() {
    return max(
        classConfidences.entrySet(),
        (entry1, entry2) -> (int) (entry1.getValue() - entry2.getValue()))
        .getKey();
  }
  //如果这个类名已经存在，则value+1，如果不存在，则新加一个key，value设置为1
  public void incrementClassConfidence(String className) {
    classConfidences.put(className,
        classConfidences.containsKey(className) ? classConfidences.get(className) + 1 : 1);
  }

  public void putClassConfidence(String className, float confidence) {
    classConfidences.put(className, confidence);
  }
}
