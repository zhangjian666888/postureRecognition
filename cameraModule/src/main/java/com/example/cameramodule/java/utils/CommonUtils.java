package com.example.cameramodule.java.utils;

import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseLandmark;

import java.util.List;

import static java.lang.Math.atan2;

/**
 * @author: ZhangJian
 * @date: 2023/4/12 15:04
 * @description:
 */
public class CommonUtils {
    //根据三个点计算关节角度
    public static double getAngle(PoseLandmark firstPoint, PoseLandmark midPoint, PoseLandmark lastPoint) {
        double result = Math.toDegrees(
                atan2(lastPoint.getPosition().y - midPoint.getPosition().y, lastPoint.getPosition().x - midPoint.getPosition().x)
                        - atan2(firstPoint.getPosition().y - midPoint.getPosition().y, firstPoint.getPosition().x - midPoint.getPosition().x));
        result = Math.abs(result); // Angle should never be negative
        if (result > 180) {
            result = (360.0 - result); // Always get the acute representation of the angle
        }
        return result;
    }

    public static double getPosesByArthrosis(Integer arthrosis, Pose pose){
        List<PoseLandmark> poseLandmarks = pose.getAllPoseLandmarks();
        switch (arthrosis){
            case PoseLandmark.LEFT_ANKLE:
                //左踝
                return getAngle(
                        poseLandmarks.get(PoseLandmark.LEFT_FOOT_INDEX),
                        poseLandmarks.get(PoseLandmark.LEFT_ANKLE),
                        poseLandmarks.get(PoseLandmark.LEFT_KNEE));
            case PoseLandmark.RIGHT_ANKLE:
                //右踝
                return getAngle(
                        poseLandmarks.get(PoseLandmark.RIGHT_FOOT_INDEX),
                        poseLandmarks.get(PoseLandmark.RIGHT_ANKLE),
                        poseLandmarks.get(PoseLandmark.RIGHT_KNEE));
            case PoseLandmark.LEFT_KNEE:
                //左膝
                return getAngle(
                        poseLandmarks.get(PoseLandmark.LEFT_ANKLE),
                        poseLandmarks.get(PoseLandmark.LEFT_KNEE),
                        poseLandmarks.get(PoseLandmark.LEFT_HIP));
            case PoseLandmark.RIGHT_KNEE:
                //右膝
                return getAngle(
                        poseLandmarks.get(PoseLandmark.RIGHT_ANKLE),
                        poseLandmarks.get(PoseLandmark.RIGHT_KNEE),
                        poseLandmarks.get(PoseLandmark.RIGHT_HIP));
            case PoseLandmark.LEFT_HIP:
                //左臀
                return getAngle(
                        poseLandmarks.get(PoseLandmark.LEFT_KNEE),
                        poseLandmarks.get(PoseLandmark.LEFT_HIP),
                        poseLandmarks.get(PoseLandmark.LEFT_SHOULDER));
            case PoseLandmark.RIGHT_HIP:
                //右臀
                return getAngle(
                        poseLandmarks.get(PoseLandmark.RIGHT_KNEE),
                        poseLandmarks.get(PoseLandmark.RIGHT_HIP),
                        poseLandmarks.get(PoseLandmark.RIGHT_SHOULDER));
            case PoseLandmark.LEFT_SHOULDER:
                //左肩
                return getAngle(
                        poseLandmarks.get(PoseLandmark.LEFT_HIP),
                        poseLandmarks.get(PoseLandmark.LEFT_SHOULDER),
                        poseLandmarks.get(PoseLandmark.LEFT_ELBOW));
            case PoseLandmark.RIGHT_SHOULDER:
                //右肩
                return getAngle(
                        poseLandmarks.get(PoseLandmark.RIGHT_HIP),
                        poseLandmarks.get(PoseLandmark.RIGHT_SHOULDER),
                        poseLandmarks.get(PoseLandmark.RIGHT_ELBOW));
            case PoseLandmark.LEFT_ELBOW:
                //左肘
                return getAngle(
                        poseLandmarks.get(PoseLandmark.LEFT_SHOULDER),
                        poseLandmarks.get(PoseLandmark.LEFT_ELBOW),
                        poseLandmarks.get(PoseLandmark.LEFT_WRIST));
            case PoseLandmark.RIGHT_ELBOW:
                //右肘
                return getAngle(
                        poseLandmarks.get(PoseLandmark.RIGHT_SHOULDER),
                        poseLandmarks.get(PoseLandmark.RIGHT_ELBOW),
                        poseLandmarks.get(PoseLandmark.RIGHT_WRIST));
            case 50:
                //右肘
                return getAngle(
                        poseLandmarks.get(PoseLandmark.LEFT_FOOT_INDEX),
                        poseLandmarks.get(PoseLandmark.LEFT_KNEE),
                        poseLandmarks.get(PoseLandmark.LEFT_HEEL));
            default:
                return 0;
        }
    }

}
