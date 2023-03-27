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

import android.content.Context;
import android.graphics.fonts.Font;
import android.graphics.fonts.FontStyle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.WorkerThread;
import com.example.cameramodule.java.PlayMusic;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.common.base.Preconditions;
import com.google.mlkit.vision.pose.Pose;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 接受Pose流来进行分类和Rep计数
 */
public class PoseClassifierProcessor {
  private static final String TAG = "PoseClassifierProcessor";
  //存储pose示例数据集文件
  private static final String POSE_SD_FILE = "pose/pose_sd.csv"; //深蹲
  private static final String POSE_DJZL_FILE = "pose/pose_djzl.csv"; //单脚站立
  private static final String POSE_QZTT_FILE = "pose/pose_qztt.csv"; //前直抬腿
  private static final String POSE_ZLWQX_FILE = "pose/pose_zlwqx.csv"; //站立位屈膝
  private static final String Lift_YOUR_LEGS_STRAIGHT_BACK_SAMPLES_FILE = "pose/lift_your_legs_straight_back_csvs.csv";
  private static final String PUSHUPS_CLASS = "pushups_down";
  private static final String SQUATS_CLASS = "down";
  //检测的类别
  private static final String[] POSE_CLASSES = {
    PUSHUPS_CLASS, SQUATS_CLASS
  };
  //是否是流模式
  private final boolean isStreamMode;

  private EMASmoothing emaSmoothing;
  private List<RepetitionCounter> repCounters;
  private PoseClassifier poseClassifier;
  private String lastRepResult;
  public static Integer completeNum = 0;
  public static Map<String, List<Float>> scoreMap = new HashMap<>();
  private Map<String, List<Float>> tmpMap = new HashMap<>();
  private String nowAction = "up";

