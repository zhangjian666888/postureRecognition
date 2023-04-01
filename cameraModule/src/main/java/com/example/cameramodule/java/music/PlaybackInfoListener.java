package com.example.cameramodule.java.music;

/**
 * @author: ZhangJian
 * @date: 2023/3/29 16:12
 * @description:
 */
public interface PlaybackInfoListener {
    void onTotalDuration(int duration);//总时长

    void onPositionChanged(int position);//当前时长进度

    void onStateChanged(int state);//记录当前的状态

    void onPlayCompleted();//播放完成回调
}

