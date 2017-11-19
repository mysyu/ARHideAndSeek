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
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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

import java.nio.ByteBuffer;
import java.util.Arrays;

import static android.hardware.camera2.CameraAccessException.CAMERA_DISABLED;

public class Game extends AppCompatActivity {

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
    public static Bitmap[] init_hide = null;
    public static Bitmap[] init_seek = null;


    private ImageView Img_treasure;
    private ImageView Img_check;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView imageView;
    private CameraManager cameraManager;
    private CaptureRequest.Builder previewRequestBuilder;
    private Handler childHandler, mainHandler;
    private String cameraID;
    private ImageReader imageReader;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice;
    private int width;
    private int height;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        Game.client.handler = handler;
        hide = new Bitmap[Game.treasure];
        seek = new Bitmap[Game.treasure];
        init_hide = new Bitmap[Game.treasure];
        init_seek = new Bitmap[Game.treasure];
        for (int i = 0; i < Game.treasure; i++) {
            hide[i] = null;
            seek[i] = null;
            init_hide[i] = null;
            init_seek[i] = null;
        }
        surfaceView = (SurfaceView) findViewById(R.id.camera);
        surfaceView.bringToFront();
        imageView = (ImageView) findViewById(R.id.capture);
        imageView.bringToFront();
        Img_check = (ImageView) findViewById(R.id.Img_check);
        Img_check.bringToFront();
        Img_treasure = (ImageView) findViewById(R.id.Img_treasure);
        Img_treasure.bringToFront();
        if (isHost) {
            imageView.setVisibility(View.GONE);
            Img_check.setVisibility(View.VISIBLE);
            Img_check.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    takePicture();
                }
            });
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
                        case MotionEvent.ACTION_DOWN:
                            x = Img_treasure.getX();
                            y = Img_treasure.getY();
                            mx = event.getRawX();
                            my = event.getRawY();
                            Img_treasure.bringToFront();
                            break;
                    }
                    return true;
                }
            });
            initView();
        } else {
            imageView.setVisibility(View.VISIBLE);
            Img_check.setVisibility(View.GONE);
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
        imageReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                imageReader.setOnImageAvailableListener(null, null);
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap bitmap, b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (b != null) {
                    if (isHost) {
                        b = Bitmap.createScaledBitmap(b, width, height, false);
                        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
                        Canvas canvas = new Canvas(bitmap);
                        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                        canvas.drawBitmap(b, 0, 0, null);
                        canvas.drawBitmap(((BitmapDrawable) Img_treasure.getDrawable()).getBitmap(), Img_treasure.getX(), Img_treasure.getY(), null);
                        paint.setColor(Color.argb(200, 255, 255, 255));
                        canvas.drawRect(0, 0, 1000, 300, paint);
                        paint.setColor(Color.BLACK);
                        paint.setTextSize(50);
                        canvas.drawText("Hide: " + (Game.hide.length - treasure + 1), 50, 100, paint);
                        canvas.drawText("Team: Host", 50, 175, paint);
                        canvas.drawText("Name: " + name, 50, 250, paint);
                        Game.hide[Game.hide.length - treasure] = bitmap;
                        Game.init_hide[Game.hide.length - treasure] = b;
                        Img_check.setVisibility(View.GONE);
                        Img_treasure.setVisibility(View.GONE);
                        imageView.setImageBitmap(bitmap);
                        imageView.setVisibility(View.VISIBLE);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e) {
                                    Log.e("game", Log.getStackTraceString(e));
                                }
                                handler.sendEmptyMessage(3);
                            }
                        }).start();
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
            cameraDevice.createCaptureSession(Arrays.asList(surfaceHolder.getSurface(), imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
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

    private void takePicture() {
        if (cameraDevice == null) return;
        final CaptureRequest.Builder captureRequestBuilder;
        try {
            captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureRequestBuilder.addTarget(imageReader.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, 0);
            CaptureRequest mCaptureRequest = captureRequestBuilder.build();
            cameraCaptureSession.capture(mCaptureRequest, null, childHandler);
        } catch (CameraAccessException e) {
            Log.e("game", Log.getStackTraceString(e));
        }
    }

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
                                Img_check.setVisibility(View.VISIBLE);
                                Img_treasure.setVisibility(View.VISIBLE);
                                imageView.setVisibility(View.GONE);
                                Img_treasure.setX(Img_check.getX());
                                Img_treasure.setY(Img_check.getY());
                            } else if (treasure == 0) {
                                for (int i = 0; i < hide.length; i++) {
                                    Log.e("game", "Hide" + i + ": " + hide[i].getWidth() + " * " + hide[i].getHeight());
                                }
                                Game.client.Send(Game.roomID + "PLAY");
                            }
                        } else {
                            Img_check.setVisibility(View.VISIBLE);
                            Img_treasure.setVisibility(View.VISIBLE);
                            imageView.setVisibility(View.GONE);
                            Toast.makeText(Game.this, "Fail! Please hide again! " + treasure + " treasure left!", Toast.LENGTH_SHORT).show();
                        }
                        cameraDevice.close();
                        initCamera2();
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
            }
        }
    };

}