  /**
   * 实例化
   * 必须传入当前的上下文Context和是否为流模式
   * 先调用静态方法Preconditions.checkState()帮助方法或构造函数检查其是否被正确调用
   * 判断当前模式是否需要初始化emaSmoothing，repCounters，lastRepResult
   * 然后进入loadPoseSamples(Context context)
   * @param context
   * @param isStreamMode
   * @param modelType
   */
  @WorkerThread
  public PoseClassifierProcessor(Context context, boolean isStreamMode, String modelType) {
    Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper());
    this.isStreamMode = isStreamMode;
    if (isStreamMode) {
      emaSmoothing = new EMASmoothing();
      repCounters = new ArrayList<>();
      lastRepResult = "";
    }
    loadPoseSamples(context, modelType);
  }

  /**
   * 读取pose从示例的csv文件中的内容并加入poseSamples中，初始化PoseClassifier。
   * @param context
   * @param modelType
   */
  private void loadPoseSamples(Context context, String modelType) {
    List<PoseSample> poseSamples = new ArrayList<>();
    try {
      //读取csv文件
      BufferedReader reader = null;
      switch (modelType){
        case "POSE_DETECTION":
          reader = new BufferedReader(new InputStreamReader(context.getAssets().open(POSE_SD_FILE)));
          break;
        case "POSE_DJZL":
          reader = new BufferedReader(new InputStreamReader(context.getAssets().open(POSE_DJZL_FILE)));
          break;
        case "POSE_QZTT":
          reader = new BufferedReader(new InputStreamReader(context.getAssets().open(POSE_QZTT_FILE)));
          break;
        case "POSE_ZLWQX":
          reader = new BufferedReader(new InputStreamReader(context.getAssets().open(POSE_ZLWQX_FILE)));
          break;
        case "Lift_YOUR_LEGS_STRAIGHT_BACK":
          reader = new BufferedReader(new InputStreamReader(context.getAssets().open(Lift_YOUR_LEGS_STRAIGHT_BACK_SAMPLES_FILE)));
          break;
        default:
          throw new IllegalStateException("Invalid model name");
      }
      String csvLine = reader.readLine();
      while (csvLine != null) {
        // If line is not a valid {@link PoseSample}, we'll get null and skip adding to the list.
        PoseSample poseSample = PoseSample.getPoseSample(csvLine, ",");
        if (poseSample != null) {
          poseSamples.add(poseSample);
        }
        csvLine = reader.readLine();
      }
    } catch (IOException e) {
      Log.e(TAG, "Error when loading pose samples.\n" + e);
    }
    poseClassifier = new PoseClassifier(poseSamples);
    //如果是流模式，repCounters加入这个类的RepetitionCounter
    if (isStreamMode) {
      for (String className : POSE_CLASSES) {
        repCounters.add(new RepetitionCounter(className));
      }
    }
  }

  /**
   * input：输入的Pose类为pose detection的检测结果，为人体关键点的坐标
   * output：输出是一个list存放的是pose分类结果
   */
  @WorkerThread
  public List<String> getPoseResult(Context context, Pose pose, Integer num) {
    Preconditions.checkState(Looper.myLooper() != Looper.getMainLooper());
    List<String> result = new ArrayList<>();
    //先对输入的pose单独判断其类别，得到分类结果classification。
    ClassificationResult classification = poseClassifier.classify(pose);
    //判断是否为流模式
    if (isStreamMode) {
      //将pose的分类结果输入EMA平滑处理，得到一个新的分类结果
      classification = emaSmoothing.getSmoothedResult(classification);
      //如果没有pose被检测到则提前返回，不更新repCounter
      if (pose.getAllPoseLandmarks().isEmpty()) {
        if("".equals(lastRepResult)){
          lastRepResult = String.format(
                  Locale.US, "%d /%d reps", 0, num);
        }
        result.add(lastRepResult);
        //Log.i(TAG, "PoseClassifierProcessor->getPoseResult->result：" + result.toString());
        return result;
      }
      //遍历每一个类的repCounter
      for (RepetitionCounter repCounter : repCounters) {
        int repsBefore = repCounter.getNumRepeats();
        int repsAfter = repCounter.addClassificationResult(classification);
        //repsAfter > repsBefore即为poseEntered = true，poseConfidence小于exitThreshold
        //停止了这个动作
        if (repsAfter > repsBefore) {
          //当计数器更新时，播放声音。
          //ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
          //tg.startTone(ToneGenerator.TONE_PROP_BEEP);
          //className: repsAfter reps
          double total = Double.parseDouble(num + "");
          if(repsAfter / total >= 0.5 && repsAfter / total < 0.6){
            PlayMusic.playUp();
          }
          if(repsAfter == num){
            PlayMusic.playComplete();
          }
          if(repsAfter > num){
            completeNum = repsAfter;
            lastRepResult = String.format(
                    Locale.US, "%d /%d reps", num, num);
          }else {
            completeNum = repsAfter;

            lastRepResult = String.format(
                    Locale.US, "%d /%d reps", repsAfter, num);
          }
          break;
        }
      }
      result.add(lastRepResult);
    }

    //如果pose被找到，将现在这个frame的最大置信度的类加入到result中
    if (!pose.getAllPoseLandmarks().isEmpty()) {
      //className : 0.XX confidence
      //置信度 = 最大置信度 / 置信值的最大范围
      String maxConfidenceClass = classification.getMaxConfidenceClass();
      float confidence = classification.getClassConfidence(maxConfidenceClass) / poseClassifier.confidenceRange();
      if(nowAction.equals(maxConfidenceClass)){
        List<Float> floats = tmpMap.get(maxConfidenceClass);
        if(CollectionUtils.isEmpty(floats)){
          List<Float> floats1 = new ArrayList<>();
          floats1.add(confidence);
          tmpMap.put(maxConfidenceClass, floats1);
        }else {
          floats.add(confidence);
        }
      }else {
        List<Float> floats = tmpMap.get(nowAction);
        float sum = 0f;
        float avg = 0f;
        if(!CollectionUtils.isEmpty(floats)){
          for (Float aFloat : floats) {
            sum = sum + aFloat;
          }
          avg = sum / floats.size();
        }
        List<Float> floats1 = scoreMap.get(nowAction);
        if(CollectionUtils.isEmpty(floats1)){
          List<Float> float2 = new ArrayList<>();
          float2.add(avg);
          scoreMap.put(nowAction, float2);
        }else {
          floats1.add(avg);
        }
        nowAction = maxConfidenceClass;
        tmpMap.clear();
      }
      String maxConfidenceClassResult = String.format(
          Locale.US,
          "%s : %.2f confidence",
          maxConfidenceClass, confidence);
      result.add(maxConfidenceClassResult);
    }
    //Log.i(TAG, "PoseClassifierProcessor->getPoseResult->result：" + result.toString());
    return result;
  }

}
