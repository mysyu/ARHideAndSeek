package tw.edu.yzu.cse.arhideandseek;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
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

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView imageView;
    private CameraManager cameraManager;
    private Handler childHandler, mainHandler;
    private String cameraID;
    private ImageReader imageReader;
    private CameraCaptureSession cameraCaptureSession;
    private CameraDevice cameraDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.game);
        Game.client.handler = handler;
        initView();
    }

    /**
     * 初始化
     */
    private void initView() {
        imageView = (ImageView) findViewById(R.id.capture);
        surfaceView = (SurfaceView) findViewById(R.id.camera);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera2();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
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
        imageReader = ImageReader.newInstance(1080, 1920, ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                cameraDevice.close();
                surfaceView.setVisibility(View.GONE);
                imageView.setVisibility(View.VISIBLE);

                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
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
            final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surfaceHolder.getSurface());
            cameraDevice.createCaptureSession(Arrays.asList(surfaceHolder.getSurface(), imageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                    if (cameraDevice == null) return;
                    Game.this.cameraCaptureSession = cameraCaptureSession;
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        CaptureRequest previewRequest = previewRequestBuilder.build();
                        Game.this.cameraCaptureSession.setRepeatingRequest(previewRequest, null, childHandler);
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


    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    break;
            }
        }
    };

}
