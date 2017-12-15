package tw.edu.yzu.cse.arhideandseek;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static android.hardware.camera2.CameraAccessException.CAMERA_DISABLED;

public class Game extends AppCompatActivity implements ImageReader.OnImageAvailableListener {

    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static Classifier objectDetector = null;
    private Matrix frameToCropTransform = null;
    private Matrix cropToFrameTransform = null;



    public static String name = null;
    public static String roomID = null;
    public static String host = null;
    public static Boolean isHost = null;
    public static Integer time = null;
    public static Boolean useCardBoard = null;
    public static Integer treasure = null;
    public static String teamA = null;
    public static String teamB = null;
    public static Client client = null;
    public static Integer status = null;
    public static Bitmap[] Img_hide = null;
    public static Bitmap[] Img_seek = null;
    public static String[] hide = null;
    public static String[] seek = null;
    public static Integer team_a_score[] = null;
    public static Integer team_b_score[] = null;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView Img_capture;
    private ImageView Img_hint;
    private CameraManager cameraManager;
    private CaptureRequest.Builder previewRequestBuilder;
    private Handler childHandler, mainHandler;
    private String cameraID;
    private ImageReader previewReader;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice;
    private int width;
    private int height;
    private String lastFind = "";
    private int lastFindCount = 0;
    private int captureStatus = 0;

