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

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 在给定姿势分类结果的流的窗口上运行EMA平滑处理(做EMA平滑处理)
 */
public class EMASmoothing {
  //窗口默认大小
  private static final int DEFAULT_WINDOW_SIZE = 10;
  //新旧结果所占的权重
  private static final float DEFAULT_ALPHA = 0.2f;
  private final int windowSize;
  private final float alpha;
  //是一个LinkedBlockingDeque<>双向阻塞队列
  private final Deque<ClassificationResult> window;

  public EMASmoothing() {
    this(DEFAULT_WINDOW_SIZE, DEFAULT_ALPHA);
  }

  public EMASmoothing(int windowSize, float alpha) {
    this.windowSize = windowSize;
    this.alpha = alpha;
    this.window = new LinkedBlockingDeque<>(windowSize);
  }

  public ClassificationResult getSmoothedResult(ClassificationResult classificationResult) {
    //如果window是满的，则去除最后一个即最老的分类结果
    if (window.size() == windowSize) {
      window.pollLast();
    }
    //在window的开头插入新的分类结果
    window.addFirst(classificationResult);
    //新建一个Set,将window中所有分类结果中出现的所有类插入进去
    Set<String> allClasses = new HashSet<>();
    for (ClassificationResult result : window) {
      allClasses.addAll(result.getAllClasses());
    }
    ClassificationResult smoothedResult = new ClassificationResult();
    //遍历Set，对于每一个className计算它的topSum和bottomSum
    for (String className : allClasses) {
      float factor = 1;
      float topSum = 0;
      float bottomSum = 0;
      //这个循环既考虑了整个窗口所有的结果，而且越新的结果占比、权重越大
      for (ClassificationResult result : window) {
        float value = result.getClassConfidence(className);
        topSum += factor * value;
        bottomSum += factor;
        //默认0.2，factor每次递减为之前的0.8
        factor = (float) (factor * (1.0 - alpha));
      }
      //className的置信度设置为topSum / bottomSum
      smoothedResult.putClassConfidence(className, topSum / bottomSum);
    }

    return smoothedResult;
  }
}
