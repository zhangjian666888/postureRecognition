package com.example.cameramodule.java.utils;

import com.google.mlkit.vision.pose.PoseLandmark;

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

}
