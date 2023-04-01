package com.example.cameramodule.java.music;
import androidx.annotation.RawRes;

/**
 * @author: ZhangJian
 * @date: 2023/3/29 16:06
 * @description:
 */
public interface IPlayerApi {
    /**
     * 加载媒体资源
     *
     * @param musiUrl
     */
    void loadMedia(String musiUrl, OnPrepareCompletedListener listener);

    /**
     * 加载元数据媒体资源
     *
     * @param musicRawId
     */
    void loadMedia(@RawRes int musicRawId, OnPrepareCompletedListener listener);

    /**
     * 释放资源
     */
    void release();

    /**
     * 判断是否在播放
     *
     * @return
     */
    boolean isPlaying();

    /**
     * 开始播放
     */
    void play();

    /**
     * 重置
     */
    void reset();

    /**
     * 暂停
     */
    void pause();

    /**
     * 滑动到某个位置
     */
    void seekTo(int position);
}

