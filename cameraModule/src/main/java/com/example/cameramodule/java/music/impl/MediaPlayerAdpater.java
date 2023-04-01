package com.example.cameramodule.java.music.impl;


import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RawRes;

import com.example.cameramodule.java.music.IPlayerApi;
import com.example.cameramodule.java.music.OnPrepareCompletedListener;
import com.example.cameramodule.java.music.PlaybackInfoListener;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author: ZhangJian
 * @date: 2023/3/29 16:09
 * @description:
 */
public class MediaPlayerAdpater implements IPlayerApi {
    public static int STATUS_PALYINGP = 0;//正在播放
    public static int STATUS_STOP = 1;//暂停播放
    public static int STATUS_RESET = 2;//重置
    public static int STATUS_PLAY_COMPLETE = 3;//播放完成
    public static int STATUS_PREPER_COMPLETE = 4;//媒体流装载完成
    public static int STATUS_PREPER_ING = 5;//媒体流加载中
    public static int STATUS_ERROR = -1;//错误

    public int PLAYBACK_POSITION_REFRESH_INTERVAL_MS = 500;
    private final String TAG = "MediaPlayerHolder";
    private MediaPlayer mMediaPlayer;
    /**
     * 开启线程
     */
    private ScheduledExecutorService mExecutor;
    private PlaybackInfoListener mPlaybackInfoListener;
    private Runnable mSeekbarPositionUpdateTask;
//    private String musiUrl;//音乐地址，可以是本地的音乐，可以是网络的音乐

    private Context context;

    public MediaPlayerAdpater(Context context) {
        this.context = context;
    }

    public void setmPlaybackInfoListener(PlaybackInfoListener mPlaybackInfoListener) {
        this.mPlaybackInfoListener = mPlaybackInfoListener;
    }


