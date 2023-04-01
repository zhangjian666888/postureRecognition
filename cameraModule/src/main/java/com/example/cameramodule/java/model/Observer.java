package com.example.cameramodule.java.model;

/**
 * @author: ZhangJian
 * @date: 2023/3/30 16:26
 * @description:
 */
public class Observer {

    private static int complateNum;

    private static boolean bodyFlag;

    public static int getComplateNum() {
        return complateNum;
    }

    public static void setComplateNum(int complateNum) {
        Observer.complateNum = complateNum;
        onComplateNumChangeListener.onChange();
    }

    public static boolean isBodyFlag() {
        return bodyFlag;
    }

    public static void setBodyFlag(boolean bodyFlag) {
        Observer.bodyFlag = bodyFlag;
        onBodyFlagChangeListener.onChange();
    }

    public interface OnChangeListener {
        void onChange();
    }

    private static OnChangeListener onComplateNumChangeListener;
    private static OnChangeListener onBodyFlagChangeListener;

    public static void setComplateNumOnChangeListener(OnChangeListener onChange){
        onComplateNumChangeListener = onChange;
    }

    public static void setBodyFlagOnChangeListener(OnChangeListener onChange){
        onBodyFlagChangeListener = onChange;
    }

}
