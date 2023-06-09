package com.example.cameramodule.java;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Outline;
import android.graphics.Rect;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.*;
import android.view.animation.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;
import com.alibaba.fastjson.JSONObject;
import com.example.cameramodule.*;
import com.example.cameramodule.java.model.Observer;
import com.example.cameramodule.java.music.OnPrepareCompletedListener;
import com.example.cameramodule.java.music.impl.MediaPlayerAdpater;
import com.example.cameramodule.java.posedetector.PoseDetectorProcessor;
import com.example.cameramodule.java.posedetector.classification.PoseClassifierProcessor;
import com.example.cameramodule.preference.PreferenceUtils;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static android.widget.RelativeLayout.CENTER_IN_PARENT;
import static com.example.cameramodule.java.posedetector.classification.PoseClassifierProcessor.completeNum;
import static com.example.cameramodule.java.utils.CommonUtils.NumberToChn;

/** Live preview demo for ML Kit APIs. */
//ML Kit api的实时预览演示
@KeepName
public final class LivePreviewActivity extends AppCompatActivity
implements OnRequestPermissionsResultCallback,
        OnItemSelectedListener,
        CompoundButton.OnCheckedChangeListener,
        SurfaceHolder.Callback,
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnInfoListener, View.OnClickListener,
        MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnVideoSizeChangedListener,
        SeekBar.OnSeekBarChangeListener, View.OnTouchListener {

  private static final String POSE_DETECTION = "深蹲";
  private static final String STAND_ON_ONE_LEG = "单脚站立";
  private static final String STRAIGHT_FORWARD_LEG_LIFT = "前直抬腿";
  private static final String STANDING_KNEE_BEND = "站立位屈膝";
  private static final String PDTZ = "站立提踵";
  private static final String ZZTZ = "坐姿提踵";
  private static final String WSKH = "蚌式开合";
  private static final String TAG = "LivePreviewActivity";
  private static final int PERMISSION_REQUESTS = 1;

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  //默认模型
  private String selectedModel = POSE_DETECTION;
  private Integer num = 10;
  private Integer indexNum = 1;
  private ToggleButton backBtn;
  private ImageButton videoBtn;
  private ImageView backVideo;
  private TextView totalText;
  private TextView numText;
  private String videoUrl = "";
  private ScheduledExecutorService  scheduledService = Executors.newScheduledThreadPool(10);
  private RelativeLayout reportView;
  private TextView useTimeNum;
  private Button punchcardButton;
  private TextView titleText;
  private TextView complateNum;
  private long startTimeDate;
  private RelativeLayout quitLayout;
  private TextView confirmButton;
  private TextView abolishButton;
  private ImageView bodyFouce;
  private ImageView bodySuccess;
  //private ImageView bodyNormal;
  private String actionDsc = "";
  private ImageView backActionDsc;
  private RelativeLayout actionDscLayout;
  private TextView actionDscText;
  private ImageButton openActionDsc;
  private TextView rxtsText;
  public static boolean isStart = false;
  private LinearLayout root;
  private  Dialog mCameraDialog;
  private Button mCountDown;
  private TextView mStartCount;
  private static final int START_COUNTING = 3;
  private RelativeLayout countDownLayout;

  //视频控件
  private ImageView playOrPauseIv;
  private SurfaceView videoSuf;
  private MediaPlayer mPlayer;
  private SeekBar mSeekBar;
  private String path;
  private RelativeLayout rootViewRl;
  private LinearLayout controlLl;
  private TextView startTime, endTime;
  private ImageView forwardButton, backwardButton;
  private boolean isShow = false;
  public static final int UPDATE_TIME = 0x0001;
  public static final int HIDE_CONTROL = 0x0002;

  private Handler mHandler = new Handler() {
    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case UPDATE_TIME:
          updateTime();
          mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 500);
          break;
        case HIDE_CONTROL:
          hideControl();
          break;
        case START_COUNTING:
          int count = (int) msg.obj;
          mStartCount.setText(count + "");
          if (count > 1) {
            Message msg1 = obtainMessage();
            msg1.what = START_COUNTING;
            msg1.obj = count - 1;
            sendMessageDelayed(msg1, 1000);
          }else{

          }
          break;
      }
    }
  };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //设置全屏
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_vision_live_preview);
    //接收传过得参数
    Intent intent1 = getIntent();
    if(intent1 != null){
      selectedModel = intent1.getStringExtra("actionName");
      if(intent1.getStringExtra("num") != null && !"".equals(intent1.getStringExtra("num"))){
        num = Integer.parseInt(intent1.getStringExtra("num"));
      }
      if(intent1.getStringExtra("indexNum") != null && !"".equals(intent1.getStringExtra("indexNum"))){
        indexNum = Integer.parseInt(intent1.getStringExtra("indexNum"));
      }
      videoUrl = intent1.getStringExtra("videoUrl");
      actionDsc = intent1.getStringExtra("actionDsc");
    }
    preview = findViewById(R.id.preview_view);
    graphicOverlay = findViewById(R.id.graphic_overlay);

    //文本框
    numText = (TextView)findViewById(R.id.numText);
    totalText = (TextView)findViewById(R.id.totalText);

    //前后置摄像头切换按钮
    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);
    //返回按钮
    root = (LinearLayout) LayoutInflater.from(this).inflate(R.layout.bottom_dialog, null);
    //初始化视图
    mCameraDialog = new Dialog(this, R.style.BottomDialog);
    mCameraDialog.setContentView(root);
    Window dialogWindow = mCameraDialog.getWindow();
    dialogWindow.setGravity(Gravity.BOTTOM);
    WindowManager.LayoutParams lp = dialogWindow.getAttributes(); // 获取对话框当前的参数值
    lp.x = 0; // 新位置X坐标
    lp.y = 0; // 新位置Y坐标
    lp.width = (int) getResources().getDisplayMetrics().widthPixels; // 宽度
    root.measure(0, 0);
    lp.height = root.getMeasuredHeight();
    lp.alpha = 9f; // 透明度
    dialogWindow.setAttributes(lp);
    backBtn = (ToggleButton) findViewById(R.id.backBtn);
    backBtn.setOnClickListener(
            v->{
              mCameraDialog.show();
            }
    );
    abolishButton = (TextView) root.findViewById(R.id.btn_cancel);
    abolishButton.setOnClickListener(
          v->{
            mCameraDialog.hide();
          }
    );
    confirmButton = (TextView) root.findViewById(R.id.confirmButton);
    confirmButton.setOnClickListener(
          v->{
            backData();
            finish();
          }
    );
    //视频播放初始化
    initViews();
    initData();
    initSurfaceView();
    initPlayer();
    initEvent();
    //显示视频按钮
    videoBtn = (ImageButton) findViewById(R.id.videoButton);
    videoBtn.setOnClickListener(
          v->{
            rootViewRl.setVisibility(View.VISIBLE);
          }
    );
    //隐藏视频按钮
    backVideo = (ImageView) findViewById(R.id.backVideo);
    backVideo.setOnClickListener(
          v->{
            rootViewRl.setVisibility(View.INVISIBLE);
            mPlayer.seekTo(0);
          }
    );

    //动作要领弹框
    openActionDsc = (ImageButton) findViewById(R.id.openActionDsc);
    actionDscLayout = (RelativeLayout) findViewById(R.id.actionDscLayout);
    backActionDsc = (ImageView) findViewById(R.id.backActionDsc);
    actionDscText = (TextView) findViewById(R.id.actionDscText);
    actionDscText.setMovementMethod(ScrollingMovementMethod.getInstance());
    if(actionDsc != null && !"".equals(actionDsc)){
      actionDscText.setText(Html.fromHtml(actionDsc));
    }
    openActionDsc.setOnClickListener(
            v->{
              actionDscLayout.setVisibility(View.VISIBLE);
            }
    );
    backActionDsc.setOnClickListener(
            v->{
              actionDscLayout.setVisibility(View.INVISIBLE);
            }
    );
    //报告弹框
    reportView = (RelativeLayout) findViewById(R.id.report);
    reportView.setVisibility(View.INVISIBLE);
    //动作标题
    titleText = (TextView) findViewById(R.id.titleText);
    titleText.setText(selectedModel);
    //动作完成数
    complateNum = (TextView) findViewById(R.id.complateNum);
    complateNum.setText(num+"");
    //使用时间
    useTimeNum = (TextView) findViewById(R.id.useTimeNum);
    punchcardButton = (Button) findViewById(R.id.punchcard);
    punchcardButton.setOnClickListener(
        v->{
          reportView.setVisibility(View.INVISIBLE);
          backData();
          finish();
        }
    );
    startTimeDate = System.currentTimeMillis();
    //完成动作的监控
    Handler handler2 = new Handler();
    Observer.setComplateNumOnChangeListener(new Observer.OnChangeListener() {
      @Override
      public void onChange() {
        if(Observer.getComplateNum() == num){
          handler2.post(new Runnable() {
            @Override
            public void run() {
              long endTime = System.currentTimeMillis();
              long second = (endTime - startTimeDate) / 1000;
              useTimeNum.setText((second / 60) + ":" + (second % 60));
              reportView.setVisibility(View.VISIBLE);
              backBtn.setVisibility(View.INVISIBLE);
              videoBtn.setVisibility(View.INVISIBLE);
            }
          });
        }
      }
    });
    //定时播放人像检测
    scheduledService.scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        if(Observer.isBodyFlag()){
          if(completeNum < num.intValue()){
            long ys = System.currentTimeMillis() % 2;
            if(ys > 0){
              playMusic(LivePreviewActivity.this, LivePreviewActivity.this.getString(R.string.rxjc));
            }else {
              playMusic(LivePreviewActivity.this, LivePreviewActivity.this.getString(R.string.rxjc2));
            }
          }
        }
      }
    },20, 10, TimeUnit.SECONDS);
    //人像框的显示隐藏
    bodyFouce = (ImageView) findViewById(R.id.bodyFouce);
    bodySuccess = (ImageView) findViewById(R.id.bodySuccess);
    //bodyNormal = (ImageView) findViewById(R.id.bodyNormal);
    //人像提示文字
    rxtsText = (TextView) findViewById(R.id.rxtsText);
    Handler handler3 = new Handler();
    Observer.setBodyFlagOnChangeListener(new Observer.OnChangeListener() {
      @Override
      public void onChange() {
        if(Observer.isBodyFlag()){
          handler3.post(new Runnable() {
            @Override
            public void run() {
              bodyFouce.setVisibility(View.VISIBLE);
              bodySuccess.setVisibility(View.INVISIBLE);
              if(isStart){
                rxtsText.setVisibility(View.VISIBLE);
              }
            }
          });
        }else {
          isStart = true;
          handler3.post(new Runnable() {
            @Override
            public void run() {
              bodyFouce.setVisibility(View.INVISIBLE);
              bodySuccess.setVisibility(View.VISIBLE);
              rxtsText.setVisibility(View.INVISIBLE);
            }
          });
          handler3.post(new Runnable() {
            @Override
            public void run() {
              bodyFouce.setVisibility(View.INVISIBLE);
              bodySuccess.setVisibility(View.INVISIBLE);
              rxtsText.setVisibility(View.INVISIBLE);
            }
          });
        }
      }
    });

    //倒计时
    countDownLayout = (RelativeLayout) findViewById(R.id.countDown);
    mStartCount = (TextView) findViewById(R.id.count_text);
    //mCountDown = (Button) findViewById(R.id.count_button);
    //mCountDown.setOnClickListener(new View.OnClickListener() {
    //  @Override
    //  public void onClick(View view) {
    //    countDownLayout.setVisibility(View.INVISIBLE);
    //    //创建相机
    //    if (allPermissionsGranted()) {
    //      //默认选择第一个检测类型
    //      createCameraSource(selectedModel, num);
    //      //播放开始的音乐
    //      playMusic(LivePreviewActivity.this, LivePreviewActivity.this.getString(R.string.ydzb));
    //    } else {
    //      getRuntimePermissions();
    //    }
    //  }
    //});
    try {
      String dzsx = "http://47.95.211.83/app/第"+ indexNum +"个动作.MP3";
      String dzmc = "http://47.95.211.83/app/"+selectedModel+".MP3";
      playDzsxMusic(dzsx,  dzmc);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private void showTskView(){
    //消息弹框
    TextView messageText = (TextView) findViewById(R.id.tkText);
    Handler handler = new Handler();
    handler.postDelayed(new Runnable() {
      @Override
      public void run() {
        messageText.setVisibility(View.INVISIBLE);
      }
    }, 5000);
  }
  private void showDjsView(){
    Message msg = mHandler.obtainMessage();
    msg.what = START_COUNTING;
    msg.obj = 3;
    mHandler.sendMessageDelayed(msg, 3);
  }
  private MediaPlayer dzsxMediaPlayer;
  public void playDzsxMusic(String url, String dzmc) throws IOException {
    dzsxMediaPlayer = new MediaPlayer();
    dzsxMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    dzsxMediaPlayer.reset();
    dzsxMediaPlayer.setDataSource(url);
    dzsxMediaPlayer.prepareAsync();
    dzsxMediaPlayer.setVolume(1f, 1f);
    dzsxMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        dzsxMediaPlayer.start();
        //String number = NumberToChn(indexNum);
        mStartCount.setText("第"+ indexNum +"个动作");
      }
    });
    dzsxMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
        try {
          playDzmcMusic(dzmc);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private MediaPlayer dzmcMediaPlayer;
  public void playDzmcMusic(String url) throws IOException {
    dzmcMediaPlayer = new MediaPlayer();
    dzmcMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    dzmcMediaPlayer.reset();
    dzmcMediaPlayer.setDataSource(url);
    dzmcMediaPlayer.prepareAsync();
    dzmcMediaPlayer.setVolume(1f, 1f);
    dzmcMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        dzmcMediaPlayer.start();
        mStartCount.setText(selectedModel);
      }
    });
    dzmcMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
        try {
          playZbksMusic();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private MediaPlayer qzbMediaPlayer;
  public void playZbksMusic() throws IOException {
    qzbMediaPlayer = new MediaPlayer();
    qzbMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    qzbMediaPlayer.reset();
    qzbMediaPlayer.setDataSource(LivePreviewActivity.this.getString(R.string.qzb));
    qzbMediaPlayer.prepareAsync();
    qzbMediaPlayer.setVolume(1f, 1f);
    qzbMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        mStartCount.setText("请准备");
        qzbMediaPlayer.start();
      }
    });
    qzbMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
        try {
          playdjsMusic();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private MediaPlayer djsMediaPlayer;
  public void playdjsMusic() throws IOException {
    djsMediaPlayer = new MediaPlayer();
    djsMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    djsMediaPlayer.reset();
    djsMediaPlayer.setDataSource(LivePreviewActivity.this.getString(R.string.djs));
    djsMediaPlayer.prepareAsync();
    djsMediaPlayer.setVolume(1f, 1f);
    djsMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        djsMediaPlayer.start();
        showDjsView();
      }
    });
    djsMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
        try {
          playksMusic();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }

  private MediaPlayer ksMediaPlayer;
  public void playksMusic() throws IOException {
    ksMediaPlayer = new MediaPlayer();
    ksMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    ksMediaPlayer.reset();
    ksMediaPlayer.setDataSource(LivePreviewActivity.this.getString(R.string.ks));
    ksMediaPlayer.prepareAsync();
    ksMediaPlayer.setVolume(1f, 1f);
    ksMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
      @Override
      public void onPrepared(MediaPlayer mediaPlayer) {
        mStartCount.setText("开始");
        ksMediaPlayer.start();

      }
    });
    ksMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
      @Override
      public void onCompletion(MediaPlayer mediaPlayer) {
        mediaPlayer.release();
        countDownLayout.setVisibility(View.INVISIBLE);
        showTskView();
        backBtn.setVisibility(View.VISIBLE);
        //播放开始的音乐
        playMusic(LivePreviewActivity.this, LivePreviewActivity.this.getString(R.string.ydzb));
        //创建相机
        if (allPermissionsGranted()) {
          //默认选择第一个检测类型
          createCameraSource(selectedModel, num);
        } else {
          getRuntimePermissions();
        }
      }
    });
  }

  //播放音乐
  public static MediaPlayerAdpater mediaPlayerAdpater;
  public static void playMusic(Context context, String url){
    if(mediaPlayerAdpater != null){
      if(mediaPlayerAdpater.isPlaying()){
        mediaPlayerAdpater.release();
      }
    }
    mediaPlayerAdpater = new MediaPlayerAdpater(context);
    mediaPlayerAdpater.loadMedia(url, new OnPrepareCompletedListener() {
      @Override
      public void onComplete() {
        mediaPlayerAdpater.play();
      }
    });
  }

  public static void stopAllMusic(){
    if(mediaPlayerAdpater != null){
      if(mediaPlayerAdpater.isPlaying()){
        mediaPlayerAdpater.release();
      }
    }
  }

  private void backData(){
    scheduledService.shutdown();
    stopAllMusic();
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
    //PoseClassifierProcessor.flag = false;
  }

  //选择那个检测类型，创建对应的检测模型
  private void createCameraSource(String model, Integer num) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
      cameraSource.setFacing(1);
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
                        "POSE_DJZL", num, numText, totalText));
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
                        "POSE_QZTT", num, numText, totalText));
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
                        "POSE_ZLWQX", num, numText, totalText));
      }else if(model.indexOf(PDTZ) >= 0){
        //平地提踵
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_PDTZ", num, numText, totalText));
      }else if(model.indexOf(ZZTZ) >= 0){
        //坐姿提踵
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_ZZTZ", num, numText, totalText));
      }else if(model.indexOf(WSKH) >= 0){
        //蚌式开合
        cameraSource.setMachineLearningFrameProcessor(
                new PoseDetectorProcessor(
                        this,
                        poseDetectorOptions,
                        shouldShowInFrameLikelihood,
                        visualizeZ,
                        rescaleZ,
                        true,
                        true,
                        "POSE_WSKH", num, numText, totalText));
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
                        "POSE_DETECTION", num, numText, totalText));
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
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
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
   */
  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    preview.stop();
    if (allPermissionsGranted()) {
      //createCameraSource(selectedModel, num);
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
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      }
    }
    preview.stop();
    startCameraSource();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
      return false;
    }
    return super.onKeyDown(keyCode, event);
  }

  /*视频播放*/
  private void initData() {
    path = videoUrl;//这里写上你的视频地址
  }

  private void initEvent() {
    playOrPauseIv.setOnClickListener(this);
    rootViewRl.setOnClickListener(this);
    rootViewRl.setOnTouchListener(this);
    forwardButton.setOnClickListener(this);
    backwardButton.setOnClickListener(this);
    mSeekBar.setOnSeekBarChangeListener(this);
  }
  private void initSurfaceView() {
    videoSuf = (SurfaceView) findViewById(R.id.surfaceView);
    videoSuf.setZOrderOnTop(false);
    videoSuf.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    videoSuf.getHolder().addCallback(this);
    videoSuf.setOutlineProvider(new ViewOutlineProvider() {
      @Override
      public void getOutline(View view, Outline outline) {
        Rect rect = new Rect();
        view.getGlobalVisibleRect(rect);
        int leftMargin = 0;
        int topMargin = 0;
        Rect selfRect = new Rect(leftMargin, topMargin,
                rect.right - rect.left - leftMargin,
                rect.bottom - rect.top - topMargin);
        outline.setRoundRect(selfRect, 50);
      }
    });
    videoSuf.setClipToOutline(true);
  }

  private void initPlayer() {
    mPlayer = new MediaPlayer();
    mPlayer.setOnCompletionListener(this);
    mPlayer.setOnErrorListener(this);
    mPlayer.setOnInfoListener(this);
    mPlayer.setOnPreparedListener(this);
    mPlayer.setOnSeekCompleteListener(this);
    mPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
      @Override
      public void onVideoSizeChanged(MediaPlayer mediaPlayer, int width, int height) {
        changeVideoSize(videoSuf, width, height);
      }
    });
    try {
      //使用手机本地视频
      mPlayer.setDataSource(path);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void initViews() {
    playOrPauseIv = (ImageView) findViewById(R.id.playOrPause);
    startTime = (TextView) findViewById(R.id.tv_start_time);
    endTime = (TextView) findViewById(R.id.tv_end_time);
    mSeekBar = (SeekBar) findViewById(R.id.tv_progess);
    controlLl = (LinearLayout) findViewById(R.id.control_ll);
    forwardButton = (ImageView) findViewById(R.id.tv_forward);
    backwardButton = (ImageView) findViewById(R.id.tv_backward);
    rootViewRl = (RelativeLayout) findViewById(R.id.root_rl);
    rootViewRl.setVisibility(View.INVISIBLE);
  }

  public void changeVideoSize(SurfaceView mSurfaceView, int videoWidth, int videoHeight) {
    int surfaceWidth = mSurfaceView.getWidth();
    int surfaceHeight = mSurfaceView.getHeight();
    //根据视频尺寸去计算->视频可以在sufaceView中放大的最大倍数。
    float max;
    if (getResources().getConfiguration().orientation== ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
      //竖屏模式下按视频宽度计算放大倍数值
      max = Math.max((float) videoWidth / (float) surfaceWidth,(float) videoHeight / (float) surfaceHeight);
    } else{
      //横屏模式下按视频高度计算放大倍数值
      max = Math.max(((float) videoWidth/(float) surfaceHeight),(float) videoHeight/(float) surfaceWidth);
    }

    //视频宽高分别/最大倍数值 计算出放大后的视频尺寸
    videoWidth = (int) Math.ceil((float) videoWidth / max);
    videoHeight = (int) Math.ceil((float) videoHeight / max);

    //无法直接设置视频尺寸，将计算出的视频尺寸设置到surfaceView 让视频自动填充。
    RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(videoWidth, videoHeight);
    layoutParams.addRule(CENTER_IN_PARENT);
    mSurfaceView.setLayoutParams(layoutParams);
    ConstraintLayout.LayoutParams cl = new ConstraintLayout.LayoutParams(videoWidth, videoHeight);
    cl.bottomToBottom = 0;
    cl.leftToLeft = 0;
    cl.rightToRight = 0;
    cl.topToTop = 0;
    cl.circleRadius = 10;
    rootViewRl.setLayoutParams(cl);
  }

  @Override
  public void surfaceCreated(SurfaceHolder holder) {
    mPlayer.setDisplay(holder);
    mPlayer.prepareAsync();
  }

  @Override
  public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

  }

  @Override
  public void surfaceDestroyed(SurfaceHolder holder) {

  }
  @Override
  public void onPrepared(MediaPlayer mp) {
    startTime.setText(getTimeLine(mp.getCurrentPosition()));
    endTime.setText(getTimeLine(mp.getDuration()));
    mSeekBar.setMax(mp.getDuration());
    mSeekBar.setProgress(mp.getCurrentPosition());
  }
  @Override
  public void onCompletion(MediaPlayer mp) {

  }
  @Override
  public boolean onError(MediaPlayer mp, int what, int extra) {
    return false;
  }

  @Override
  public boolean onInfo(MediaPlayer mp, int what, int extra) {
    return false;
  }
  private void play() {
    if (mPlayer == null) {
      return;
    }
    Log.i("playPath", path);
    if (mPlayer.isPlaying()) {
      mPlayer.pause();
      mHandler.removeMessages(UPDATE_TIME);
      mHandler.removeMessages(HIDE_CONTROL);
      playOrPauseIv.setVisibility(View.VISIBLE);
      forwardButton.setVisibility(View.VISIBLE);
      backwardButton.setVisibility(View.VISIBLE);
      playOrPauseIv.setImageResource(android.R.drawable.ic_media_play);
    } else {
      mPlayer.start();
      mHandler.sendEmptyMessageDelayed(UPDATE_TIME, 500);
      mHandler.sendEmptyMessageDelayed(HIDE_CONTROL, 5000);
      playOrPauseIv.setVisibility(View.INVISIBLE);
      forwardButton.setVisibility(View.INVISIBLE);
      backwardButton.setVisibility(View.INVISIBLE);
      playOrPauseIv.setImageResource(android.R.drawable.ic_media_pause);
    }
  }
  @Override
  public void onSeekComplete(MediaPlayer mp) {
    //TODO
  }

  @Override
  public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {

  }
  @Override
  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.tv_backward) {
      backWard();
    } else if (id == R.id.tv_forward) {
      forWard();
    } else if (id == R.id.playOrPause) {
      play();
    } else if (id == R.id.root_rl) {
      showControl();
    }
  }
  /**
   * 更新播放时间
   */
  private void updateTime() {
    startTime.setText(getTimeLine(mPlayer.getCurrentPosition()));
    mSeekBar.setProgress(mPlayer.getCurrentPosition());
  }

  /**
   * 隐藏进度条
   */
  private void hideControl() {
    isShow = false;
    mHandler.removeMessages(UPDATE_TIME);
    controlLl.animate().setDuration(300).translationY(controlLl.getHeight());
  }
  /**
   * 显示进度条
   */
  private void showControl() {
    if (isShow) {
      play();
    }
    isShow = true;
    mHandler.removeMessages(HIDE_CONTROL);
    mHandler.sendEmptyMessage(UPDATE_TIME);
    mHandler.sendEmptyMessageDelayed(HIDE_CONTROL, 5000);
    controlLl.animate().setDuration(300).translationY(0);
  }
  /**
   * 设置快进10秒方法
   */
  private void forWard(){
    if(mPlayer != null){
      int position = mPlayer.getCurrentPosition();
      mPlayer.seekTo(position + 10000);
    }
  }

  /**
   * 设置快退10秒的方法
   */
  public void backWard(){
    if(mPlayer != null){
      int position = mPlayer.getCurrentPosition();
      if(position > 10000){
        position-=10000;
      }else{
        position = 0;
      }
      mPlayer.seekTo(position);
    }
  }

  //OnSeekBarChangeListener
  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
    if(mPlayer != null && b){
      mPlayer.seekTo(progress);
    }
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {

  }

  @Override
  public boolean onTouch(View view, MotionEvent motionEvent) {
    return false;
  }

  public String getTimeLine(int time){
    if(time == 0){
      return "00:00";
    }else {
      int second = time / 1000;
      int second2 = second % 60;
      int minute = second / 60;
      String minuteStr = "";
      if(minute < 10){
        minuteStr = "0" + minute;
      }else{
        minuteStr = "" + minute;
      }
      String secondStr = "";
      if(second2 < 10){
        secondStr = "0" + second2;
      }else{
        secondStr = "" + second2;
      }
      return minuteStr + ":" + secondStr;
    }
  }

}
