package tw.edu.yzu.cse.arhideandseek;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import java.util.LinkedList;
import java.util.List;

public class Load extends AppCompatActivity {

    static {
        System.loadLibrary("opencv_java");
        System.loadLibrary("nonfree");
    }

    private static final FeatureDetector detector = FeatureDetector
            .create(FeatureDetector.SIFT);
    private static final DescriptorExtractor extractor = DescriptorExtractor
            .create(DescriptorExtractor.SIFT);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.load);
        Log.e("detector", Boolean.toString(detector == null));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 0);
            } else {
                handler.sendEmptyMessage(-1);
            }
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0 && requestCode == 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            handler.sendEmptyMessage(-1);
        } else {
            handler.sendEmptyMessage(0);
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(3000);
                                handler.sendEmptyMessage(1);
                            } catch (InterruptedException e) {
                                Log.e("load", Log.getStackTraceString(e));
                            }
                        }
                    }).start();
                    break;
                case 1:
                    Intent intent = new Intent();
                    intent.setClass(Load.this, Menu.class);
                    startActivity(intent);
                    Load.this.finish();
                    break;
                case -1:
                    Toast.makeText(Load.this, "You do not have camera permission", Toast.LENGTH_SHORT).show();
                    Load.this.finish();
                    break;
            }
        }
    };

    public Bitmap recognize(Bitmap source, Bitmap target) {

        Mat src = new Mat();
        Mat tgt = new Mat();
        Mat srcgray = new Mat();
        Mat tgtgray = new Mat();
        Mat result = new Mat();
        Mat descriptors1 = new Mat();
        Mat descriptors2 = new Mat();
        MatOfKeyPoint keyPoint1 = new MatOfKeyPoint();
        MatOfKeyPoint keyPoint2 = new MatOfKeyPoint();
        Utils.bitmapToMat(source, src);
        src.convertTo(descriptors1, CvType.CV_8UC3);
        Imgproc.cvtColor(src, srcgray, Imgproc.COLOR_RGB2GRAY);
        Utils.bitmapToMat(target, tgt);
        tgt.convertTo(descriptors1, CvType.CV_8UC3);
        Imgproc.cvtColor(tgt, tgtgray, Imgproc.COLOR_RGB2GRAY);
        detector.detect(srcgray, keyPoint1);
        extractor.compute(srcgray, keyPoint1, descriptors1);
        descriptors1.convertTo(descriptors1, CvType.CV_8UC3);
        detector.detect(tgtgray, keyPoint2);
        extractor.compute(tgtgray, keyPoint2, descriptors2);
        descriptors2.convertTo(descriptors2, CvType.CV_8UC3);
        MatOfDMatch matches = new MatOfDMatch();
        Log.e("type", descriptors1.type() + " " + descriptors2.type());
        DescriptorMatcher matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        matcher.match(descriptors1, descriptors2, matches);
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
        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (int i = 0; i < matchesList.size(); i++) {
            if (matchesList.get(i).distance <= (1.5 * min_dist))
                good_matches.addLast(matchesList.get(i));
        }
        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);
        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        Features2d.drawMatches(srcgray, keyPoint1, tgtgray, keyPoint2, goodMatches, result, new Scalar(255, 0, 0), new Scalar(255, 0, 0), drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        Bitmap bmp = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(result, bmp);
        KeyPoint[] points = keyPoint2.toArray();
        for (DMatch p : good_matches) {
            Log.e("match", p.trainIdx + " " + points[p.trainIdx].pt.x + " , " + points[p.trainIdx].pt.y);
        }
        return bmp;
/*
        // Matching
        MatOfDMatch matches = new MatOfDMatch();
        if (img1.type() == aInputFrame.type()) {
            matcher.match(descriptors1, descriptors2, matches);
        } else {
            return aInputFrame;
        }
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

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (int i = 0; i < matchesList.size(); i++) {
            if (matchesList.get(i).distance <= (1.5 * min_dist))
                good_matches.addLast(matchesList.get(i));
        }

        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);
        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        if (aInputFrame.empty() || aInputFrame.cols() < 1 || aInputFrame.rows() < 1) {
            return aInputFrame;
        }
        Features2d.drawMatches(img1, keypoints1, aInputFrame, keypoints2, goodMatches, outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        Imgproc.resize(outputImg, outputImg, aInputFrame.size());

        return outputImg;
        */
    }

}
