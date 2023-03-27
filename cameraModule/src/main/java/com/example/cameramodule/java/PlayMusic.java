package com.example.cameramodule.java;

import android.media.MediaPlayer;

import java.io.IOException;

/**
 * @author: ZhangJian
 * @date: 2023/3/15 14:20
 * @description:
 */
public class PlayMusic {

    private static MediaPlayer mediaPlayUp = new MediaPlayer();
    private static MediaPlayer mediaPlayComplete = new MediaPlayer();

    public static void loadResource(){
        try {
            mediaPlayUp.setDataSource("http://43.143.181.73/app/ydybhzys.mp3"); // 指定音频文件的路径
            mediaPlayUp.prepareAsync(); // 让MediaPlayer进入到准备状态

            mediaPlayComplete.setDataSource("http://43.143.181.73/app/ydwc.mp3"); // 指定音频文件的路径
            mediaPlayComplete.prepareAsync(); // 让MediaPlayer进入到准备状态
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void playUp(){
        mediaPlayUp.start();
    }

    public static void playComplete(){
        mediaPlayComplete.start();

    }

}
