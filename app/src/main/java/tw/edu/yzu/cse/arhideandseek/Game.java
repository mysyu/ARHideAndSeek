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
import android.graphics.Rect;
import android.graphics.RectF;
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
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static android.hardware.camera2.CameraAccessException.CAMERA_DISABLED;

public class Game extends AppCompatActivity {

    private static final String TF_OD_API_MODEL_FILE =
            "file:///android_asset/ssd_mobilenet_v1_android_export.pb";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/coco_labels_list.txt";
    private static Classifier objectDetector = null;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;



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
    public static Bitmap[] hide = null;
    public static Bitmap[] seek = null;

    public static Integer team_a_score[] = {0};
    public static Integer team_b_score[] = {0};

    private ImageView Img_treasure;
    private ImageView Img_detect;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView imageView;
    private CameraManager cameraManager;
    private CaptureRequest.Builder previewRequestBuilder;
    private Handler childHandler, mainHandler;
    private String cameraID;
    private ImageReader imageReader;
    private ImageReader previewReader;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice;
    private int width;
    private int height;
    private Rect detect = null;
    private float initTreasureX;
    private float initTreasureY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        Game.client.handler = handler;
        hide = new Bitmap[Game.treasure];
        seek = new Bitmap[Game.treasure];
        for (int i = 0; i < Game.treasure; i++) {
            hide[i] = null;
            seek[i] = null;
        }
        surfaceView = (SurfaceView) findViewById(R.id.camera);
        surfaceView.bringToFront();
        imageView = (ImageView) findViewById(R.id.capture);
        Img_detect = (ImageView) findViewById(R.id.Img_detect);
        Img_detect.bringToFront();
        imageView.bringToFront();
        Img_treasure = (ImageView) findViewById(R.id.Img_treasure);
        Img_treasure.bringToFront();
        if (isHost) {
            imageView.setVisibility(View.GONE);
            Img_treasure.setVisibility(View.VISIBLE);
            Img_treasure.setOnTouchListener(new View.OnTouchListener() {
                float x, y, mx, my;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_MOVE:
                            mx -= event.getRawX();
                            my -= event.getRawY();
                            Img_treasure.setX(x - mx);
                            Img_treasure.setY(y - my);
                            x = Img_treasure.getX();
                            y = Img_treasure.getY();
                            mx = event.getRawX();
                            my = event.getRawY();
                            Img_treasure.bringToFront();
                            break;
                        case MotionEvent.ACTION_DOWN:
                            initTreasureX = Img_treasure.getX();
                            initTreasureY = Img_treasure.getY();
                            x = Img_treasure.getX();
                            y = Img_treasure.getY();
                            mx = event.getRawX();
                            my = event.getRawY();
                            Img_treasure.bringToFront();
                            break;
                        case MotionEvent.ACTION_UP:
                            if (detect.contains((int) Img_treasure.getX(), (int) Img_treasure.getY(), (int) Img_treasure.getX() + Img_treasure.getWidth(), (int) Img_treasure.getY() + Img_treasure.getHeight())) {
                                Img_treasure.setX(initTreasureX);
                                Img_treasure.setY(initTreasureY);
                                Img_treasure.setVisibility(View.GONE);
                            } else {
                                Img_treasure.setVisibility(View.VISIBLE);
                                Img_treasure.setX(initTreasureX);
                                Img_treasure.setY(initTreasureY);
                            }
                            break;
                    }
                    return true;
                }
            });
            initView();
        } else {
            imageView.setVisibility(View.VISIBLE);
            Img_treasure.setVisibility(View.GONE);
        }
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
                try {
                    objectDetector = TensorFlowObjectDetectionAPIModel.create(getAssets(), TF_OD_API_MODEL_FILE, TF_OD_API_LABELS_FILE, 300, 300);
                } catch (Exception e) {
                    Log.e("err", Log.getStackTraceString(e));
                }
                if (detect == null) {
                    detect = new Rect((int) Img_detect.getX(), (int) Img_detect.getY(), (int) Img_detect.getX() + Img_detect.getWidth(), (int) Img_detect.getY() + Img_detect.getHeight());
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
        imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 2);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                Image image = reader.acquireNextImage();
                final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                image.close();
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (b != null) {
                    Log.e("game", "take picture");
                    b = Bitmap.createScaledBitmap(b, width, height, true);
                    final Bitmap bitmap = b.copy(Bitmap.Config.ARGB_8888, true);
                    if (isHost) {
                        final int now = Game.hide.length - treasure;
                        Game.hide[now] = bitmap;
                        imageView.setImageBitmap(b);
                        imageView.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                                try {
                                    MySQL.Excute("INSERT INTO capture VALUES(?,'HIDE',?,?)", new Object[]{Game.roomID, now, stream.toByteArray()});
                                } catch (Exception e) {
                                    Log.e("err", Log.getStackTraceString(e));
                                }
                                handler.sendEmptyMessage(3);
                            }
                        }).start();
                    } else {
                    }
                }
            }
        }, mainHandler);

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
            previewReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireNextImage();
                    final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);
                    image.close();
                    Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    if (b != null) {
                        b = Bitmap.createScaledBitmap(b, width, height, true);
                        final Bitmap crop = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888), src = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        if (isHost) {
                            Canvas canvas = new Canvas(crop);
                            frameToCropTransform = new Matrix();
                            frameToCropTransform.postScale(300 / (float) width, 300 / (float) height);
                            cropToFrameTransform = new Matrix();
                            frameToCropTransform.invert(cropToFrameTransform);
                            canvas.drawBitmap(b, frameToCropTransform, null);
                            List<Classifier.Recognition> recognitions = objectDetector.recognizeImage(crop);

                            Paint paint = new Paint();
                            paint.setColor(Color.RED);
                            canvas = new Canvas(src);
                            for (Classifier.Recognition recognition : recognitions) {
                                RectF location = recognition.getLocation();
                                Log.e("game", recognition.toString());
                                cropToFrameTransform.mapRect(location);
                                paint.setStyle(Paint.Style.STROKE);
                                paint.setStrokeWidth(5f);
                                canvas.drawRect(location, paint);
                                paint.setStyle(Paint.Style.FILL);
                                paint.setTextSize(50);
                                canvas.drawText(recognition.getTitle(), location.left + 50, location.top + 100, paint);
                            }
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImageBitmap(src);
                        } else {
                        }
                    }
                }
            }, mainHandler);
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

    private boolean hideTreasure() {
        return true;
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 3:
                    if (isHost) {
                        if (hideTreasure()) {
                            treasure--;
                            Toast.makeText(Game.this, "Success! " + treasure + " treasure left!", Toast.LENGTH_SHORT).show();
                            if (treasure > 0) {
                                Img_treasure.setVisibility(View.VISIBLE);
                                imageView.setVisibility(View.GONE);
                            } else if (treasure == 0) {
                                Img_treasure.setVisibility(View.GONE);
                                imageView.setVisibility(View.GONE);
                                isHost = false;
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        while (true) {
                                            try {
                                                Thread.sleep(5000);
                                            } catch (InterruptedException e) {
                                                Log.e("err", Log.getStackTraceString(e));
                                            }
                                        }
                                    }
                                }).start();

                                //Game.client.Send(Game.roomID + "PLAY");
                            }
                        } else {
                            Img_treasure.setVisibility(View.VISIBLE);
                            imageView.setVisibility(View.GONE);
                            Toast.makeText(Game.this, "Fail! Please hide again! " + treasure + " treasure left!", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                case 4:
                    Log.e("game", msg.getData().getString("PLAY", ""));
                    if (msg.getData().getString("PLAY", "").equals("Start")) {
                        if (isHost) {
                            Game.client.handler = null;
                            Intent intent = new Intent();
                            intent.setClass(Game.this, Stat.class);
                            startActivity(intent);
                            Game.this.finish();
                        } else {
                            imageView.setVisibility(View.GONE);
                            initView();
                        }
                    }
                    break;
                case 5:
                    Game.client.handler = null;
                    Intent intent = new Intent();
                    intent.setClass(Game.this, Stat.class);
                    startActivity(intent);
                    Game.this.finish();
                    break;
                case 55:
                    Log.e("hi", "hi");
                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(msg.getData().getByteArray("IMG")));
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageBitmap(bitmap);
                    break;
            }
        }
    };

}
