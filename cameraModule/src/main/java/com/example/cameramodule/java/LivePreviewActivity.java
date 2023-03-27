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

package com.example.cameramodule.java;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import com.alibaba.fastjson.JSONObject;
import com.example.cameramodule.*;
import com.example.cameramodule.java.posedetector.PoseDetectorProcessor;
import com.example.cameramodule.java.posedetector.classification.PoseClassifierProcessor;
import com.example.cameramodule.preference.PreferenceUtils;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Live preview demo for ML Kit APIs. */
//ML Kit api的实时预览演示
@KeepName
public final class LivePreviewActivity extends Activity
        implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener {

  private static final String POSE_DETECTION = "深蹲";
  private static final String Lift_YOUR_LEGS_STRAIGHT_BACK = "向后直退抬高";
  private static final String STAND_ON_ONE_LEG = "单脚站立";
  private static final String STRAIGHT_FORWARD_LEG_LIFT = "前直抬腿";
  private static final String STANDING_KNEE_BEND = "站立位屈膝";
  private static final String TAG = "LivePreviewActivity";
  private static final int PERMISSION_REQUESTS = 1;

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  //默认模型
  private String selectedModel = POSE_DETECTION;
  private Integer num = 10;
  private Button backBtn;
  private VideoView videoView;
  MediaController mediaController;
  private String videoUrl = "";
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent1 = getIntent();
    if(intent1 != null){
      selectedModel = intent1.getStringExtra("actionName");
      if(intent1.getStringExtra("num") != null && !"".equals(intent1.getStringExtra("num"))){
        num = Integer.parseInt(intent1.getStringExtra("num"));
      }
      videoUrl = intent1.getStringExtra("videoUrl");
      //videoUrl = "http://43.143.181.73/app/1ac086e2-4759-45ec-84c4-1c68d1291a7016672734210002022-11-01.mp4";
    }

    setContentView(R.layout.activity_vision_live_preview);

    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      //Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      //Log.d(TAG, "graphicOverlay is null");
    }

    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);

    if (allPermissionsGranted()) {
      //默认选择第一个检测类型
      createCameraSource(selectedModel, num);
    } else {
      getRuntimePermissions();
    }

    PlayMusic.loadResource();

    backBtn = (Button) findViewById(R.id.backBtn);
    backBtn.setOnClickListener(
        v->{
          backData();
          finish();
        }
    );

    //视频播放1
    if(!"".equals(videoUrl) && videoUrl != null){
      playVideo(videoUrl);
    }

  }

  private void playVideo(String videoUrl){
    videoView = new VideoView(this);
    videoView = (VideoView)findViewById(R.id.video);
    videoView.setVideoURI(Uri.parse(videoUrl));
    mediaController = new MediaController(this);
    mediaController.setMediaPlayer(videoView);
    videoView.setMediaController(mediaController);
    videoView.start();
    videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mp) {
        videoView.start();
      }
    });
    videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mp) {
        videoView.start();
      }
    });
  }

  private void backData(){
    Intent intent = new Intent();
    JSONObject jsonObject = new JSONObject();
    Integer completeNum = PoseClassifierProcessor.completeNum;
    Map<String, List<Float>> scoreMap = PoseClassifierProcessor.scoreMap;
    if(completeNum == null){
      completeNum = 0;
    }
    if(scoreMap != null && scoreMap.isEmpty()){
      List<Float> floatList = new ArrayList<>();
      for (Map.Entry<String, List<Float>> entry : scoreMap.entrySet()) {
        float sum = 0f;
        List<Float> value = entry.getValue();
        for (Float aFloat : value) {
          sum = sum + aFloat;
        }
        floatList.add(sum / value.size());
      }
      float sum = 0f;
      for (Float aFloat : floatList) {
        sum = sum + aFloat;
      }
      if(floatList.size() == 0){
        jsonObject.put("score", 0);
      }else {
        jsonObject.put("score", sum / floatList.size() * 100);
      }
    }else{
      jsonObject.put("score", 0);
    }
    if(num == completeNum.intValue()){
      jsonObject.put("completeStatus", 1);
    }else{
      jsonObject.put("completeStatus", 0);
    }
    intent.putExtra("respond", jsonObject.toString());
    setResult(CameraModule.REQUEST_CODE, intent);
    PoseClassifierProcessor.completeNum = 0;
    PoseClassifierProcessor.scoreMap.clear();
  }

  //选择那个检测类型，创建对应的检测模型
  private void createCameraSource(String model, Integer num) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
    }
    try {
      //获取多物体检测
      PoseDetectorOptionsBase poseDetectorOptions =
              PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
      //显示置信度
      boolean shouldShowInFrameLikelihood =
              PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
      //可视化深度通道
      boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
      //归一化深度通道
      boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
      //深蹲计数模式
      boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
      if(model.indexOf(STAND_ON_ONE_LEG) >= 0){
        //单脚站立
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_DJZL", num));
      }else if(model.indexOf(STRAIGHT_FORWARD_LEG_LIFT) >= 0){
        //前直抬腿
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_QZTT", num));
      }else if(model.indexOf(STANDING_KNEE_BEND) >= 0){
        //站立位屈膝
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_ZLWQX", num));
      }else{
        //深蹲
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_DETECTION", num));
      }
    } catch (RuntimeException e) {
      //Log.e(TAG, "Can not create image processor: " + model, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          //Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          //Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    createCameraSource(selectedModel, num);
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
    backData();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
  }

  private String[] getRequiredPermissions() {
    try {
      PackageInfo info =
          this.getPackageManager()
              .getPackageInfo(this.getPackageName(), PackageManager.GET_PERMISSIONS);
      String[] ps = info.requestedPermissions;
      if (ps != null && ps.length > 0) {
        return ps;
      } else {
        return new String[0];
      }
    } catch (Exception e) {
      return new String[0];
    }
  }

  private boolean allPermissionsGranted() {
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        return false;
      }
    }
    return true;
  }

  private void getRuntimePermissions() {
    List<String> allNeededPermissions = new ArrayList<>();
    for (String permission : getRequiredPermissions()) {
      if (!isPermissionGranted(this, permission)) {
        allNeededPermissions.add(permission);
      }
    }

    if (!allNeededPermissions.isEmpty()) {
      ActivityCompat.requestPermissions(
          this, allNeededPermissions.toArray(new String[0]), PERMISSION_REQUESTS);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    if (allPermissionsGranted()) {
      createCameraSource(selectedModel, num);
    }
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  private static boolean isPermissionGranted(Context context, String permission) {
    if (ContextCompat.checkSelfPermission(context, permission)
        == PackageManager.PERMISSION_GRANTED) {
      return true;
    }
    return false;
  }

  /**
   * 选择检测类型的方法
   * @param parent
   * @param view
   * @param pos
   * @param id
   */
  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    preview.stop();
    if (allPermissionsGranted()) {
      createCameraSource(selectedModel, num);
      startCameraSource();
    } else {
      getRuntimePermissions();
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (cameraSource != null) {
      if (isChecked) {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      }
    }
    preview.stop();
    startCameraSource();
  }


}