    private long lastUpdate = -1;
    private float x, y, z;
    private float last_x, last_y, last_z;
    private static final int SHAKE_THRESHOLD = 800;
    private SensorManager sensorMgr;
    private boolean isShaked = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        Game.client.handler = handler;
        Img_hide = new Bitmap[Game.treasure];
        Img_seek = new Bitmap[Game.treasure];
        hide = new String[Game.treasure];
        seek = new String[Game.treasure];
        for (int i = 0; i < Game.treasure; i++) {
            Img_hide[i] = null;
            Img_seek[i] = null;
            hide[i] = null;
            seek[i] = null;

        }
        surfaceView = (SurfaceView) findViewById(R.id.camera);
        surfaceView.bringToFront();
        Img_capture = (ImageView) findViewById(R.id.capture);
        Img_capture.bringToFront();
        Img_hint = (ImageView) findViewById(R.id.hint);
        Img_hint.bringToFront();
        if (isHost) {
            Img_capture.setVisibility(View.VISIBLE);
            Img_hint.setVisibility(View.GONE);
            captureStatus = 0;
        } else {
            Img_hint.setImageResource(R.drawable.load);
            Img_hint.setVisibility(View.VISIBLE);
            Img_capture.setVisibility(View.GONE);
            captureStatus = 4;
        }
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Log.e("game", width + " * " + height);
                Game.this.width = width;
                Game.this.height = height;
                frameToCropTransform = new Matrix();
                frameToCropTransform.postScale(300 / (float) width, 300 / (float) height);
                cropToFrameTransform = new Matrix();
                frameToCropTransform.invert(cropToFrameTransform);
                try {
                    objectDetector = TensorFlowObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, TF_OD_API_INPUT_SIZE);
                } catch (Exception e) {
                    Log.e("game", Log.getStackTraceString(e));
                }
                initCamera2();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (cameraDevice != null) {
                    cameraDevice.close();
                    cameraDevice = null;
                }
            }
        });
    }

    /**
     * 初始化Camera2
     */
    private void initCamera2() {
        HandlerThread handlerThread = new HandlerThread("Camera2");
        handlerThread.start();
        childHandler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler(getMainLooper());
        cameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                throw new CameraAccessException(CAMERA_DISABLED);
            } else {
                cameraManager.openCamera(cameraID, stateCallback, mainHandler);
            }
        } catch (CameraAccessException e) {
            Log.e("game", Log.getStackTraceString(e));
        }
    }


    /**
     * 摄像头创建监听
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            cameraDevice = camera;
            takePreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e("game", "can not open camera");
        }
    };

    /**
     * 开始预览
     */
    private void takePreview() {
        try {
            previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surfaceHolder.getSurface());
            previewReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
            previewReader.setOnImageAvailableListener(this, mainHandler);
            previewRequestBuilder.addTarget(previewReader.getSurface());
            cameraDevice.createCaptureSession(Arrays.asList(surfaceHolder.getSurface(), previewReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null) return;
                    Game.this.cameraCaptureSession = cameraCaptureSession;
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        Game.this.cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(), null, childHandler);
                        Log.e("game", "start preview");
                    } catch (CameraAccessException e) {
                        Log.e("game", Log.getStackTraceString(e));
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                    Log.e("game", "can not connect camera to surface view");
                }
            }, childHandler);
        } catch (CameraAccessException e) {
            Log.e("game", Log.getStackTraceString(e));
        }
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireNextImage();
        final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        image.close();
        Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (captureStatus == 0 && b != null) {
            b = Bitmap.createScaledBitmap(b, width, height, true);
            final Bitmap crop = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Bitmap.Config.ARGB_8888), src = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(crop);
            Canvas findCanvas = new Canvas(b);
            canvas.drawBitmap(b, frameToCropTransform, null);
            List<Classifier.Recognition> recognitions = objectDetector.recognizeImage(crop);
            boolean lock = false;
            canvas = new Canvas(src);
            Paint paint = new Paint();
            paint.setStrokeWidth(5f);
            paint.setTextSize(50);
            paint.setColor(Color.RED);
            for (Classifier.Recognition recognition : recognitions) {
                RectF location = recognition.getLocation();
                Log.e("game", recognition.toString());
                cropToFrameTransform.mapRect(location);
                if (location.contains(width / 2, height / 2) && !lock) {
                    lock = true;
                    paint.setColor(Color.BLUE);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(location, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(recognition.getTitle(), location.left + 50, location.top + 100, paint);
                    if (lastFind.equals(recognition.getTitle())) {
                        lastFindCount++;
                        if (lastFindCount == 10) {
                            captureStatus = 1;
                            paint.setColor(Color.BLUE);
                            paint.setStyle(Paint.Style.STROKE);
                            findCanvas.drawRect(location, paint);
                            paint.setStyle(Paint.Style.FILL);
                            findCanvas.drawText(recognition.getTitle(), location.left + 50, location.top + 100, paint);
                            if (isHost) {
                                Game.Img_hide[Img_hide.length - treasure] = b;
                                Game.hide[Img_hide.length - treasure] = lastFind;
                                handler.sendEmptyMessage(3);
                                return;
                            } else {
                            }

                        }
                    } else {
                        lastFind = recognition.getTitle();
                        lastFindCount = 0;
                    }
                } else {
                    paint.setColor(Color.RED);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(location, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(recognition.getTitle(), location.left + 50, location.top + 100, paint);
                }
            }
            if (!lock) {
                lastFind = "";
                lastFindCount = 0;
                Log.e("game", "Nothing");
            }

            Img_capture.setImageBitmap(src);

        }
    }

    /*private void takePicture() {
        if (cameraDevice == null) return;
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            Log.e("game", "capture");
            cameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            Log.e("game", Log.getStackTraceString(e));
        }
    }*/

    public static float Round(float Rval, int Rpl) {
        float p = (float)Math.pow(10,Rpl);
        Rval = Rval * p;
        float tmp = Math.round(Rval);
        return (float)tmp/p;
    }

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (status == 3) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long curTime = System.currentTimeMillis();
                // only allow one update every 100ms.
                if ((curTime - lastUpdate) > 100) {
                    //long diffTime = (curTime - lastUpdate);
                    lastUpdate = curTime;

                    x = sensorEvent.values[0];
                    y = sensorEvent.values[1];
                    z = sensorEvent.values[2];

                    // 上下晃動（點頭）
                    if (Round(z, 4) < -0.0 || Round(y, 4) < 1.0) {
                        Log.d("sensor", "z Right axis: " + z);
                        Log.d("sensor", "y Left axis: " + y);
                        Toast.makeText(this, "Right shake detected", Toast.LENGTH_SHORT).show();
                        isShaked = true;
                        handler.sendEmptyMessage(4);
                    }
                    // 左右晃動（搖頭）
                    else if (Round(x, 4) > 10.0000 || Round(x, 4) < -10.0000) {
                        Log.d("sensor", "X Right axis: " + x);
                        Log.d("sensor", "X Left axis: " + x);
                        Toast.makeText(this, "Right shake detected", Toast.LENGTH_SHORT).show();
                        isShaked = true;
                        handler.sendEmptyMessage(5);
                    }

                    //float speed = Math.abs(x+y+z - last_x - last_y - last_z)/diffTime * 10000;
                    //if (speed > SHAKE_THRESHOLD) {
                    //      yes, this is a shake action! Do something about it!
                    //   isShaked = true;
                    //   handler.sendEmptyMessage(4);

                }
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    if (captureStatus == 1) {
                        captureStatus = 2;
                        if (isHost) {
                            Bitmap bitmap = Game.Img_hide[Img_hide.length - treasure].copy(Bitmap.Config.ARGB_8888, true);
                            Canvas canvas = new Canvas(bitmap);
                            Paint paint = new Paint();
                            paint.setTextSize(50);
                            paint.setStyle(Paint.Style.FILL);
                            paint.setColor(Color.argb(200, 255, 255, 255));
                            canvas.drawRect(width / 4, height * 3 / 4, width * 3 / 4, height, paint);
                            paint.setColor(Color.BLACK);
                            canvas.drawText("Nod for Accept\t\t\tShake for Cancel", width / 4 + 50, height * 3 / 4 + 150, paint);
                            Img_hint.setImageBitmap(bitmap);
                            Img_hint.setVisibility(View.VISIBLE);
                            Img_capture.setVisibility(View.GONE);
                            captureStatus = 3;
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(3000);
                                        captureStatus = 4;
                                        handler.sendEmptyMessage(4);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }
                    break;
                case 4:
                    if (isHost) {
                        final int current = Game.hide.length - Game.treasure;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                Game.client.Send(Game.roomID + "HOST" + current + ":" + hide[current]);
                                if (current + 1 == Game.hide.length) {
                                    Game.client.Send(Game.roomID + "PLAY");
                                }
                            }
                        }).start();
                        treasure--;
                        if (treasure > 0) {
                            Img_capture.setVisibility(View.VISIBLE);
                            Img_hint.setVisibility(View.GONE);
                            lastFind = "";
                            lastFindCount = 0;
                            captureStatus = 0;
                        } else {
                            Img_hint.setImageResource(R.drawable.load);
                            Img_hint.setVisibility(View.VISIBLE);
                            Img_capture.setVisibility(View.GONE);
                        }
                    }
                    break;
                case 5:
                    Img_capture.setVisibility(View.VISIBLE);
                    Img_hint.setVisibility(View.GONE);
                    lastFind = "";
                    lastFindCount = 0;
                    captureStatus = 0;
                    break;
                case 6:
                    Log.e("game", msg.getData().getString("PLAY", ""));
                    String current = msg.getData().getString("PLAY", "");
                    if (current.equals("Start")) {
                        Toast.makeText(Game.this, current, Toast.LENGTH_SHORT).show();
                        handler.sendEmptyMessage(7);
                    } else {
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.load).copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(bitmap);
                        Paint paint = new Paint();
                        paint.setTextSize(50);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setColor(Color.argb(200, 255, 255, 255));
                        canvas.drawRect(width / 2 - 100, height - 200, width / 2 + 100, height, paint);
                        paint.setColor(Color.BLACK);
                        canvas.drawText(current, width / 2 - 50, height - 150, paint);
                        Img_hint.setImageBitmap(bitmap);
                        Img_hint.setVisibility(View.VISIBLE);
                        Img_capture.setVisibility(View.GONE);
                    }
                    break;
                case 7:
                    Game.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(Game.this, Stat.class);
                    startActivity(intent);
                    Game.this.finish();
                    break;
            }
        }
    };

}