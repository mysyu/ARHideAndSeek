package tw.edu.yzu.cse.arhideandseek;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Point;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mysyu on 2017/12/10.
 */

public class Detector {

    public static enum DETECTSTATE {
        NULL, NONE, NEAR, FIND
    }

    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("nonfree");
    }

    private static final FeatureDetector detector = FeatureDetector
            .create(FeatureDetector.SURF);
    private static final DescriptorExtractor extractor = DescriptorExtractor
            .create(DescriptorExtractor.SURF);
    private Handler handler = null;
    private Bitmap[] init_hide = null;
    private Mat[] mat_hide = null;
    private Mat[] mat_hide_gray = null;
    private Mat[] descriptors_hide = null;
    private MatOfKeyPoint[] keyPoint_hide = null;
    private Rect detect = null;
    private int width;
    private int height;
    public boolean isReady = true;


    public Detector(Integer treasure, Integer width, Integer height, Rect detect, Handler handler) {
        this.width = width;
        this.height = height;
        this.detect = detect;
        this.handler = handler;
        init_hide = new Bitmap[treasure];
        mat_hide = new Mat[treasure];
        mat_hide_gray = new Mat[treasure];
        descriptors_hide = new Mat[treasure];
        keyPoint_hide = new MatOfKeyPoint[treasure];
        for (int i = 0; i < treasure; i++) {
            init_hide[i] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            init_hide[i].eraseColor(Color.WHITE);
            mat_hide[i] = new Mat();
            mat_hide_gray[i] = new Mat();
            descriptors_hide[i] = new Mat();
            keyPoint_hide[i] = new MatOfKeyPoint();
        }
    }

    public void setHide(Integer index, Bitmap hide) {
        Canvas canvas = new Canvas(init_hide[index]);
        canvas.drawBitmap(hide, 0, 0, null);
        Utils.bitmapToMat(init_hide[index], mat_hide[index]);
        mat_hide[index].convertTo(mat_hide[index], CvType.CV_8UC3);
        Imgproc.cvtColor(mat_hide[index], mat_hide_gray[index], Imgproc.COLOR_RGB2GRAY);
        mat_hide_gray[index].convertTo(mat_hide_gray[index], CvType.CV_8UC3);
        detector.detect(mat_hide_gray[index], keyPoint_hide[index]);
        extractor.compute(mat_hide_gray[index], keyPoint_hide[index], descriptors_hide[index]);
        descriptors_hide[index].convertTo(descriptors_hide[index], CvType.CV_8UC3);
    }

    public void startDetect(final Bitmap seek) {
        final DETECTSTATE[] detectstates = new DETECTSTATE[init_hide.length];
        for (int i = 0; i < init_hide.length; i++) {
            final int now = i;
            detectstates[now] = DETECTSTATE.NULL;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    List<Point> matches = recognize(now, seek);
                    float numDetect = 0;
                    for (Point p : matches) {
                        Log.e("detect", p.x + " , " + p.y);
                        if (detect.contains((int) p.x, (int) p.y)) {
                            numDetect++;
                        }
                    }
                    if (numDetect / (float) matches.size() >= 0.8) {
                        detectstates[now] = DETECTSTATE.FIND;
                    } else if (numDetect / (float) matches.size() > 0) {
                        detectstates[now] = DETECTSTATE.NEAR;
                    } else {
                        detectstates[now] = DETECTSTATE.NONE;
                    }
                }
            }).start();
        }
        for (int i = 0; i < init_hide.length; i++) {
            if (detectstates[i] == DETECTSTATE.NULL) {
                i--;
            }
        }
        DETECTSTATE detectState = DETECTSTATE.NONE;
        for (DETECTSTATE state : detectstates) {
            if (state == DETECTSTATE.FIND) {
                break;
            } else if (state == DETECTSTATE.NEAR) {
                detectState = state;
            }
        }
        Log.e("detect", "DetectState: " + detectState.toString());
        isReady = true;
    }

    private List<Point> recognize(int index, Bitmap seek) {

        Mat mat_seek = new Mat();
        Mat mat_seek_gray = new Mat();
        Mat descriptors_seek = new Mat();
        MatOfKeyPoint keyPoint_seek = new MatOfKeyPoint();
        Utils.bitmapToMat(seek, mat_seek);
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        MatOfDMatch matches = new MatOfDMatch();
        List<Point> goodMatchesList = new ArrayList<Point>();
        List<Integer> find = new ArrayList<Integer>();


        mat_seek.convertTo(mat_seek, CvType.CV_8UC3);
        Imgproc.cvtColor(mat_seek, mat_seek_gray, Imgproc.COLOR_RGB2GRAY);
        mat_seek_gray.convertTo(mat_seek_gray, CvType.CV_8UC3);
        detector.detect(mat_seek_gray, keyPoint_seek);
        extractor.compute(mat_seek_gray, keyPoint_seek, descriptors_seek);
        descriptors_seek.convertTo(descriptors_seek, CvType.CV_8UC3);
        matcher.match(descriptors_hide[index], descriptors_seek, matches);
        List<DMatch> matchesList = matches.toList();
        Double max_dist = 0.0;
        Double min_dist = 100.0;
        for (int i = 0; i < matchesList.size(); i++) {
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist)
                min_dist = dist;
            if (dist > max_dist)
                max_dist = dist;
        }
        KeyPoint[] points = keyPoint_seek.toArray();
        for (DMatch m : matchesList) {
            if (m.distance <= (1.5 * min_dist) && !find.contains(m.trainIdx)) {
                find.add(m.trainIdx);
                goodMatchesList.add(points[m.trainIdx].pt.clone());
            }
        }
        Log.e("game", "GoodMatch: " + (float) goodMatchesList.size() + " / " + (float) matchesList.size());
        if ((float) goodMatchesList.size() / (float) matchesList.size() < 0.8) {
            goodMatchesList.clear();
        }
        return goodMatchesList;
    }

}
