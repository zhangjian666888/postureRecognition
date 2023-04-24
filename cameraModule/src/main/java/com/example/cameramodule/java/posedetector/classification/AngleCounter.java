package com.example.cameramodule.java.posedetector.classification;

import com.example.cameramodule.java.utils.CommonUtils;
import com.google.mlkit.vision.pose.Pose;

/**
 * @author: ZhangJian
 * @date: 2023/4/20 17:41
 * @description:
 */
public class AngleCounter {

    private static final float DEFAULT_ENTER_THRESHOLD = 1000f;

    private static final float DEFAULT_EXIT_THRESHOLD = 1000f;

    private final String className;

    private final Integer arthrosis;

    private final float enterThreshold;

    private final float exitThreshold;

    private int numRepeats;

    private boolean poseEntered;

    public AngleCounter(String className, Integer arthrosis) {
        this(className, arthrosis, DEFAULT_ENTER_THRESHOLD, DEFAULT_EXIT_THRESHOLD);
    }

    public AngleCounter(String className, Integer arthrosis, float enterThreshold, float exitThreshold) {
        this.className = className;
        this.arthrosis = arthrosis;
        this.enterThreshold = enterThreshold;
        this.exitThreshold = exitThreshold;
        numRepeats = 0;
        poseEntered = false;
    }

    public double getAngleByPose(Pose pose){
       return CommonUtils.getPosesByArthrosis(arthrosis, pose);
    }

    public int addClassificationResult(Pose pose) {

        double leftAnkleAngle = CommonUtils.getPosesByArthrosis(arthrosis, pose);

        if (!poseEntered) {
            poseEntered = leftAnkleAngle > enterThreshold;
            return numRepeats;
        }

        if (leftAnkleAngle < exitThreshold) {
            numRepeats++;
            poseEntered = false;
        }

        return numRepeats;
    }

    public String getClassName() {
        return className;
    }

    public int getNumRepeats() {
        return numRepeats;
    }

}
