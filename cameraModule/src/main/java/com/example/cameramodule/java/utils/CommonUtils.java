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

    private static String CHN_NUMBER[] = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
    private static String CHN_UNIT[] = {"", "十", "百", "千"};          //权位
    private static String CHN_UNIT_SECTION[] = {"", "万", "亿", "万亿"}; //节权位

    public static String NumberToChn(int num) {
        StringBuffer returnStr = new StringBuffer();
        Boolean needZero = false;
        int pos = 0;           //节权位的位置
        if (num == 0) {
            //如果num为0，进行特殊处理。
            returnStr.insert(0, CHN_NUMBER[0]);
        }
        while (num > 0) {
            int section = num % 10000;
            if (needZero) {
                returnStr.insert(0, CHN_NUMBER[0]);
            }
            String sectionToChn = SectionNumToChn(section);
            //判断是否需要节权位
            sectionToChn += (section != 0) ? CHN_UNIT_SECTION[pos] : CHN_UNIT_SECTION[0];
            returnStr.insert(0, sectionToChn);
            needZero = ((section < 1000 && section > 0) ? true : false); //判断section中的千位上是不是为零，若为零应该添加一个零。
            pos++;
            num = num / 10000;
        }
        return returnStr.toString();
    }

    /**
     * 将四位的section转换为中文数字
     * @param section
     * @return
     */
    public static String SectionNumToChn(int section) {
        StringBuffer returnStr = new StringBuffer();
        int unitPos = 0;       //节权位的位置编号，0-3依次为个十百千;

        Boolean zero = true;
        while (section > 0) {

            int v = (section % 10);
            if (v == 0) {
                if ((section == 0) || !zero) {
                    zero = true; /*需要补0，zero的作用是确保对连续的多个0，只补一个中文零*/
                    //chnStr.insert(0, chnNumChar[v]);
                    returnStr.insert(0, CHN_NUMBER[v]);
                }
            } else {
                zero = false; //至少有一个数字不是0
                StringBuffer tempStr = new StringBuffer(CHN_NUMBER[v]);//数字v所对应的中文数字
                tempStr.append(CHN_UNIT[unitPos]);  //数字v所对应的中文权位
                returnStr.insert(0, tempStr);
            }
            unitPos++; //移位
            section = section / 10;
        }
        return returnStr.toString();
    }

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
