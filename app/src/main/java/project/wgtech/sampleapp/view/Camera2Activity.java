package project.wgtech.sampleapp.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import project.wgtech.sampleapp.R;
import project.wgtech.sampleapp.databinding.ActivityCamera2Binding;
import project.wgtech.sampleapp.tools.Constants;
import project.wgtech.sampleapp.tools.PermissionsActivity;

public class Camera2Activity extends AppCompatActivity implements SurfaceHolder.Callback2, ImageReader.OnImageAvailableListener {

    private final static String TAG = Camera2Activity.class.getSimpleName();

    private ActivityCamera2Binding binding;

    private Context context;

    private CameraManager manager;
    private CameraDevice camera;
    private CameraCharacteristics cc;
    private CameraCaptureSession session;
    private CaptureRequest.Builder reqBuilder;
    private ImageReader reader;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = Camera2Activity.this;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "onCreate: Above M Version");
            String[] permissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            Intent perm = new Intent(this, PermissionsActivity.class);
            perm.putExtra("permissions", permissions);

            startActivityForResult(perm, Constants.PERMISSIONS_REQUEST);
        } else {
            Log.d(TAG, "onCreate: Under M Version");
            initActivity();
        }
    }

    private void initActivity() {
        Log.d(TAG, "initActivity: ");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_camera2);
        binding.setActivity(this);

        binding.svCameraPreview.getHolder().setKeepScreenOn(true);
        binding.svCameraPreview.getHolder().setFixedSize(1440, 1080);
        binding.svCameraPreview.getHolder().addCallback(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 권한
        if (resultCode == Constants.PERMISSIONS_RESPONSE_OK) {
            Log.d(TAG, "onActivityResult: 권한 획득 성공 후 카메라 실행");
            binding.svCameraPreview.setVisibility(View.VISIBLE);
            initActivity();
        }

        else if (resultCode == Constants.PERMISSIONS_RESPONSE_FAIL) {
            // 권한 없음
            setResult(Constants.PERMISSIONS_RESPONSE_FAIL);
            finish();
        }

        // 카메라
        if (resultCode == Constants.CAMERA_PIC_OK) {
            // 저장
            Log.d(TAG, "onActivityResult: " + data.getData().getPath());
            setResult(resultCode);
            sendBroadcast(data);
        }
    }

    ////////////////////////////////////////////////////////////////////////

    @SuppressLint("MissingPermission")
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated: SurfaceView 생성");

        manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);

        reader = ImageReader.newInstance(1440, 1080, ImageFormat.JPEG, 2);
        reader.setOnImageAvailableListener(this, null);

        try {
            for (String camId: manager.getCameraIdList()) {
                cc = manager.getCameraCharacteristics(camId);
                manager.openCamera(camId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice c) {
                        Log.d(TAG, "onOpened: ");
                        camera = c;

                        try {
                            reqBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                            reqBuilder.addTarget(holder.getSurface());
                            reqBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

                            camera.createCaptureSession(
                                    Arrays.asList(holder.getSurface()),
                                    new CameraCaptureSession.StateCallback() {
                                        @Override
                                        public void onConfigured(@NonNull CameraCaptureSession s) {
                                            Log.d(TAG, "onConfigured: ");
                                            session = s;
                                            try {
                                                session.setRepeatingRequest(
                                                    reqBuilder.build(),
                                                    new CameraCaptureSession.CaptureCallback() {
                                                        @Override
                                                        public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                                                            super.onCaptureStarted(session, request, timestamp, frameNumber);
                                                        }

                                                        @Override
                                                        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                                                            super.onCaptureCompleted(session, request, result);
                                                        }

                                                        @Override
                                                        public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
                                                            super.onCaptureFailed(session, request, failure);
                                                        }
                                                    }, null);
                                            } catch (CameraAccessException e) {
                                                e.printStackTrace();
                                            }
                                            
                                        }

                                        @Override
                                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                                            Log.d(TAG, "onConfigureFailed: ");
                                        }
                                    },
                                    null
                            ); // camera.createCaptureSession

                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice c) {
                        Log.d(TAG, "onDisconnected: ");
                        camera.close();
                        camera = null;
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.d(TAG, "onError: " + error);

                    }
                }, null);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged: ");

    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {
        Log.d(TAG, "surfaceRedrawNeeded: ");
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: ");
        if (reader != null) reader.close();
        if (session != null) session.close();
        if (camera != null) camera.close();
        if (manager != null) manager = null;
    }

    ////////////////////////////////////////////////////////////////////////

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image img = reader.acquireLatestImage();
        if (reader.getImageFormat() == ImageFormat.JPEG) {
            Log.d(TAG, "onImageAvailable: " + img.getTimestamp());
        }

        if (img != null) {
            img.close();
        }

    }

    ////////////////////////////////////////////////////////////////////////

    public void clickLensChange(View view) {
        Toast.makeText(context, "전후 반전", Toast.LENGTH_SHORT).show();
    }

    public void clickCapture(View view) {
        Toast.makeText(context, "촬영", Toast.LENGTH_SHORT).show();
        try {
            session.capture(reqBuilder.build(),
                new CameraCaptureSession.CaptureCallback() {
                    @Override
                    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                        Log.d(TAG, "onCaptureCompleted: 촬영 완료");
                        super.onCaptureCompleted(session, request, result);
                    }
                }, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }
}