    /**
     * 初始化MediaPlayer
     */
    private void initializeMediaPlayer() {

        //注册，播放完成后的监听
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopUpdatingCallbackWithPosition(true);
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onStateChanged(STATUS_PLAY_COMPLETE);
                    mPlaybackInfoListener.onPlayCompleted();
                }
                release();
            }
        });

        //监听媒体流是否装载完成
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                medisaPreparedCompled();
                if (mOnPrepareCompletedListener != null) {
                    mOnPrepareCompletedListener.onComplete();
                }
            }
        });

        // 监听媒体错误信息
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onStateChanged(STATUS_ERROR);
                }
                Log.d(TAG, "OnError - Error code: " + what + " Extra code: " + extra);
                switch (what) {
                    case -1004:
                        Log.d(TAG, "MEDIA_ERROR_IO");
                        break;
                    case -1007:
                        Log.d(TAG, "MEDIA_ERROR_MALFORMED");
                        break;
                    case 200:
                        Log.d(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                        break;
                    case 100:
                        Log.d(TAG, "MEDIA_ERROR_SERVER_DIED");
                        break;
                    case -110:
                        Log.d(TAG, "MEDIA_ERROR_TIMED_OUT");
                        break;
                    case 1:
                        Log.d(TAG, "MEDIA_ERROR_UNKNOWN");
                        break;
                    case -1010:
                        Log.d(TAG, "MEDIA_ERROR_UNSUPPORTED");
                        break;
                }
                switch (extra) {
                    case 800:
                        Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                        break;
                    case 702:
                        Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
                        break;
                    case 701:
                        Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                        break;
                    case 802:
                        Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                        break;
                    case 801:
                        Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE");
                        break;
                    case 1:
                        Log.d(TAG, "MEDIA_INFO_UNKNOWN");
                        break;
                    case 3:
                        Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                        break;
                    case 700:
                        Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                        break;
                }
                return false;
            }
        });
    }

    private int mMusicRawId = 0;

    private String mMusicUrl = null;
    private OnPrepareCompletedListener mOnPrepareCompletedListener;

    /**
     * 加载媒体资源
     *
     * @param musiUrl String:音乐地址，可以是本地的音乐，可以是网络的音乐
     **/
    @Override
    public void loadMedia(String musiUrl, OnPrepareCompletedListener listener) {
        if (TextUtils.isEmpty(musiUrl)) {
            Log.i(TAG, "地址为空");
            return;
        }
        mOnPrepareCompletedListener = listener;
        mMusicUrl = musiUrl;

        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onStateChanged(STATUS_PREPER_ING);
        }

        mMediaPlayer = new MediaPlayer();
        initializeMediaPlayer();
        try {
            //防止再次添加进来出现崩溃信息
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(musiUrl);
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage().toString());
        }
    }

    /**
     * 加载媒体资源
     **/
    @Override
    public void loadMedia(@RawRes int musicRawId, OnPrepareCompletedListener listener) {
        try {
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(musicRawId);
            if (afd == null) {
                Log.e(TAG, "afd == null");
                return;
            }

            mOnPrepareCompletedListener = listener;

            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(STATUS_PREPER_ING);
            }

            mMediaPlayer = new MediaPlayer();
            initializeMediaPlayer();
            //防止再次添加进来出现崩溃信息
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage().toString());
        }
    }


    /**
     * 释放媒体资源
     **/
    @Override
    public void release() {
        if (mMediaPlayer != null) {
            stopUpdatingCallbackWithPosition(false);
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMusicUrl = null;
            mMusicRawId = 0;
        }
    }

    /**
     * 判断是否正在播放
     **/
    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.isPlaying();
        }
        return false;
    }

    /**
     * 播放开始
     **/
    @Override
    public void play() {
        if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(STATUS_PALYINGP);
            }
            startUpdatingCallbackWithPosition();
        }
    }

    /**
     * 开启线程，获取当前播放的进度
     **/
    private void startUpdatingCallbackWithPosition() {
        if (mExecutor == null) {
            mExecutor = Executors.newSingleThreadScheduledExecutor();
        }
        if (mSeekbarPositionUpdateTask == null) {
            mSeekbarPositionUpdateTask = new Runnable() {
                @Override
                public void run() {
                    updateProgressCallbackTask();
                }
            };
        }

        mExecutor.scheduleAtFixedRate(
                mSeekbarPositionUpdateTask,
                0,
                PLAYBACK_POSITION_REFRESH_INTERVAL_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void reset() {
        if (mMediaPlayer != null) {
            if (mMusicUrl != null) {
                loadMedia(mMusicUrl, mOnPrepareCompletedListener);
            } else {
                loadMedia(mMusicRawId, mOnPrepareCompletedListener);
            }
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(STATUS_RESET);
            }
            stopUpdatingCallbackWithPosition(true);
        }
    }

    /**
     * 暂停
     **/
    @Override
    public void pause() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            if (mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onStateChanged(STATUS_STOP);
            }
            stopUpdatingCallbackWithPosition(false);
        }
    }

    /**
     * 更新当前的进度
     **/
    private void updateProgressCallbackTask() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            try {
                int currentPosition = mMediaPlayer.getCurrentPosition();
                if (mPlaybackInfoListener != null) {
                    mPlaybackInfoListener.onPositionChanged(currentPosition);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 加载完成回调:完成媒体流的装载
     **/
    private void medisaPreparedCompled() {
        int duration = mMediaPlayer.getDuration();//获取总时长
        if (mPlaybackInfoListener != null) {
            mPlaybackInfoListener.onTotalDuration(duration);
            mPlaybackInfoListener.onPositionChanged(0);
            mPlaybackInfoListener.onStateChanged(STATUS_PREPER_COMPLETE);
        }
    }

    /**
     * 滑动播放到某个位置
     **/
    @Override
    public void seekTo(int position) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(position);
        }
    }

    /**
     * 播放完成回调监听
     **/
    private void stopUpdatingCallbackWithPosition(boolean resetUIPlaybackPosition) {
        if (mExecutor != null) {
            mExecutor.shutdownNow();
            mExecutor = null;
            mSeekbarPositionUpdateTask = null;
            if (resetUIPlaybackPosition && mPlaybackInfoListener != null) {
                mPlaybackInfoListener.onPositionChanged(0);
            }
        }
    }
}

