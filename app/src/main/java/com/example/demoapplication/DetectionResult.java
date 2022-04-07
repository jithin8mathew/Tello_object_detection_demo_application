package com.example.demoapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import android.os.Bundle;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

class Result {
    int classIndex;
    Float score;
    Rect rect;

    public Result(int cls, Float output, Rect rect) {
        this.classIndex = cls;
        this.score = output;
        this.rect = rect;
    }
};

class ImageProcessing{
    static float[] NO_MEAN_RGB = new float[] {0.0f, 0.0f, 0.0f};
    static float[] NO_STD_RGB = new float[] {1.0f, 1.0f, 1.0f};

    // model input image size
    static int jInputW = 640;
    static int jInputH = 640;

    // model output is of size 25200*(num_of_class+5)
    private static int mOutputRow = 25200; // as decided by the YOLOv5 model for input image of size 640*640
    private static int mOutputColumn = 85; // left, top, right, bottom, score and 80 class probability
    //    private static float mThreshold = 0.30f; // score above which a detection is generated
    private static int mNmsLimit = 1000;

    static String[] jClasses;

    // The two methods nonMaxSuppression and IOU below are ported from https://github.com/hollance/YOLO-CoreML-MPSNNGraph/blob/master/Common/Helpers.swift
    /**
     Removes bounding boxes that overlap too much with other boxes that have
     a higher score.
     - Parameters:
     - boxes: an array of bounding boxes and their scores
     - limit: the maximum number of boxes that will be selected
     - threshold: used to decide whether boxes overlap too much
     */
    static ArrayList<Result> nonMaxSuppression(ArrayList<Result> boxes, int limit, float threshold) {

        // Do an argsort on the confidence scores, from high to low.
        Collections.sort(boxes,
                new Comparator<Result>() {
                    @Override
                    public int compare(Result o1, Result o2) {
                        return o1.score.compareTo(o2.score);
                    }
                });

        ArrayList<Result> selected = new ArrayList<>();
        boolean[] active = new boolean[boxes.size()];
        Arrays.fill(active, true);
        int numActive = active.length;

        // The algorithm is simple: Start with the box that has the highest score.
        // Remove any remaining boxes that overlap it more than the given threshold
        // amount. If there are any boxes left (i.e. these did not overlap with any
        // previous boxes), then repeat this procedure, until no more boxes remain
        // or the limit has been reached.
        boolean done = false;
        for (int i=0; i<boxes.size() && !done; i++) {
            if (active[i]) {
                Result boxA = boxes.get(i);
                selected.add(boxA);
                if (selected.size() >= limit) break;

                for (int j=i+1; j<boxes.size(); j++) {
                    if (active[j]) {
                        Result boxB = boxes.get(j);
                        if (IOU(boxA.rect, boxB.rect) > threshold) {
                            active[j] = false;
                            numActive -= 1;
                            if (numActive <= 0) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    /**
     Computes intersection-over-union overlap between two bounding boxes.
     */
    static float IOU(Rect a, Rect b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        if (areaA <= 0.0) return 0.0f;

        float areaB = (b.right - b.left) * (b.bottom - b.top);
        if (areaB <= 0.0) return 0.0f;

        float intersectionMinX = Math.max(a.left, b.left);
        float intersectionMinY = Math.max(a.top, b.top);
        float intersectionMaxX = Math.min(a.right, b.right);
        float intersectionMaxY = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0) *
                Math.max(intersectionMaxX - intersectionMinX, 0);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    static ArrayList<Result> outputsNMSFilter(float[] outputs, float mThreshold, float imgScaleX, float imgScaleY, float ivScaleX, float ivScaleY, float startX, float startY) {
        ArrayList<Result> results = new ArrayList<>();
        for (int i = 0; i< mOutputRow; i++) {
            if (outputs[i* mOutputColumn +4] > mThreshold) {
                float x = outputs[i* mOutputColumn];
                float y = outputs[i* mOutputColumn +1];
                float w = outputs[i* mOutputColumn +2];
                float h = outputs[i* mOutputColumn +3];

                float left = imgScaleX * (x - w/2);
                float top = imgScaleY * (y - h/2);
                float right = imgScaleX * (x + w/2);
                float bottom = imgScaleY * (y + h/2);

                float max = outputs[i* mOutputColumn +5];
                int cls = 0;
                for (int j = 0; j < mOutputColumn -5; j++) {
                    if (outputs[i* mOutputColumn +5+j] > max) {
                        max = outputs[i* mOutputColumn +5+j];
                        cls = j;
                    }
                }

                Rect rect = new Rect((int)(startX+ivScaleX*left), (int)(startY+top*ivScaleY), (int)(startX+ivScaleX*right), (int)(startY+ivScaleY*bottom));
                Result result = new Result(cls, outputs[i*mOutputColumn+4], rect);
                results.add(result);
            }
        }
        return nonMaxSuppression(results, mNmsLimit, mThreshold);
    }

}

public class DetectionResult extends View {

    private final static int TX = 5;
    private final static int TY = 10;
    private final static int TW = 52;
    private final static int TH = 10;

    private Paint jPaintRect;
    private Paint jPaintTxt;
    private ArrayList<Result> jResults;

    public DetectionResult(Context context) {
        super(context);
    }

    public DetectionResult(Context context, AttributeSet attrs) {
        super(context, attrs);
        jPaintRect = new Paint();
        jPaintRect.setColor(Color.WHITE);
        jPaintTxt = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (jResults == null)
            return;
        for (Result result : jResults) {
            jPaintRect.setStrokeWidth(8);
            jPaintRect.setStyle(Paint.Style.STROKE);
            RectF recT = new RectF(result.rect);
            canvas.drawRoundRect(recT,10,10, jPaintRect);

            Path jPath = new Path();
            RectF jRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TW, result.rect.top + TH);
            jPath.addRect(jRectF, Path.Direction.CW);
            jPaintTxt.setColor(Color.WHITE);
            canvas.drawPath(jPath, jPaintTxt);

            jPaintTxt.setColor(Color.BLACK);
            jPaintTxt.setStrokeWidth(0);
            jPaintTxt.setStyle(Paint.Style.FILL_AND_STROKE);
            jPaintTxt.setTextSize(22);
            try {
                canvas.drawText(String.format("%s %.2f", ImageProcessing.jClasses[result.classIndex], result.score), result.rect.left + TX, result.rect.top + TY, jPaintTxt);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        jPaintTxt.setColor(Color.GRAY);
        jPaintTxt.setStrokeWidth(3);
        jPaintTxt.setStyle(Paint.Style.FILL);
        jPaintTxt.setTextSize(32);
        canvas.drawText(String.format("%s %d","Count :",jResults.size()), TX +30, TY+35, jPaintTxt);

    }
    public void setResults(ArrayList<Result> results){
//        String nameofCurrMethod = new Throwable().getStackTrace()[0].getMethodName();
//        Log.d(nameofCurrMethod, ""+java.time.LocalTime.now());

        jResults = results;
    }
}

