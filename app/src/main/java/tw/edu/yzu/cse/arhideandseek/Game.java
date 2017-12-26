package tw.edu.yzu.cse.arhideandseek;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.hardware.SensorEventListener;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static android.hardware.camera2.CameraAccessException.CAMERA_DISABLED;

public class Game extends AppCompatActivity implements ImageReader.OnImageAvailableListener, SensorEventListener {

    private static final String TF_OD_API_MODEL_FILE = "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static final int TF_OD_API_INPUT_SIZE = 300;

    private static Classifier objectDetector = null;
    private Matrix frameToCropTransform = null;
    private Matrix cropToFrameTransform = null;

    private ImageView Img_capture;
    private ImageView Img_hint;
    private TextView hint;

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
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
    private int captureIndex = 0;
    private Bitmap capture = null;
    private int life = 3;

    private float x, y, z;
    private boolean isShaked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        Static.client.handler = handler;
        Static.Img_hide = new Bitmap[Static.treasure];
        Static.Img_seek = new Bitmap[Static.treasure];
        Static.hide = new String[Static.treasure];
        Static.seek = new String[Static.treasure];
        for (int i = 0; i < Static.treasure; i++) {
            Static.Img_hide[i] = null;
            Static.Img_seek[i] = null;
            Static.hide[i] = null;
            Static.seek[i] = null;

        }
        surfaceView = (SurfaceView) findViewById(R.id.camera);
        surfaceView.bringToFront();
        Img_capture = (ImageView) findViewById(R.id.capture);
        Img_capture.bringToFront();
        Img_hint = (ImageView) findViewById(R.id.hint);
        Img_hint.bringToFront();
        hint = (TextView) findViewById(R.id.hint_text);
        hint.bringToFront();
        if (Static.isHost) {
            Img_capture.setVisibility(View.VISIBLE);
            Img_hint.setVisibility(View.GONE);
            hint.setText("");
            captureStatus = 0;
        } else {
            Img_hint.setBackgroundColor(Color.WHITE);
            Img_hint.setVisibility(View.VISIBLE);
            hint.setText("");
            Img_capture.setVisibility(View.GONE);
            captureStatus = 4;
        }
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), SensorManager.SENSOR_DELAY_GAME);
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
                frameToCropTransform.postScale(TF_OD_API_INPUT_SIZE / (float) width, TF_OD_API_INPUT_SIZE / (float) height);
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
            b = Bitmap.createScaledBitmap(b, width, height, true).copy(Bitmap.Config.ARGB_8888, true);
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
                if (Static.isHost && Arrays.asList(Static.hide).contains(recognition.getTitle())) {
                    lock = true;
                }
                if (location.contains(width / 2, height / 2) && !lock) {
                    lock = true;
                    paint.setColor(Color.BLUE);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(location, paint);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawText(recognition.getTitle(), location.left + 50, location.top + 100, paint);
                    if (lastFind.equals(recognition.getTitle())) {
                        lastFindCount++;
                        if (lastFindCount == 5) {
                            captureStatus = 1;
                            paint.setColor(Color.BLUE);
                            paint.setStyle(Paint.Style.STROKE);
                            findCanvas.drawRect(location, paint);
                            paint.setStyle(Paint.Style.FILL);
                            findCanvas.drawText(recognition.getTitle(), location.left + 50, location.top + 100, paint);
                            if (Static.isHost) {
                                Static.Img_hide[Static.Img_hide.length - Static.treasure] = b;
                                Static.hide[Static.Img_hide.length - Static.treasure] = lastFind;
                            } else {
                                captureIndex = Arrays.asList(Static.hide).indexOf(recognition.getTitle());
                                capture = b;
                            }
                            handler.sendEmptyMessage(3);
                            return;
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

    public void onSensorChanged(SensorEvent sensorEvent) {
        if (captureStatus == 3 && !isShaked) {

            if (sensorEvent.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                long curTime = System.currentTimeMillis();

                x = sensorEvent.values[0];
                y = sensorEvent.values[1];
                z = sensorEvent.values[2];

                Log.e("sensor", x + " " + y + " " + z);

                if (Math.abs(z) < 2 && ((Math.abs(x) < 2) ^ (Math.abs(y) < 2))) {

                    Log.e("game", x + " " + y + " " + z);
                    isShaked = true;

                    if (Math.abs(x) < 1) {
                        Log.e("game", "nod");
                        handler.sendEmptyMessage(4);
                    } else if (Math.abs(y) < 1) {
                        Log.e("game", "shake");
                        if (Static.isHost) {
                            Static.hide[Static.hide.length - Static.treasure] = null;
                            Static.Img_hide[Static.hide.length - Static.treasure] = null;
                        }
                        handler.sendEmptyMessage(5);
                    }
                }

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    if (captureStatus == 1) {
                        captureStatus = 2;
                        Bitmap bitmap;
                        if (Static.isHost) {
                            Img_hint.setImageBitmap(Static.Img_hide[Static.Img_hide.length - Static.treasure]);
                        } else {
                            Img_hint.setImageBitmap(capture);
                        }
                        Img_hint.setVisibility(View.VISIBLE);
                        hint.setText("Nod for Accept\t\t\tShake for Cancel");
                        Img_capture.setVisibility(View.GONE);
                        captureStatus = 3;
                        /*new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(3000);
                                    captureStatus = 4;
                                    handler.sendEmptyMessage(4);
                                } catch (InterruptedException e) {
                                    Log.e("game",Log.getStackTraceString(e));
                                }
                            }
                        }).start();*/
                    }
                    break;
                case 4:
                    if (Static.isHost) {
                        final int current = Static.hide.length - Static.treasure;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                Static.Img_hide[current].compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                try {
                                    MySQL.Excute("INSERT INTO capture VALUES(?,'HIDE',?,?)", new Object[]{Static.roomID, current, stream.toByteArray()});
                                    Static.client.Send(Static.roomID + "HIDE;" + current + ";" + Static.hide[current]);
                                    if (current + 1 == Static.hide.length) {
                                        Static.client.Send(Static.roomID + "PLAY");
                                    }
                                } catch (Exception e) {
                                    Log.e("game", Log.getStackTraceString(e));
                                }
                            }
                        }).start();
                        Static.treasure--;
                        Img_hint.setImageDrawable(getDrawable(R.drawable.wait));
                        Img_hint.setVisibility(View.VISIBLE);
                        Img_capture.setVisibility(View.GONE);
                        hint.setText("");
                        Toast.makeText(Game.this, "Hide Success. " + Static.treasure + " left.", Toast.LENGTH_SHORT).show();
                        if (Static.treasure > 0) {
                            handler.sendEmptyMessage(5);
                        }
                    } else {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                capture.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                try {
                                    if (captureIndex == -1) throw new Exception("Fail");
                                    MySQL.Excute("INSERT INTO capture VALUES(?,'SEEK',?,?)", new Object[]{Static.roomID, captureIndex, stream.toByteArray()});
                                    Static.client.Send(Static.roomID + "SEEK;" + captureIndex + ";" + Static.hide[captureIndex] + ";" + Static.name);
                                } catch (Exception e) {
                                    if (e.getMessage().equals("Fail")) {
                                        handler.sendEmptyMessage(8);
                                    } else {
                                        Log.e("game", Log.getStackTraceString(e));
                                    }
                                }
                            }
                        }).start();
                        Img_hint.setImageDrawable(getDrawable(R.drawable.wait));
                        Img_hint.setVisibility(View.VISIBLE);
                        Img_capture.setVisibility(View.GONE);
                        hint.setText("");
                    }
                    break;
                case 5:
                    Img_capture.setVisibility(View.VISIBLE);
                    Img_hint.setVisibility(View.GONE);
                    hint.setText("");
                    lastFind = "";
                    lastFindCount = 0;
                    captureStatus = 0;
                    isShaked = false;
                    break;
                case 6:
                    String now = msg.getData().getString("PLAY", "");
                    Log.e("game", now);
                    if (now.equals("Start")) {
                        hint.setText(now);
                        //isHost = false;
                        if (Static.isHost) {
                            handler.sendEmptyMessage(9);
                        } else {
                            handler.sendEmptyMessage(5);
                        }
                    } else if (Static.status == 2) {
                        hint.setText(now);
                        Img_hint.setImageDrawable(getDrawable(R.drawable.wait));
                        Img_hint.setVisibility(View.VISIBLE);
                        Img_capture.setVisibility(View.GONE);
                    }
                    break;
                case 7:
                    int current = msg.getData().getInt("SEEK", -1);
                    String who = msg.getData().getString("WHO", "");
                    Log.e("game", "SEEK" + current + ":" + who);
                    Toast.makeText(Game.this, who + " find 1 treasure", Toast.LENGTH_SHORT).show();
                    if (who.equals(Static.name)) {
                        handler.sendEmptyMessage(5);
                    }
                    if (Static.score_a + Static.score_b == Static.hide.length) {
                        handler.sendEmptyMessage(9);
                    }
                    break;
                case 8:
                    life--;
                    Toast.makeText(Game.this, "Fail! Life: " + life, Toast.LENGTH_SHORT).show();
                    Log.e("game", "Fail! Life: " + life);
                    if (life != 0) {
                        handler.sendEmptyMessage(5);
                    } else {
                        handler.sendEmptyMessage(9);
                    }
                    break;
                case 9:
                    if (!Static.isHost) {
                        Toast.makeText(Game.this, "Game End", Toast.LENGTH_SHORT).show();
                    }
                    Static.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(Game.this, Room.class);
                    startActivity(intent);
                    Game.this.finish();
                    break;
            }
        }
    };

}